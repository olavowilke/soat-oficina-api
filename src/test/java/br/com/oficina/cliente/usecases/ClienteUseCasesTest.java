package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.entities.Documento;
import br.com.oficina.cliente.entities.TipoDocumento;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteUseCasesTest {

    private static final String VALID_CPF = "52998224725";

    @Mock
    private ClienteGateway clienteGateway;

    @InjectMocks
    private CadastrarClienteUseCase cadastrarCliente;
    @InjectMocks
    private AtualizarClienteUseCase atualizarCliente;
    @InjectMocks
    private ListarClientesUseCase listarClientes;
    @InjectMocks
    private RemoverClienteUseCase removerCliente;

    private CadastrarClienteCommand buildCommand() {
        return new CadastrarClienteCommand(
                "João Silva", "joao@example.com", "11999999999", VALID_CPF, TipoDocumento.CPF);
    }

    private Cliente buildCliente() {
        Documento documento = new Documento(VALID_CPF, TipoDocumento.CPF);
        return Cliente.novo("João Silva", "joao@example.com", "11999999999", documento);
    }

    @Test
    void shouldCadastrarAndSaveCliente() {
        CadastrarClienteCommand command = buildCommand();
        when(clienteGateway.existsByDocumento(VALID_CPF)).thenReturn(false);
        when(clienteGateway.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente result = cadastrarCliente.execute(command);

        assertNotNull(result);
        assertEquals("João Silva", result.getNome());
        verify(clienteGateway).save(any(Cliente.class));
    }

    @Test
    void shouldThrowWhenDocumentoAlreadyExists() {
        CadastrarClienteCommand command = buildCommand();
        when(clienteGateway.existsByDocumento(VALID_CPF)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> cadastrarCliente.execute(command));
        verify(clienteGateway, never()).save(any());
    }

    @Test
    void shouldAtualizarAndSaveCliente() {
        UUID id = UUID.randomUUID();
        Cliente cliente = buildCliente();
        when(clienteGateway.findById(id)).thenReturn(Optional.of(cliente));
        when(clienteGateway.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarClienteCommand command = new AtualizarClienteCommand("Novo Nome", "novo@example.com", "11888888888");
        Cliente result = atualizarCliente.execute(id, command);

        assertEquals("Novo Nome", result.getNome());
        verify(clienteGateway).save(any(Cliente.class));
    }

    @Test
    void shouldThrowWhenAtualizarAndClienteNotFound() {
        UUID id = UUID.randomUUID();
        when(clienteGateway.findById(id)).thenReturn(Optional.empty());

        AtualizarClienteCommand command = new AtualizarClienteCommand("Novo Nome", "novo@example.com", null);
        assertThrows(RecursoNaoEncontradoException.class, () -> atualizarCliente.execute(id, command));
    }

    @Test
    void shouldDesativarClienteOnRemover() {
        UUID id = UUID.randomUUID();
        Cliente cliente = buildCliente();
        when(clienteGateway.findById(id)).thenReturn(Optional.of(cliente));
        when(clienteGateway.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        removerCliente.execute(id);

        assertFalse(cliente.isAtivo());
        verify(clienteGateway).save(cliente);
    }

    @Test
    void shouldThrowWhenRemoverAndClienteNotFound() {
        UUID id = UUID.randomUUID();
        when(clienteGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> removerCliente.execute(id));
    }

    @Test
    void listarDeveRetornarApenasClientesAtivos() {
        Cliente c1 = buildCliente();
        when(clienteGateway.findAllAtivos()).thenReturn(List.of(c1));

        List<Cliente> result = listarClientes.execute();

        assertEquals(1, result.size());
        verify(clienteGateway).findAllAtivos();
        verify(clienteGateway, never()).findAll();
    }
}
