package br.com.oficina.ordemservico.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StatusOSTest {

    @Test
    void devePermitirTransicaoRecebidaParaEmDiagnostico() {
        assertThat(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.EM_DIAGNOSTICO)).isTrue();
    }

    @Test
    void deveRejeitarTransicaoInvalida_RecebidaParaFinalizada() {
        assertThat(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.FINALIZADA)).isFalse();
    }

    @Test
    void devePermitirTransicaoEmDiagnosticoParaAguardandoAprovacao() {
        assertThat(StatusOS.EM_DIAGNOSTICO.podeTransicionarPara(StatusOS.AGUARDANDO_APROVACAO)).isTrue();
    }

    @Test
    void devePermitirAprovarOrcamento_AguardandoParaEmExecucao() {
        assertThat(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.EM_EXECUCAO)).isTrue();
    }

    @Test
    void devePermitirRecusarOrcamento_AguardandoParaCancelada() {
        assertThat(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.CANCELADA)).isTrue();
    }

    @Test
    void devePermitirTransicaoEmExecucaoParaFinalizada() {
        assertThat(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.FINALIZADA)).isTrue();
    }

    @Test
    void devePermitirTransicaoFinalizadaParaEntregue() {
        assertThat(StatusOS.FINALIZADA.podeTransicionarPara(StatusOS.ENTREGUE)).isTrue();
    }

    @Test
    void deveRejeitarTransicaoDeEntregue() {
        for (StatusOS s : StatusOS.values()) {
            assertThat(StatusOS.ENTREGUE.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    void deveRejeitarTransicaoDeCancelada() {
        for (StatusOS s : StatusOS.values()) {
            assertThat(StatusOS.CANCELADA.podeTransicionarPara(s)).isFalse();
        }
    }
}
