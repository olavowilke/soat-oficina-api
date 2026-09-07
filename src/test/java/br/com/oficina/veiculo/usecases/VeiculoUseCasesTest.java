package br.com.oficina.veiculo.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.entities.Placa;
import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoUseCasesTest {

    @Mock
    private VeiculoGateway veiculoGateway;

    @Mock
    private ClienteGateway clienteGateway;

    private UUID clienteId;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
    }

    private Veiculo buildVeiculo() {
        return Veiculo.novo(clienteId, new Placa("ABC1234"), "Toyota", "Corolla", 2020, "Prata");
    }

    @Test
    void deveCadastrarVeiculoComSucesso() {
        CadastrarVeiculoUseCase useCase = new CadastrarVeiculoUseCase(veiculoGateway, clienteGateway);
        CadastrarVeiculoCommand command = new CadastrarVeiculoCommand(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata");
        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        // The use case normalizes the plate (strips hyphens, uppercases) BEFORE
        // calling existsByPlaca, so the stub must match the normalized value.
        when(veiculoGateway.existsByPlaca("ABC1234")).thenReturn(false);
        when(veiculoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Veiculo result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getMarca()).isEqualTo("Toyota");
        verify(veiculoGateway).save(any());
    }

    @Test
    void deveLancarExcecaoAoCadastrarSeClienteNaoExiste() {
        CadastrarVeiculoUseCase useCase = new CadastrarVeiculoUseCase(veiculoGateway, clienteGateway);
        CadastrarVeiculoCommand command = new CadastrarVeiculoCommand(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata");
        when(clienteGateway.existsById(clienteId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoGateway, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCadastrarComPlacaDuplicada() {
        CadastrarVeiculoUseCase useCase = new CadastrarVeiculoUseCase(veiculoGateway, clienteGateway);
        CadastrarVeiculoCommand command = new CadastrarVeiculoCommand(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata");
        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.existsByPlaca("ABC1234")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("placa");
        verify(veiculoGateway, never()).save(any());
    }

    @Test
    void deveAtualizarVeiculoComSucesso() {
        AtualizarVeiculoUseCase useCase = new AtualizarVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        Veiculo veiculo = buildVeiculo();
        when(veiculoGateway.findById(id)).thenReturn(Optional.of(veiculo));
        when(veiculoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AtualizarVeiculoCommand command = new AtualizarVeiculoCommand("Honda", "Civic", 2022, "Preto");
        Veiculo result = useCase.execute(id, command);

        assertThat(result.getMarca()).isEqualTo("Honda");
        assertThat(result.getModelo()).isEqualTo("Civic");
        verify(veiculoGateway).save(any());
    }

    @Test
    void deveLancarExcecaoAoAtualizarVeiculoNaoEncontrado() {
        AtualizarVeiculoUseCase useCase = new AtualizarVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        when(veiculoGateway.findById(id)).thenReturn(Optional.empty());

        AtualizarVeiculoCommand command = new AtualizarVeiculoCommand("Honda", "Civic", 2022, "Preto");
        assertThatThrownBy(() -> useCase.execute(id, command))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveBuscarVeiculoPorId() {
        BuscarVeiculoUseCase useCase = new BuscarVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        Veiculo veiculo = buildVeiculo();
        when(veiculoGateway.findById(id)).thenReturn(Optional.of(veiculo));

        Veiculo result = useCase.execute(id);

        assertThat(result).isEqualTo(veiculo);
    }

    @Test
    void deveLancarExcecaoAoBuscarVeiculoNaoEncontrado() {
        BuscarVeiculoUseCase useCase = new BuscarVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        when(veiculoGateway.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarVeiculosPorCliente() {
        ListarVeiculosPorClienteUseCase useCase = new ListarVeiculosPorClienteUseCase(veiculoGateway, clienteGateway);
        Veiculo veiculo = buildVeiculo();
        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.findByClienteId(clienteId)).thenReturn(List.of(veiculo));

        List<Veiculo> result = useCase.execute(clienteId);

        assertThat(result).hasSize(1);
        verify(veiculoGateway).findByClienteId(clienteId);
    }

    @Test
    void deveLancarExcecaoAoListarVeiculosDeClienteNaoEncontrado() {
        ListarVeiculosPorClienteUseCase useCase = new ListarVeiculosPorClienteUseCase(veiculoGateway, clienteGateway);
        when(clienteGateway.existsById(clienteId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(clienteId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoGateway, never()).findByClienteId(any());
    }

    @Test
    void deveRemoverVeiculoExistente() {
        RemoverVeiculoUseCase useCase = new RemoverVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        when(veiculoGateway.existsById(id)).thenReturn(true);

        useCase.execute(id);

        verify(veiculoGateway).deleteById(id);
    }

    @Test
    void deveLancarExcecaoAoRemoverVeiculoNaoEncontrado() {
        RemoverVeiculoUseCase useCase = new RemoverVeiculoUseCase(veiculoGateway);
        UUID id = UUID.randomUUID();
        when(veiculoGateway.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoGateway, never()).deleteById(any());
    }
}
