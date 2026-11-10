package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.EstornoNaoPendenteException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.MetricasSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstornoManualUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock private SagaGateway sagaGateway;
    @Mock private OrdemServicoGateway ordemServicoGateway;
    @Mock private ComandoSagaGateway comandos;
    @Mock private DevolverPecasAoEstoque devolverPecas;
    @Mock private NotificadorStatusOrdem notificador;
    @Mock private MetricasSagaGateway metricas;

    private EstornoManualUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EstornoManualUseCase(sagaGateway, ordemServicoGateway, comandos, devolverPecas, notificador,
                metricas, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private OrdemServico os(StatusOS status) {
        return OrdemServico.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, null,
                List.of(), List.of(), null, null, null, null);
    }

    private SagaOrdemServico saga(UUID osId, EstadoSaga estado, UUID pagamentoId, Instant criadoEm) {
        return SagaOrdemServico.reconstituir(UUID.randomUUID(), osId, estado, UUID.randomUUID(), pagamentoId, null,
                null, "Gateway fora", 0L, criadoEm, criadoEm);
    }

    private SagaOrdemServico preparar(OrdemServico os, SagaOrdemServico... sagas) {
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarPorOsId(os.getId())).thenReturn(List.of(sagas));
        return sagas[0];
    }

    @Test
    void reenviarVoltaParaEstornandoEGravaComando() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO);
        SagaOrdemServico saga = preparar(os, saga(os.getId(), EstadoSaga.FALHOU, UUID.randomUUID(), AGORA));
        when(sagaGateway.salvar(saga)).thenReturn(saga);

        SagaOrdemServico resultado = useCase.reenviarEstorno(os.getId());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSaga.ESTORNANDO);
        assertThat(resultado.getMotivoFalha()).isEqualTo("Gateway fora");
        verify(sagaGateway).salvar(saga);
        verify(comandos).estornarPagamento(any(SagaOrdemServico.class), any(String.class));
        verifyNoInteractions(devolverPecas, notificador, metricas);
    }

    @Test
    void cancelarDevolvePecasCancelaAOsECompensaASaga() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO);
        SagaOrdemServico saga = preparar(os, saga(os.getId(), EstadoSaga.FALHOU, UUID.randomUUID(), AGORA));
        when(sagaGateway.salvar(saga)).thenReturn(saga);

        SagaOrdemServico resultado = useCase.cancelarAposEstornoManual(os.getId());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);
        assertThat(resultado.getMotivoFalha()).isEqualTo("Gateway fora");
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(devolverPecas).execute(os);
        verify(ordemServicoGateway).save(os);
        verify(notificador).notificar(os);
        verify(metricas).sagaFinalizada(EstadoSaga.COMPENSADA);
        verifyNoInteractions(comandos);
    }

    @Test
    void usaSoAUltimaSaga() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO);
        SagaOrdemServico antiga = saga(os.getId(), EstadoSaga.FALHOU, UUID.randomUUID(),
                AGORA.minus(Duration.ofDays(1)));
        SagaOrdemServico nova = saga(os.getId(), EstadoSaga.AGUARDANDO_ORCAMENTO, null, AGORA);
        preparar(os, antiga, nova);

        assertThatThrownBy(() -> useCase.reenviarEstorno(os.getId())).isInstanceOf(EstornoNaoPendenteException.class);
        assertThatThrownBy(() -> useCase.cancelarAposEstornoManual(os.getId()))
                .isInstanceOf(EstornoNaoPendenteException.class);
        verify(sagaGateway, never()).salvar(any());
    }

    @Test
    void falhaDeOrcamentoSemPagamentoNaoEhFalhaDeEstorno() {
        OrdemServico os = os(StatusOS.EM_DIAGNOSTICO);
        preparar(os, saga(os.getId(), EstadoSaga.FALHOU, null, AGORA));

        assertThatThrownBy(() -> useCase.reenviarEstorno(os.getId())).isInstanceOf(EstornoNaoPendenteException.class);
        assertThatThrownBy(() -> useCase.cancelarAposEstornoManual(os.getId()))
                .isInstanceOf(EstornoNaoPendenteException.class);
        verify(sagaGateway, never()).salvar(any());
        verifyNoInteractions(comandos, devolverPecas, notificador);
    }

    @Test
    void osForaDeAguardandoExecucaoNaoAceita() {
        OrdemServico os = os(StatusOS.CANCELADA);
        preparar(os, saga(os.getId(), EstadoSaga.FALHOU, UUID.randomUUID(), AGORA));

        assertThatThrownBy(() -> useCase.cancelarAposEstornoManual(os.getId()))
                .isInstanceOf(EstornoNaoPendenteException.class);
    }

    @Test
    void osSemSagaRetorna409() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarPorOsId(os.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.reenviarEstorno(os.getId())).isInstanceOf(EstornoNaoPendenteException.class);
    }

    @Test
    void osInexistenteRetorna404() {
        UUID id = UUID.randomUUID();
        when(ordemServicoGateway.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reenviarEstorno(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
