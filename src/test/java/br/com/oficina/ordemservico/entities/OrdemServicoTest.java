package br.com.oficina.ordemservico.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private OrdemServico osAguardandoExecucao() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.adicionarServico(ItemServico.novo(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.00")));
        os.enviarParaOrcamento();
        os.registrarOrcamentoGerado();
        os.registrarAprovacao();
        os.registrarPagamentoConfirmado();
        return os;
    }

    private OrdemServico osEmDiagnosticoComItem() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.adicionarServico(ItemServico.novo(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.00")));
        return os;
    }

    @Test
    void deveSeguirFluxoFelizDaSaga() {
        OrdemServico os = osAguardandoExecucao();
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);

        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 1, 1, 12, 0);
        os.iniciarExecucao(inicio);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getInicioExecucao()).isEqualTo(inicio);

        os.finalizarExecucao(fim);
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getFimExecucao()).isEqualTo(fim);
    }

    @Test
    void enviarParaOrcamentoSemItensFalha() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        assertThatThrownBy(os::enviarParaOrcamento)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("item");
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void enviarParaOrcamentoComSomentePecaFunciona() {
        OrdemServico os = novaOS();
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.adicionarPeca(ItemPeca.novo(UUID.randomUUID(), "Filtro", 1, new BigDecimal("45.00")));
        os.enviarParaOrcamento();
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_ORCAMENTO);
    }

    @Test
    void adicionarPecaDepoisDeEnviarOrcamentoFalha() {
        OrdemServico os = osEmDiagnosticoComItem();
        os.enviarParaOrcamento();

        ItemPeca peca = ItemPeca.novo(UUID.randomUUID(), "Filtro", 1, new BigDecimal("45.00"));
        assertThatThrownBy(() -> os.adicionarPeca(peca)).isInstanceOf(IllegalStateException.class);
        ItemServico servico = ItemServico.novo(UUID.randomUUID(), "Extra", new BigDecimal("10.00"));
        assertThatThrownBy(() -> os.adicionarServico(servico)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> os.removerPeca(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> os.removerServico(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveVoltarParaDiagnosticoQuandoOrcamentoFalha() {
        OrdemServico os = osEmDiagnosticoComItem();
        os.enviarParaOrcamento();
        os.voltarParaDiagnostico();
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveCancelarEmAguardandoAprovacao() {
        OrdemServico os = osEmDiagnosticoComItem();
        os.enviarParaOrcamento();
        os.registrarOrcamentoGerado();
        os.cancelar();
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    void naoPodeCancelarEmExecucao() {
        OrdemServico os = osAguardandoExecucao();
        os.iniciarExecucao(LocalDateTime.now());
        assertThatThrownBy(os::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoPodeRegistrarPagamentoSemAprovacao() {
        OrdemServico os = novaOS();
        assertThatThrownBy(os::registrarPagamentoConfirmado).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveImpedirEdicaoDeOsEntregue() {
        OrdemServico os = osAguardandoExecucao();
        os.iniciarExecucao(LocalDateTime.now());
        os.finalizarExecucao(LocalDateTime.now());
        os.avancarStatus(StatusOS.ENTREGUE);

        ItemServico item = ItemServico.novo(UUID.randomUUID(), "Extra", new BigDecimal("100.00"));
        assertThatThrownBy(() -> os.adicionarServico(item))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENTREGUE");
    }
}
