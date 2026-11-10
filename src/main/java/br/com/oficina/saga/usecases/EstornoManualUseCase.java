package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.EstornoNaoPendenteException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.UUID;

/**
 * Saída do operador quando o estorno falhou (saga FALHOU, OS em AGUARDANDO_EXECUCAO, com pagamento).
 * Pode pedir o estorno de novo ou cancelar a OS depois de estornar fora do sistema.
 */
@Service
public class EstornoManualUseCase {

    private final SagaGateway sagaGateway;
    private final OrdemServicoGateway ordemServicoGateway;
    private final ComandoSagaGateway comandos;
    private final DevolverPecasAoEstoque devolverPecas;
    private final NotificadorStatusOrdem notificador;
    private final Clock clock;

    public EstornoManualUseCase(SagaGateway sagaGateway, OrdemServicoGateway ordemServicoGateway,
                                ComandoSagaGateway comandos, DevolverPecasAoEstoque devolverPecas,
                                NotificadorStatusOrdem notificador, Clock clock) {
        this.sagaGateway = sagaGateway;
        this.ordemServicoGateway = ordemServicoGateway;
        this.comandos = comandos;
        this.devolverPecas = devolverPecas;
        this.notificador = notificador;
        this.clock = clock;
    }

    @Transactional
    public SagaOrdemServico reenviarEstorno(UUID osId) {
        SagaOrdemServico saga = sagaComEstornoFalho(osId).saga();
        saga.mover(EstadoSaga.FALHOU, EstadoSaga.ESTORNANDO, clock.instant());
        SagaOrdemServico salva = sagaGateway.salvar(saga);
        comandos.estornarPagamento(salva, "Estorno reenviado pelo operador");
        return salva;
    }

    @Transactional
    public SagaOrdemServico cancelarAposEstornoManual(UUID osId) {
        Falha falha = sagaComEstornoFalho(osId);
        SagaOrdemServico saga = falha.saga();
        OrdemServico os = falha.os();
        saga.mover(EstadoSaga.FALHOU, EstadoSaga.COMPENSADA, clock.instant());
        devolverPecas.execute(os);
        os.cancelar();
        SagaOrdemServico salva = sagaGateway.salvar(saga);
        ordemServicoGateway.save(os);
        notificador.notificar(os);
        return salva;
    }

    private Falha sagaComEstornoFalho(UUID osId) {
        OrdemServico os = ordemServicoGateway.findById(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        SagaOrdemServico saga = sagaGateway.buscarPorOsId(osId).stream()
                .max(Comparator.comparing(SagaOrdemServico::getCriadoEm))
                .orElseThrow(() -> new EstornoNaoPendenteException(osId));
        if (!saga.estornoFalhou() || os.getStatus() != StatusOS.AGUARDANDO_EXECUCAO) {
            throw new EstornoNaoPendenteException(osId);
        }
        return new Falha(saga, os);
    }

    private record Falha(SagaOrdemServico saga, OrdemServico os) { }
}
