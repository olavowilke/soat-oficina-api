package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DevolverPecasAoEstoqueIT extends BaseControllerIT {

    @Autowired private DevolverPecasAoEstoque devolver;
    @Autowired private PecaGateway pecaGateway;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void mesmaPecaEmDoisItensSomaNoBanco() {
        Peca peca = pecaGateway.save(Peca.novo("Filtro", "Filtro de óleo", new BigDecimal("25.00"), 8));
        OrdemServico os = OrdemServico.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                StatusOS.AGUARDANDO_APROVACAO, null, List.of(),
                List.of(ItemPeca.novo(peca.getId(), "Filtro", 2, new BigDecimal("25.00")),
                        ItemPeca.novo(peca.getId(), "Filtro", 3, new BigDecimal("25.00"))),
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        devolver.execute(os);

        assertThat(jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE id = ?", Integer.class,
                peca.getId())).isEqualTo(13);
    }

    @Test
    void devolucaoSobreEstoqueAlteradoPorForaSomaSobreOValorAtual() {
        Peca peca = pecaGateway.save(Peca.novo("Pastilha", "Freio", new BigDecimal("40.00"), 8));
        jdbc.update("UPDATE pecas SET quantidade_estoque = 20 WHERE id = ?", peca.getId());

        pecaGateway.devolverAoEstoque(peca.getId(), 2);

        assertThat(jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE id = ?", Integer.class,
                peca.getId())).isEqualTo(22);
    }
}
