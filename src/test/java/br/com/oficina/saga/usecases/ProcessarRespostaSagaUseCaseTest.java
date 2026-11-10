package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessarRespostaSagaUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock private SagaGateway sagaGateway;
    @Mock private OrdemServicoGateway ordemServicoGateway;
    @Mock private ComandoSagaGateway comandos;
    @Mock private DevolverPecasAoEstoque devolverPecas;
    @Mock private NotificadorStatusOrdem notificador;
    @Mock private PecaGateway pecaGateway;

    private ProcessarRespostaSagaUseCase useCase;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarRespostaSagaUseCase(sagaGateway, ordemServicoGateway, comandos, devolverPecas,
                notificador, Clock.fixed(AGORA, ZoneOffset.UTC));
        logger = (Logger) LoggerFactory.getLogger(ProcessarRespostaSagaUseCase.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    private SagaOrdemServico saga(EstadoSaga estado, UUID osId) {
        return SagaOrdemServico.reconstituir(UUID.randomUUID(), osId, estado, null, null, null, null, null,
                0L, AGORA, AGORA);
    }

    private OrdemServico os(StatusOS status, List<ItemPeca> pecas) {
        return OrdemServico.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, null,
                List.of(), pecas, null, null, null, null);
    }

    private SagaOrdemServico preparar(EstadoSaga estado, OrdemServico os) {
        SagaOrdemServico saga = saga(estado, os.getId());
        when(sagaGateway.buscarPorId(saga.getId())).thenReturn(Optional.of(saga));
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        return saga;
    }

    private RespostaSaga resposta(SagaOrdemServico saga, String tipo, Map<String, Object> payload) {
        return new RespostaSaga(UUID.randomUUID(), saga.getId(), saga.getOsId(), tipo, payload);
    }

    private void verificarSalvos(SagaOrdemServico saga, OrdemServico os) {
        verify(sagaGateway).salvar(saga);
        verify(ordemServicoGateway).save(os);
    }

    @Test
    void orcamentoGeradoGuardaOrcamentoAvancaENotifica() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_ORCAMENTO, os);
        UUID orcamentoId = UUID.randomUUID();

        useCase.execute(resposta(saga, "OrcamentoGerado",
                Map.of("orcamentoId", orcamentoId.toString(), "valorTotal", "350.00")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_APROVACAO);
        assertThat(saga.getOrcamentoId()).isEqualTo(orcamentoId);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void orcamentoFalhouGuardaMotivoEVoltaParaDiagnostico() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_ORCAMENTO, os);

        useCase.execute(resposta(saga, "OrcamentoFalhou", Map.of("motivo", "Total zero")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.FALHOU);
        assertThat(saga.getMotivoFalha()).isEqualTo("Total zero");
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        verificarSalvos(saga, os);
        verifyNoInteractions(notificador, devolverPecas, comandos);
    }

    @Test
    void orcamentoAprovadoGuardaPagamentoELinkENotificaComLink() {
        OrdemServico os = os(StatusOS.AGUARDANDO_APROVACAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_APROVACAO, os);
        UUID pagamentoId = UUID.randomUUID();

        useCase.execute(resposta(saga, "OrcamentoAprovado", Map.of("orcamentoId", UUID.randomUUID().toString(),
                "pagamentoId", pagamentoId.toString(), "linkPagamento", "https://pagar.exemplo/abc")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_PAGAMENTO);
        assertThat(saga.getPagamentoId()).isEqualTo(pagamentoId);
        assertThat(saga.getLinkPagamento()).isEqualTo("https://pagar.exemplo/abc");
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_PAGAMENTO);
        verificarSalvos(saga, os);
        verify(notificador).notificar(os, "https://pagar.exemplo/abc");
    }

    @Test
    void orcamentoRecusadoDevolvePecasCancelaENotifica() {
        OrdemServico os = os(StatusOS.AGUARDANDO_APROVACAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_APROVACAO, os);

        useCase.execute(resposta(saga, "OrcamentoRecusado", Map.of("orcamentoId", UUID.randomUUID().toString())));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(devolverPecas).execute(os);
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void pagamentoConfirmadoAvancaEPedeExecucao() {
        OrdemServico os = os(StatusOS.AGUARDANDO_PAGAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_PAGAMENTO, os);

        useCase.execute(resposta(saga, "PagamentoConfirmado", Map.of()));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_EXECUCAO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);
        verificarSalvos(saga, os);
        verify(comandos).iniciarExecucao(saga, os);
        verifyNoInteractions(devolverPecas);
    }

    @Test
    void pagamentoFalhouGuardaMotivoDevolvePecasCancelaENotifica() {
        OrdemServico os = os(StatusOS.AGUARDANDO_PAGAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_PAGAMENTO, os);

        useCase.execute(resposta(saga, "PagamentoFalhou",
                Map.of("pagamentoId", UUID.randomUUID().toString(), "motivo", "EXPIRADO")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);
        assertThat(saga.getMotivoFalha()).isEqualTo("EXPIRADO");
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(devolverPecas).execute(os);
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void execucaoAceitaGuardaExecucaoIniciaENotifica() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_EXECUCAO, os);
        UUID execucaoId = UUID.randomUUID();

        useCase.execute(resposta(saga, "ExecucaoAceita", Map.of("execucaoId", execucaoId.toString())));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.EM_EXECUCAO);
        assertThat(saga.getExecucaoId()).isEqualTo(execucaoId);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getInicioExecucao()).isEqualTo(LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC));
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void execucaoRejeitadaPedeEstornoENaoDevolveAinda() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_EXECUCAO, os);

        useCase.execute(resposta(saga, "ExecucaoRejeitada", Map.of("motivo", "CAPACIDADE")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.ESTORNANDO);
        assertThat(saga.getMotivoFalha()).isEqualTo("CAPACIDADE");
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);
        verify(sagaGateway).salvar(saga);
        verify(comandos).estornarPagamento(eq(saga), any(String.class));
        verifyNoInteractions(devolverPecas, notificador);
        verify(ordemServicoGateway, never()).save(any());
    }

    @Test
    void estornoConfirmadoDevolvePecasECancela() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.ESTORNANDO, os);

        useCase.execute(resposta(saga, "PagamentoEstornado", Map.of("pagamentoId", UUID.randomUUID().toString())));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(devolverPecas).execute(os);
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void estornoFalhouMarcaFalhouMantemOsELogaErro() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.ESTORNANDO, os);

        useCase.execute(resposta(saga, "EstornoFalhou",
                Map.of("pagamentoId", UUID.randomUUID().toString(), "motivo", "Gateway fora")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.FALHOU);
        assertThat(saga.getMotivoFalha()).isEqualTo("Gateway fora");
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);
        verify(sagaGateway).salvar(saga);
        verify(ordemServicoGateway, never()).save(any());
        verifyNoInteractions(devolverPecas, notificador);
        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.ERROR);
            assertThat(e.getFormattedMessage()).contains("saga.estorno.falhou");
        });
    }

    @Test
    void estornoFalhouComPagamentoIdNuloAindaMarcaFalhouELogaErro() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.ESTORNANDO, os);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("pagamentoId", null);
        payload.put("motivo", "Sem pagamento no Billing");

        useCase.execute(resposta(saga, "EstornoFalhou", payload));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.FALHOU);
        assertThat(saga.getMotivoFalha()).isEqualTo("Sem pagamento no Billing");
        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.ERROR);
            assertThat(e.getFormattedMessage()).contains("saga.estorno.falhou");
        });
    }

    @Test
    void execucaoFinalizadaFinalizaConcluiENotifica() {
        OrdemServico os = os(StatusOS.EM_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.EM_EXECUCAO, os);

        useCase.execute(resposta(saga, "ExecucaoFinalizada", Map.of("execucaoId", UUID.randomUUID().toString(),
                "finalizadaEm", "2026-11-09T09:59:00Z")));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.CONCLUIDA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getFimExecucao()).isEqualTo(LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC));
        verificarSalvos(saga, os);
        verify(notificador).notificar(os);
    }

    @Test
    void respostaAdiantadaLancaErroParaARedeliveryEMantemTudo() {
        OrdemServico os = os(StatusOS.AGUARDANDO_APROVACAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_APROVACAO, os);

        assertThatThrownBy(() -> useCase.execute(resposta(saga, "PagamentoConfirmado", Map.of())))
                .isInstanceOf(RespostaAdiantadaException.class);

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_APROVACAO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        verify(sagaGateway, never()).salvar(any());
        verify(ordemServicoGateway, never()).save(any());
        verifyNoInteractions(comandos, devolverPecas, notificador);
        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains(saga.getId().toString(), "PagamentoConfirmado",
                    "AGUARDANDO_APROVACAO");
        });
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void respostaTardiaComSagaAdianteEhConfirmadaComAviso() {
        OrdemServico os = os(StatusOS.AGUARDANDO_PAGAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_PAGAMENTO, os);

        useCase.execute(resposta(saga, "OrcamentoGerado", Map.of("orcamentoId", UUID.randomUUID().toString())));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_PAGAMENTO);
        verify(sagaGateway, never()).salvar(any());
        verifyNoInteractions(comandos, devolverPecas, notificador);
        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains("OrcamentoGerado", "AGUARDANDO_PAGAMENTO");
        });
    }

    @Test
    void respostaDuplicadaEmEstadoTerminalEhConfirmadaComAviso() {
        OrdemServico os = os(StatusOS.FINALIZADA, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.CONCLUIDA, os);

        useCase.execute(resposta(saga, "ExecucaoFinalizada", Map.of()));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.CONCLUIDA);
        verify(sagaGateway, never()).salvar(any());
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    void respostaDeEstornoComSagaEmExecucaoNaoEhAdiantada() {
        OrdemServico os = os(StatusOS.EM_EXECUCAO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.EM_EXECUCAO, os);

        useCase.execute(resposta(saga, "PagamentoEstornado", Map.of()));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.EM_EXECUCAO);
        verify(sagaGateway, never()).salvar(any());
    }

    @Test
    void osIdDiferenteDoDaSagaEhIgnoradoComAviso() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_ORCAMENTO, os);
        UUID outraOs = UUID.randomUUID();

        useCase.execute(new RespostaSaga(UUID.randomUUID(), saga.getId(), outraOs, "OrcamentoGerado",
                Map.of("orcamentoId", UUID.randomUUID().toString())));

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        verify(sagaGateway, never()).salvar(any());
        verify(ordemServicoGateway, never()).save(any());
        verifyNoInteractions(comandos, devolverPecas, notificador);
        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains(saga.getId().toString(), outraOs.toString());
        });
    }

    @Test
    void sagaDesconhecidaEhDescartadaComAviso() {
        UUID sagaId = UUID.randomUUID();
        when(sagaGateway.buscarPorId(sagaId)).thenReturn(Optional.empty());

        useCase.execute(new RespostaSaga(UUID.randomUUID(), sagaId, UUID.randomUUID(), "OrcamentoGerado", Map.of()));

        verify(sagaGateway, never()).salvar(any());
        verifyNoInteractions(ordemServicoGateway, comandos, devolverPecas, notificador);
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    void tipoDesconhecidoLancaErro() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_ORCAMENTO, os);

        assertThatThrownBy(() -> useCase.execute(resposta(saga, "Inventada", Map.of())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Inventada");
        verify(sagaGateway, never()).salvar(any());
    }

    @Test
    void payloadInvalidoEmTipoConhecidoLogaErroELancaSemMudarNada() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, List.of());
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_ORCAMENTO, os);

        assertThatThrownBy(() -> useCase.execute(resposta(saga, "OrcamentoGerado", Map.of("orcamentoId", "xyz"))))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("orcamentoId");
        assertThatThrownBy(() -> useCase.execute(resposta(saga, "OrcamentoGerado", Map.of())))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("orcamentoId");

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_ORCAMENTO);
        verify(sagaGateway, never()).salvar(any());
        verify(ordemServicoGateway, never()).save(any());
        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.ERROR).hasSize(2)
                .allSatisfy(e -> assertThat(e.getFormattedMessage()).contains("orcamentoId"));
    }

    @Test
    void recusaDevolvePecasAoEstoque() {
        UUID pecaId = UUID.randomUUID();
        OrdemServico os = os(StatusOS.AGUARDANDO_APROVACAO,
                List.of(ItemPeca.novo(pecaId, "Filtro", 2, new BigDecimal("25.00")),
                        ItemPeca.novo(pecaId, "Filtro", 3, new BigDecimal("25.00"))));
        SagaOrdemServico saga = preparar(EstadoSaga.AGUARDANDO_APROVACAO, os);
        useCase = new ProcessarRespostaSagaUseCase(sagaGateway, ordemServicoGateway, comandos,
                new DevolverPecasAoEstoque(pecaGateway), notificador, Clock.fixed(AGORA, ZoneOffset.UTC));

        useCase.execute(resposta(saga, "OrcamentoRecusado", Map.of("orcamentoId", UUID.randomUUID().toString())));

        verify(pecaGateway).devolverAoEstoque(pecaId, 2);
        verify(pecaGateway).devolverAoEstoque(pecaId, 3);
    }
}
