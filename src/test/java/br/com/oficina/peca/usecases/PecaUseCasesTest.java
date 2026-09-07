package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
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
class PecaUseCasesTest {

    @Mock
    private PecaGateway pecaGateway;

    private Peca buildPeca() {
        return Peca.novo("Filtro de Óleo", "Filtro para motor", new BigDecimal("25.90"), 10);
    }

    @Test
    void cadastrarShouldSaveAndReturnPeca() {
        CadastrarPecaUseCase useCase = new CadastrarPecaUseCase(pecaGateway);
        CadastrarPecaCommand command = new CadastrarPecaCommand(
                "Filtro de Óleo", "Filtro para motor", new BigDecimal("25.90"), 10);
        when(pecaGateway.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        Peca result = useCase.execute(command);

        assertNotNull(result);
        assertEquals("Filtro de Óleo", result.getNome());
        verify(pecaGateway).save(any(Peca.class));
    }

    @Test
    void atualizarShouldUpdateAndSavePeca() {
        AtualizarPecaUseCase useCase = new AtualizarPecaUseCase(pecaGateway);
        UUID id = UUID.randomUUID();
        Peca peca = buildPeca();
        when(pecaGateway.findById(id)).thenReturn(Optional.of(peca));
        when(pecaGateway.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarPecaCommand command = new AtualizarPecaCommand("Filtro Premium", "Novo", new BigDecimal("30.00"));
        Peca result = useCase.execute(id, command);

        assertEquals("Filtro Premium", result.getNome());
        verify(pecaGateway).save(any(Peca.class));
    }

    @Test
    void ajustarEstoqueFindsAdjustsAndSavesAndReturnsPeca() {
        AjustarEstoquePecaUseCase useCase = new AjustarEstoquePecaUseCase(pecaGateway);
        UUID id = UUID.randomUUID();
        Peca peca = buildPeca();
        when(pecaGateway.findById(id)).thenReturn(Optional.of(peca));
        when(pecaGateway.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        Peca result = useCase.execute(id, 5);

        assertEquals(15, result.getQuantidadeEstoque());
        verify(pecaGateway).save(any(Peca.class));
    }

    @Test
    void ajustarEstoqueThrowsRecursoNaoEncontradoExceptionWhenPecaNotFound() {
        AjustarEstoquePecaUseCase useCase = new AjustarEstoquePecaUseCase(pecaGateway);
        UUID id = UUID.randomUUID();
        when(pecaGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(id, 5));
        verify(pecaGateway, never()).save(any());
    }

    @Test
    void removerDesativaPecaSoftDelete() {
        RemoverPecaUseCase useCase = new RemoverPecaUseCase(pecaGateway);
        UUID id = UUID.randomUUID();
        Peca peca = buildPeca();
        when(pecaGateway.findById(id)).thenReturn(Optional.of(peca));
        when(pecaGateway.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(id);

        assertFalse(peca.isAtivo());
        verify(pecaGateway).save(peca);
    }

    @Test
    void buscarPorIdThrowsRecursoNaoEncontradoExceptionWhenNotFound() {
        BuscarPecaUseCase useCase = new BuscarPecaUseCase(pecaGateway);
        UUID id = UUID.randomUUID();
        when(pecaGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(id));
    }

    @Test
    void listarShouldReturnOnlyAtivos() {
        ListarPecasUseCase useCase = new ListarPecasUseCase(pecaGateway);
        when(pecaGateway.findAllAtivos()).thenReturn(List.of(buildPeca()));

        List<Peca> result = useCase.execute();

        assertEquals(1, result.size());
        verify(pecaGateway).findAllAtivos();
        verify(pecaGateway, never()).findAll();
    }
}
