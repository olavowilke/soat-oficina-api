package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaJaAtivaException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniciarSagaUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private OrdemServicoGateway ordemServicoGateway;
    @Mock
    private SagaGateway sagaGateway;
    @Mock
    private ComandoSagaGateway comandos;
    @Mock
    private NotificadorStatusOrdem notificador;

    private IniciarSagaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new IniciarSagaUseCase(ordemServicoGateway, sagaGateway, comandos, notificador,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private OrdemServico osEmDiagnostico(boolean comItem) {
        return OrdemServico.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                StatusOS.EM_DIAGNOSTICO, null,
                comItem ? List.of(ItemServico.novo(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.00")))
                        : List.of(),
                List.of(), null, null, null, null);
    }

    @Test
    void iniciaSagaEnviaComandoENotifica() {
        OrdemServico os = osEmDiagnostico(true);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarAtivaPorOsId(os.getId())).thenReturn(Optional.empty());
        when(ordemServicoGateway.save(os)).thenReturn(os);
        when(sagaGateway.salvar(any())).thenAnswer(i -> i.getArgument(0));

        SagaOrdemServico saga = useCase.execute(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_ORCAMENTO);
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        assertThat(saga.getOsId()).isEqualTo(os.getId());
        assertThat(saga.getCriadoEm()).isEqualTo(AGORA);
        var ordem = inOrder(ordemServicoGateway, sagaGateway, comandos, notificador);
        ordem.verify(ordemServicoGateway).save(os);
        ordem.verify(sagaGateway).salvar(saga);
        ordem.verify(comandos).gerarOrcamento(saga, os);
        ordem.verify(notificador).notificar(os);
    }

    @Test
    void osInexistenteLancaNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(ordemServicoGateway.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(sagaGateway, comandos, notificador);
    }

    @Test
    void sagaAtivaLancaSagaJaAtivaSemAlterarNada() {
        OrdemServico os = osEmDiagnostico(true);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarAtivaPorOsId(os.getId()))
                .thenReturn(Optional.of(SagaOrdemServico.iniciar(os.getId(), AGORA)));

        assertThatThrownBy(() -> useCase.execute(os.getId())).isInstanceOf(SagaJaAtivaException.class);
        verify(ordemServicoGateway, never()).save(any());
        verifyNoInteractions(comandos, notificador);
    }

    @Test
    void osSemItensLancaIllegalState() {
        OrdemServico os = osEmDiagnostico(false);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarAtivaPorOsId(os.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(os.getId())).isInstanceOf(IllegalStateException.class);
        verify(sagaGateway, never()).salvar(any());
        verifyNoInteractions(comandos, notificador);
    }
}
