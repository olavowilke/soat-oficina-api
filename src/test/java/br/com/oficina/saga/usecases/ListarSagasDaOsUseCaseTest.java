package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarSagasDaOsUseCaseTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;
    @Mock
    private SagaGateway sagaGateway;
    @InjectMocks
    private ListarSagasDaOsUseCase useCase;

    private SagaOrdemServico saga(UUID osId, String criadoEm) {
        return SagaOrdemServico.reconstituir(UUID.randomUUID(), osId, EstadoSaga.FALHOU, null, null, null,
                null, null, 0L, Instant.parse(criadoEm), Instant.parse(criadoEm));
    }

    @Test
    void devolveSagasDaMaisNovaParaAMaisAntiga() {
        OrdemServico os = OrdemServico.nova(UUID.randomUUID(), UUID.randomUUID(), null);
        SagaOrdemServico velha = saga(os.getId(), "2026-11-01T10:00:00Z");
        SagaOrdemServico meio = saga(os.getId(), "2026-11-05T10:00:00Z");
        SagaOrdemServico nova = saga(os.getId(), "2026-11-09T10:00:00Z");
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(sagaGateway.buscarPorOsId(os.getId())).thenReturn(List.of(velha, nova, meio));

        assertThat(useCase.execute(os.getId())).containsExactly(nova, meio, velha);
    }

    @Test
    void osInexistenteLancaNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(ordemServicoGateway.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(sagaGateway);
    }
}
