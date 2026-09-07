package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoUseCasesTest {

    @Mock
    private ServicoGateway servicoGateway;

    private Servico buildServico() {
        return Servico.novo("Troca de Óleo", "Troca completa de óleo", new BigDecimal("150.00"), 30);
    }

    @Test
    void cadastrarShouldSaveAndReturnServico() {
        CadastrarServicoUseCase useCase = new CadastrarServicoUseCase(servicoGateway);
        CadastrarServicoCommand command = new CadastrarServicoCommand(
                "Troca de Óleo", "Troca completa de óleo", new BigDecimal("150.00"), 30);
        when(servicoGateway.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        Servico result = useCase.execute(command);

        assertNotNull(result);
        assertEquals("Troca de Óleo", result.getNome());
        verify(servicoGateway).save(any(Servico.class));
    }

    @Test
    void atualizarShouldUpdateAndSave() {
        AtualizarServicoUseCase useCase = new AtualizarServicoUseCase(servicoGateway);
        UUID id = UUID.randomUUID();
        Servico servico = buildServico();
        when(servicoGateway.findById(id)).thenReturn(Optional.of(servico));
        when(servicoGateway.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarServicoCommand command = new AtualizarServicoCommand(
                "Troca de Óleo Sintético", "Óleo sintético premium", new BigDecimal("200.00"), 45);
        Servico result = useCase.execute(id, command);

        assertEquals("Troca de Óleo Sintético", result.getNome());
        verify(servicoGateway).save(any(Servico.class));
    }

    @Test
    void atualizarShouldThrowRecursoNaoEncontradoExceptionWhenNotFound() {
        AtualizarServicoUseCase useCase = new AtualizarServicoUseCase(servicoGateway);
        UUID id = UUID.randomUUID();
        when(servicoGateway.findById(id)).thenReturn(Optional.empty());

        AtualizarServicoCommand command = new AtualizarServicoCommand(
                "Troca de Óleo", "desc", new BigDecimal("150.00"), 30);
        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(id, command));
    }

    @Test
    void buscarShouldThrowWhenNotFound() {
        BuscarServicoUseCase useCase = new BuscarServicoUseCase(servicoGateway);
        UUID id = UUID.randomUUID();
        when(servicoGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(id));
    }

    @Test
    void removerShouldDesativarServico() {
        RemoverServicoUseCase useCase = new RemoverServicoUseCase(servicoGateway);
        UUID id = UUID.randomUUID();
        Servico servico = buildServico();
        when(servicoGateway.findById(id)).thenReturn(Optional.of(servico));
        when(servicoGateway.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(id);

        assertFalse(servico.isAtivo());
        verify(servicoGateway).save(servico);
    }

    @Test
    void listarDeveRetornarApenasServicosAtivos() {
        ListarServicosUseCase useCase = new ListarServicosUseCase(servicoGateway);
        Servico s1 = buildServico();
        when(servicoGateway.findAllAtivos()).thenReturn(List.of(s1));

        List<Servico> result = useCase.execute();

        assertEquals(1, result.size());
        verify(servicoGateway).findAllAtivos();
        verify(servicoGateway, never()).findAll();
    }
}
