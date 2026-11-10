package br.com.oficina.saga.infrastructure;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.oficina.saga.entities.SagaJaAtivaException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaGatewayImplIT extends BaseControllerIT {

    @Autowired
    private SagaGateway sagaGateway;

    @Autowired
    private OrdemServicoGateway ordemServicoGateway;

    private UUID novaOs() {
        UUID[] ids = inserirClienteEVeiculo();
        return ordemServicoGateway.save(OrdemServico.nova(ids[0], ids[1], null)).getId();
    }

    @Test
    void salvaEBuscaPorIdEPorOs() {
        UUID osId = novaOs();
        SagaOrdemServico saga = SagaOrdemServico.iniciar(osId, Instant.now());
        saga.definirLinkPagamento("https://pagamento/x");

        SagaOrdemServico salva = sagaGateway.salvar(saga);

        assertThat(sagaGateway.buscarPorId(salva.getId())).get().satisfies(s -> {
            assertThat(s.getOsId()).isEqualTo(osId);
            assertThat(s.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
            assertThat(s.getLinkPagamento()).isEqualTo("https://pagamento/x");
        });
        assertThat(sagaGateway.buscarAtivaPorOsId(osId)).isPresent();
        assertThat(sagaGateway.buscarPorOsId(osId)).hasSize(1);
        assertThat(sagaGateway.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void sagaTerminalNaoEhAtivaEPermiteNovaSaga() {
        UUID osId = novaOs();
        SagaOrdemServico primeira = sagaGateway.salvar(SagaOrdemServico.iniciar(osId, Instant.now()));
        primeira.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.FALHOU, Instant.now());
        sagaGateway.salvar(primeira);

        assertThat(sagaGateway.buscarAtivaPorOsId(osId)).isEmpty();

        sagaGateway.salvar(SagaOrdemServico.iniciar(osId, Instant.now()));

        assertThat(sagaGateway.buscarPorOsId(osId)).hasSize(2);
        assertThat(sagaGateway.buscarAtivaPorOsId(osId)).isPresent();
    }

    @Test
    void segundaSagaAtivaParaMesmaOsViolaUnicidade() {
        UUID osId = novaOs();
        sagaGateway.salvar(SagaOrdemServico.iniciar(osId, Instant.now()));

        assertThatThrownBy(() -> sagaGateway.salvar(SagaOrdemServico.iniciar(osId, Instant.now())))
                .isInstanceOf(SagaJaAtivaException.class);
    }

    @Test
    void escritaComVersaoVelhaFalha() {
        UUID osId = novaOs();
        SagaOrdemServico salva = sagaGateway.salvar(SagaOrdemServico.iniciar(osId, Instant.now()));
        SagaOrdemServico copiaA = sagaGateway.buscarPorId(salva.getId()).orElseThrow();
        SagaOrdemServico copiaB = sagaGateway.buscarPorId(salva.getId()).orElseThrow();

        copiaA.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.AGUARDANDO_APROVACAO, Instant.now());
        sagaGateway.salvar(copiaA);
        copiaB.mover(EstadoSaga.AGUARDANDO_ORCAMENTO, EstadoSaga.FALHOU, Instant.now());

        assertThatThrownBy(() -> sagaGateway.salvar(copiaB))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
