package br.com.oficina.ordemservico.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({
            "EM_DIAGNOSTICO,AGUARDANDO_ORCAMENTO",
            "AGUARDANDO_ORCAMENTO,AGUARDANDO_APROVACAO",
            "AGUARDANDO_ORCAMENTO,EM_DIAGNOSTICO",
            "AGUARDANDO_APROVACAO,AGUARDANDO_PAGAMENTO",
            "AGUARDANDO_APROVACAO,CANCELADA",
            "AGUARDANDO_PAGAMENTO,AGUARDANDO_EXECUCAO",
            "AGUARDANDO_PAGAMENTO,CANCELADA",
            "AGUARDANDO_EXECUCAO,EM_EXECUCAO",
            "AGUARDANDO_EXECUCAO,CANCELADA"
    })
    void devePermitirTransicoesDaSaga(StatusOS de, StatusOS para) {
        assertThat(de.podeTransicionarPara(para)).isTrue();
    }

    @Test
    void deveRejeitarPularEtapasDaSaga() {
        assertThat(StatusOS.EM_DIAGNOSTICO.podeTransicionarPara(StatusOS.AGUARDANDO_APROVACAO)).isFalse();
        assertThat(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.EM_EXECUCAO)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "RECEBIDA,EM_DIAGNOSTICO,true",
            "FINALIZADA,ENTREGUE,true",
            "EM_DIAGNOSTICO,AGUARDANDO_ORCAMENTO,false",
            "AGUARDANDO_APROVACAO,CANCELADA,false"
    })
    void transicaoManual(StatusOS de, StatusOS para, boolean esperado) {
        assertThat(de.transicaoManualPermitida(para)).isEqualTo(esperado);
    }

    @Test
    void ordemDeListagemMantemExecucaoPrimeiro() {
        assertThat(StatusOS.EM_EXECUCAO.prioridadeListagem()).isEqualTo(1);
        assertThat(StatusOS.AGUARDANDO_EXECUCAO.prioridadeListagem()).isEqualTo(2);
        assertThat(StatusOS.AGUARDANDO_PAGAMENTO.prioridadeListagem()).isEqualTo(3);
        assertThat(StatusOS.AGUARDANDO_APROVACAO.prioridadeListagem()).isEqualTo(4);
        assertThat(StatusOS.AGUARDANDO_ORCAMENTO.prioridadeListagem()).isEqualTo(5);
        assertThat(StatusOS.EM_DIAGNOSTICO.prioridadeListagem()).isEqualTo(6);
        assertThat(StatusOS.RECEBIDA.prioridadeListagem()).isEqualTo(7);
        assertThat(StatusOS.FINALIZADA.prioridadeListagem()).isEqualTo(99);
        assertThat(StatusOS.ENTREGUE.prioridadeListagem()).isEqualTo(99);
        assertThat(StatusOS.CANCELADA.prioridadeListagem()).isEqualTo(99);
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
