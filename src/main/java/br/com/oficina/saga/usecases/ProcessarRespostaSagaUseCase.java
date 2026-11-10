package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.EstadoSagaInesperadoException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.MetricasSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orquestrador da saga. Cada resposta move a saga de um estado esperado para o
 * novo e muda a OS na mesma transação. Se a saga já passou do estado esperado (resposta
 * atrasada ou duplicada), só gera um WARN. Se ainda não chegou lá (resposta adiantada),
 * lança {@link RespostaAdiantadaException} para a mensagem voltar à fila.
 */
@Service
public class ProcessarRespostaSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarRespostaSagaUseCase.class);
    private static final int MAX_MOTIVO = 300;

    private final SagaGateway sagaGateway;
    private final OrdemServicoGateway ordemServicoGateway;
    private final ComandoSagaGateway comandos;
    private final DevolverPecasAoEstoque devolverPecas;
    private final NotificadorStatusOrdem notificador;
    private final MetricasSagaGateway metricas;
    private final Clock clock;

    public ProcessarRespostaSagaUseCase(SagaGateway sagaGateway, OrdemServicoGateway ordemServicoGateway,
                                        ComandoSagaGateway comandos, DevolverPecasAoEstoque devolverPecas,
                                        NotificadorStatusOrdem notificador, MetricasSagaGateway metricas,
                                        Clock clock) {
        this.sagaGateway = sagaGateway;
        this.ordemServicoGateway = ordemServicoGateway;
        this.comandos = comandos;
        this.devolverPecas = devolverPecas;
        this.notificador = notificador;
        this.metricas = metricas;
        this.clock = clock;
    }

    @Transactional
    public void execute(RespostaSaga r) {
        SagaOrdemServico saga = sagaGateway.buscarPorId(r.sagaId()).orElse(null);
        if (saga == null) {
            log.warn("Resposta {} ({}) para saga desconhecida {}; descartada", r.messageId(), r.type(), r.sagaId());
            return;
        }
        OrdemServico os = ordemServicoGateway.findById(saga.getOsId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", saga.getOsId()));
        if (r.osId() != null && !r.osId().equals(saga.getOsId())) {
            log.warn("Resposta {} ({}) cita a OS {}, mas a saga {} é da OS {}; ignorada",
                    r.messageId(), r.type(), r.osId(), saga.getId(), saga.getOsId());
            return;
        }
        EstadoSaga estadoAntes = saga.getEstado();
        try {
            aplicar(r, saga, os);
            if (!saga.getEstado().ativo()) {
                metricas.sagaFinalizada(saga.getEstado());
            }
        } catch (EstadoSagaInesperadoException e) {
            if (estadoAntes.antesDe(e.getEsperado())) {
                // Corrida esperada: a resposta passou a que a antecede. Sem confirmar, o SQS entrega de novo.
                log.warn("Resposta {} adiantada: saga {} em {}, espera {}; volta para a fila",
                        r.type(), saga.getId(), estadoAntes, e.getEsperado());
                throw new RespostaAdiantadaException(saga.getId(), r.type(), estadoAntes, e.getEsperado());
            }
            log.warn("Resposta {} atrasada ou duplicada: saga {} em {}; sem mudança ({})",
                    r.type(), saga.getId(), estadoAntes, e.getMessage());
        } catch (PayloadInvalidoException e) {
            // A mensagem não é confirmada: vai para a DLQ depois das tentativas, e o alerta de DLQ avisa o operador.
            log.error("Payload inválido em {} da saga {} (mensagem {}): {}",
                    r.type(), saga.getId(), r.messageId(), e.getMessage());
            throw e;
        }
    }

    private void aplicar(RespostaSaga r, SagaOrdemServico saga, OrdemServico os) {
        Instant agora = clock.instant();
        switch (r.type()) {
            case "OrcamentoGerado" -> {
                UUID orcamentoId = uuid(r, "orcamentoId");
                saga.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.AGUARDANDO_APROVACAO, agora);
                saga.definirOrcamentoId(orcamentoId);
                os.registrarOrcamentoGerado();
                salvar(saga, os);
                notificador.notificar(os);
            }
            case "OrcamentoFalhou" -> {
                saga.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.FALHOU, agora);
                saga.definirMotivoFalha(motivo(r));
                os.voltarParaDiagnostico();
                salvar(saga, os);
            }
            case "OrcamentoAprovado" -> {
                UUID pagamentoId = uuid(r, "pagamentoId");
                String link = texto(r, "linkPagamento");
                saga.mover(EstadoSaga.AGUARDANDO_APROVACAO, EstadoSaga.AGUARDANDO_PAGAMENTO, agora);
                saga.definirPagamentoId(pagamentoId);
                saga.definirLinkPagamento(link);
                os.registrarAprovacao();
                salvar(saga, os);
                notificador.notificar(os, link);
            }
            case "OrcamentoRecusado" -> {
                saga.mover(EstadoSaga.AGUARDANDO_APROVACAO, EstadoSaga.COMPENSADA, agora);
                compensarECancelar(saga, os);
            }
            case "PagamentoConfirmado" -> {
                saga.mover(EstadoSaga.AGUARDANDO_PAGAMENTO, EstadoSaga.AGUARDANDO_EXECUCAO, agora);
                os.registrarPagamentoConfirmado();
                salvar(saga, os);
                comandos.iniciarExecucao(saga, os);
            }
            case "PagamentoFalhou" -> {
                saga.mover(EstadoSaga.AGUARDANDO_PAGAMENTO, EstadoSaga.COMPENSADA, agora);
                saga.definirMotivoFalha(motivo(r));
                compensarECancelar(saga, os);
            }
            case "ExecucaoAceita" -> {
                UUID execucaoId = uuid(r, "execucaoId");
                saga.mover(EstadoSaga.AGUARDANDO_EXECUCAO, EstadoSaga.EM_EXECUCAO, agora);
                saga.definirExecucaoId(execucaoId);
                os.iniciarExecucao(LocalDateTime.now(clock));
                salvar(saga, os);
                notificador.notificar(os);
            }
            case "ExecucaoRejeitada" -> {
                String motivo = motivo(r);
                saga.mover(EstadoSaga.AGUARDANDO_EXECUCAO, EstadoSaga.ESTORNANDO, agora);
                saga.definirMotivoFalha(motivo);
                sagaGateway.salvar(saga);
                comandos.estornarPagamento(saga, "Execução rejeitada: " + motivo);
            }
            case "PagamentoEstornado" -> {
                saga.mover(EstadoSaga.ESTORNANDO, EstadoSaga.COMPENSADA, agora);
                compensarECancelar(saga, os);
            }
            case "EstornoFalhou" -> {
                String motivo = motivo(r);
                saga.mover(EstadoSaga.ESTORNANDO, EstadoSaga.FALHOU, agora);
                saga.definirMotivoFalha(motivo);
                sagaGateway.salvar(saga);
                log.error("saga.estorno.falhou sagaId={} osId={} motivo={}", saga.getId(), saga.getOsId(), motivo);
            }
            case "ExecucaoFinalizada" -> {
                saga.mover(EstadoSaga.EM_EXECUCAO, EstadoSaga.CONCLUIDA, agora);
                os.finalizarExecucao(LocalDateTime.now(clock));
                salvar(saga, os);
                notificador.notificar(os);
            }
            default -> throw new IllegalArgumentException("Tipo de resposta desconhecido: " + r.type());
        }
    }

    private void compensarECancelar(SagaOrdemServico saga, OrdemServico os) {
        devolverPecas.execute(os);
        os.cancelar();
        salvar(saga, os);
        notificador.notificar(os);
    }

    private void salvar(SagaOrdemServico saga, OrdemServico os) {
        sagaGateway.salvar(saga);
        ordemServicoGateway.save(os);
    }

    private String motivo(RespostaSaga r) {
        Object m = r.payload() == null ? null : r.payload().get("motivo");
        String texto = (m == null || m.toString().isBlank()) ? "sem motivo" : m.toString();
        return texto.length() > MAX_MOTIVO ? texto.substring(0, MAX_MOTIVO) : texto;
    }

    private String texto(RespostaSaga r, String campo) {
        Object v = r.payload() == null ? null : r.payload().get(campo);
        if (v == null || v.toString().isBlank()) {
            throw new PayloadInvalidoException("campo " + campo + " ausente");
        }
        return v.toString();
    }

    private UUID uuid(RespostaSaga r, String campo) {
        String valor = texto(r, campo);
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw new PayloadInvalidoException("campo " + campo + " não é um UUID: " + valor);
        }
    }

    /** Problema de negócio no payload de um tipo conhecido. Não adianta tentar de novo. */
    static class PayloadInvalidoException extends RuntimeException {
        PayloadInvalidoException(String message) {
            super(message);
        }
    }
}
