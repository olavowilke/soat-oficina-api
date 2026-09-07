package br.com.oficina.ordemservico.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrdemServicoTest {

    private OrdemServico novaOS() {
        return OrdemServico.nova(UUID.randomUUID(), UUID.randomUUID(), "Revisão geral");
    }

    @Test
    void deveCriarOsComStatusRecebida() {
        OrdemServico os = novaOS();
        assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.getId()).isNotNull();
        assertThat(os.getCriadoEm()).isNotNull();
    }

    @Test
    void deveLancarExcecaoSemClienteId() {
        assertThatThrownBy(() -> OrdemServico.nova(null, UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ClienteId");
    }

    @Test
    void deveLancarExcecaoSemVeiculoId() {
        assertThatThrownBy(() -> OrdemServico.nova(UUID.randomUUID(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VeiculoId");
    }

    @Test
    void deveAdicionarServico() {
        OrdemServico os = novaOS();
        ItemServico item = ItemServico.novo(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.00"));
        os.adicionarServico(item);
        assertThat(os.getItensServico()).hasSize(1);
    }

    @Test
    void deveAdicionarPeca() {
        OrdemServico os = novaOS();
        ItemPeca item = ItemPeca.novo(UUID.randomUUID(), "Filtro de ar", 2, new BigDecimal("45.00"));
        os.adicionarPeca(item);
        assertThat(os.getItensPeca()).hasSize(1);
    }

    @Test
    void deveRemoverServico() {
        OrdemServico os = novaOS();
        ItemServico item = ItemServico.novo(UUID.randomUUID(), "Alinhamento", new BigDecimal("80.00"));
        os.adicionarServico(item);
        os.removerServico(item.getId());
        assertThat(os.getItensServico()).isEmpty();
    }

    @Test
    void deveLancarExcecaoAoRemoverServicoInexistente() {
        OrdemServico os = novaOS();
        assertThatThrownBy(() -> os.removerServico(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveCalcularOrcamentoTotalCorretamente() {
        OrdemServico os = novaOS();
        os.adicionarServico(ItemServico.novo(UUID.randomUUID(), "Revisão", new BigDecimal("200.00")));
        os.adicionarPeca(ItemPeca.novo(UUID.randomUUID(), "Filtro", 3, new BigDecimal("50.00")));

        BigDecimal total = os.calcularOrcamentoTotal();
        // 200 + (3 * 50) = 350
        assertThat(total).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    void deveAvancarStatusCorretamente() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveLancarExcecaoTransicaoInvalida() {
        OrdemServico os = novaOS();
        assertThatThrownBy(() -> os.avancarStatus(StatusOS.FINALIZADA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void deveRegistrarInicioExecucaoAoAprovarOrcamento() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
        os.aprovarOrcamento();
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getInicioExecucao()).isNotNull();
    }

    @Test
    void deveRegistrarFimExecucaoAoFinalizar() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
        os.aprovarOrcamento();
        os.avancarStatus(StatusOS.FINALIZADA);
        assertThat(os.getFimExecucao()).isNotNull();
    }

    @Test
    void deveRecusarOrcamentoComCancelamento() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
        os.recusarOrcamento();
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    void deveImpedirEdicaoDeOsEntregue() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
        os.aprovarOrcamento();
        os.avancarStatus(StatusOS.FINALIZADA);
        os.avancarStatus(StatusOS.ENTREGUE);

        ItemServico item = ItemServico.novo(UUID.randomUUID(), "Extra", new BigDecimal("100.00"));
        assertThatThrownBy(() -> os.adicionarServico(item))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENTREGUE");
    }
}
