package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaJaAtivaException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.SagaGateway;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SagaGatewayImpl implements SagaGateway {

    private static final List<EstadoSaga> ATIVOS =
            Arrays.stream(EstadoSaga.values()).filter(EstadoSaga::ativo).toList();

    private final SagaJpaRepository repository;

    public SagaGatewayImpl(SagaJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Grava a saga. {@code saveAndFlush} faz a violação do índice único de saga
     * ativa e o conflito de versão aparecerem aqui, não só no commit.
     */
    @Override
    public SagaOrdemServico salvar(SagaOrdemServico saga) {
        try {
            return paraDominio(repository.saveAndFlush(paraData(saga)));
        } catch (DataIntegrityViolationException e) {
            if (violaIndiceDeSagaAtiva(e)) throw new SagaJaAtivaException(saga.getOsId());
            throw e;
        }
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorId(UUID id) {
        return repository.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<SagaOrdemServico> buscarAtivaPorOsId(UUID osId) {
        return repository.findFirstByOsIdAndEstadoIn(osId, ATIVOS).map(this::paraDominio);
    }

    @Override
    public List<SagaOrdemServico> buscarPorOsId(UUID osId) {
        return repository.findByOsIdOrderByCriadoEmAsc(osId).stream().map(this::paraDominio).toList();
    }

    private static boolean violaIndiceDeSagaAtiva(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("ux_saga_os_ativa")) return true;
        }
        return false;
    }

    private SagaData paraData(SagaOrdemServico s) {
        SagaData d = new SagaData();
        d.setId(s.getId());
        d.setOsId(s.getOsId());
        d.setEstado(s.getEstado());
        d.setOrcamentoId(s.getOrcamentoId());
        d.setPagamentoId(s.getPagamentoId());
        d.setExecucaoId(s.getExecucaoId());
        d.setLinkPagamento(s.getLinkPagamento());
        d.setMotivoFalha(s.getMotivoFalha());
        d.setVersao(s.getVersao());
        d.setCriadoEm(s.getCriadoEm());
        d.setAtualizadoEm(s.getAtualizadoEm());
        return d;
    }

    private SagaOrdemServico paraDominio(SagaData d) {
        return SagaOrdemServico.reconstituir(d.getId(), d.getOsId(), d.getEstado(), d.getOrcamentoId(),
                d.getPagamentoId(), d.getExecucaoId(), d.getLinkPagamento(), d.getMotivoFalha(),
                d.getVersao(), d.getCriadoEm(), d.getAtualizadoEm());
    }
}
