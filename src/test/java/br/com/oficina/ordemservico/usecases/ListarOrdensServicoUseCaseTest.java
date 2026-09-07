package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarOrdensServicoUseCaseTest {

    @Mock
    private OrdemServicoGateway gateway;

    @InjectMocks
    private ListarOrdensServicoUseCase listar;

    private OrdemServico os(StatusOS status, LocalDateTime criadoEm) {
        return OrdemServico.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                status, null, List.of(), List.of(), criadoEm, criadoEm, null, null);
    }

    @Test
    void deveOrdenarPorPrioridadeDeStatusEExcluirTerminais() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        OrdemServico recebida = os(StatusOS.RECEBIDA, base.plusHours(1));
        OrdemServico diagnostico = os(StatusOS.EM_DIAGNOSTICO, base.plusHours(2));
        OrdemServico aguardando = os(StatusOS.AGUARDANDO_APROVACAO, base.plusHours(3));
        OrdemServico execucao = os(StatusOS.EM_EXECUCAO, base.plusHours(4));
        OrdemServico finalizada = os(StatusOS.FINALIZADA, base.plusHours(5));
        OrdemServico entregue = os(StatusOS.ENTREGUE, base.plusHours(6));
        OrdemServico cancelada = os(StatusOS.CANCELADA, base.plusHours(7));

        when(gateway.findAll()).thenReturn(List.of(
                recebida, finalizada, execucao, entregue, aguardando, cancelada, diagnostico));

        List<OrdemServico> resultado = listar.execute(null, null);

        // Terminais excluídas; ativas ordenadas por prioridade (execução primeiro)
        assertThat(resultado).containsExactly(execucao, aguardando, diagnostico, recebida);
    }

    @Test
    void deveOrdenarMaisAntigasPrimeiroDentroDoMesmoStatus() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        OrdemServico maisNova = os(StatusOS.EM_EXECUCAO, base.plusHours(5));
        OrdemServico maisAntiga = os(StatusOS.EM_EXECUCAO, base.plusHours(1));

        when(gateway.findAll()).thenReturn(List.of(maisNova, maisAntiga));

        List<OrdemServico> resultado = listar.execute(null, null);

        assertThat(resultado).containsExactly(maisAntiga, maisNova);
    }

    @Test
    void filtroExplicitoPorStatusTerminalDeveRetornarAsOrdens() {
        OrdemServico finalizada = os(StatusOS.FINALIZADA, LocalDateTime.now());
        when(gateway.findByStatus(StatusOS.FINALIZADA)).thenReturn(List.of(finalizada));

        List<OrdemServico> resultado = listar.execute(null, StatusOS.FINALIZADA);

        assertThat(resultado).containsExactly(finalizada);
    }

    @Test
    void filtroPorClienteDeveExcluirTerminaisEOrdenar() {
        UUID clienteId = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        OrdemServico ativa = OrdemServico.reconstituir(UUID.randomUUID(), clienteId, UUID.randomUUID(),
                StatusOS.RECEBIDA, null, List.of(), List.of(), base, base, null, null);
        OrdemServico terminal = OrdemServico.reconstituir(UUID.randomUUID(), clienteId, UUID.randomUUID(),
                StatusOS.ENTREGUE, null, List.of(), List.of(), base, base, null, null);

        when(gateway.findByClienteId(clienteId)).thenReturn(List.of(ativa, terminal));

        List<OrdemServico> resultado = listar.execute(clienteId, null);

        assertThat(resultado).containsExactly(ativa);
    }
}
