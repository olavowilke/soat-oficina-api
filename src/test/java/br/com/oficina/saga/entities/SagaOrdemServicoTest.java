package br.com.oficina.saga.entities;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaOrdemServicoTest {

    private final Instant t0 = Instant.parse("2026-11-09T12:00:00Z");
    private final Instant t1 = Instant.parse("2026-11-09T12:05:00Z");

    @Test
    void iniciarCriaSagaAguardandoOrcamento() {
        UUID osId = UUID.randomUUID();

        SagaOrdemServico saga = SagaOrdemServico.iniciar(osId, t0);

        assertThat(saga.getId()).isNotNull();
        assertThat(saga.getOsId()).isEqualTo(osId);
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        assertThat(saga.getCriadoEm()).isEqualTo(t0);
        assertThat(saga.getAtualizadoEm()).isEqualTo(t0);
    }

    @Test
    void moverDoEstadoEsperadoMudaEstadoEData() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(UUID.randomUUID(), t0);

        saga.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.AGUARDANDO_APROVACAO, t1);

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_APROVACAO);
        assertThat(saga.getAtualizadoEm()).isEqualTo(t1);
    }

    @Test
    void moverDeEstadoErradoLanca() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(UUID.randomUUID(), t0);

        assertThatThrownBy(() -> saga.mover(EstadoSaga.EM_EXECUCAO, EstadoSaga.CONCLUIDA, t1))
                .isInstanceOf(EstadoSagaInesperadoException.class)
                .hasMessageContaining("EM_EXECUCAO")
                .hasMessageContaining("AGUARDANDO_ORCAMENTO");
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
    }

    @Test
    void settersGuardamIdsLinkEMotivo() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(UUID.randomUUID(), t0);
        UUID orcamento = UUID.randomUUID();
        UUID pagamento = UUID.randomUUID();
        UUID execucao = UUID.randomUUID();

        saga.definirOrcamentoId(orcamento);
        saga.definirPagamentoId(pagamento);
        saga.definirExecucaoId(execucao);
        saga.definirLinkPagamento("https://pagamento/x");
        saga.definirMotivoFalha("REJEITADO");

        assertThat(saga.getOrcamentoId()).isEqualTo(orcamento);
        assertThat(saga.getPagamentoId()).isEqualTo(pagamento);
        assertThat(saga.getExecucaoId()).isEqualTo(execucao);
        assertThat(saga.getLinkPagamento()).isEqualTo("https://pagamento/x");
        assertThat(saga.getMotivoFalha()).isEqualTo("REJEITADO");
    }

    @Test
    void ativoCobreAteEstornandoEOsTerminaisNaoSaoAtivos() {
        assertThat(EstadoSaga.AGUARDANDO_ORCAMENTO.ativo()).isTrue();
        assertThat(EstadoSaga.AGUARDANDO_APROVACAO.ativo()).isTrue();
        assertThat(EstadoSaga.AGUARDANDO_PAGAMENTO.ativo()).isTrue();
        assertThat(EstadoSaga.AGUARDANDO_EXECUCAO.ativo()).isTrue();
        assertThat(EstadoSaga.EM_EXECUCAO.ativo()).isTrue();
        assertThat(EstadoSaga.ESTORNANDO.ativo()).isTrue();
        assertThat(EstadoSaga.CONCLUIDA.ativo()).isFalse();
        assertThat(EstadoSaga.COMPENSADA.ativo()).isFalse();
        assertThat(EstadoSaga.FALHOU.ativo()).isFalse();
    }

    @Test
    void reconstituirRestauraTodosOsCampos() {
        UUID id = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        UUID orc = UUID.randomUUID();
        UUID pag = UUID.randomUUID();
        UUID exe = UUID.randomUUID();

        SagaOrdemServico saga = SagaOrdemServico.reconstituir(id, osId, EstadoSaga.EM_EXECUCAO, orc, pag, exe,
                "link", "motivo", 3L, t0, t1);

        assertThat(saga.getId()).isEqualTo(id);
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.EM_EXECUCAO);
        assertThat(saga.getVersao()).isEqualTo(3L);
        assertThat(saga.getExecucaoId()).isEqualTo(exe);
        assertThat(saga.getAtualizadoEm()).isEqualTo(t1);
    }
}
