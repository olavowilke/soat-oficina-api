package br.com.oficina.saga.entities;

import java.time.Instant;
import java.util.UUID;

public class SagaOrdemServico {

    private UUID id;
    private UUID osId;
    private EstadoSaga estado;
    private UUID orcamentoId;
    private UUID pagamentoId;
    private UUID execucaoId;
    private String linkPagamento;
    private String motivoFalha;
    private Long versao;
    private Instant criadoEm;
    private Instant atualizadoEm;

    private SagaOrdemServico() {}

    public static SagaOrdemServico iniciar(UUID osId, Instant agora) {
        if (osId == null) throw new IllegalArgumentException("OsId não pode ser nulo");
        SagaOrdemServico saga = new SagaOrdemServico();
        saga.id = UUID.randomUUID();
        saga.osId = osId;
        saga.estado = EstadoSaga.AGUARDANDO_ORCAMENTO;
        saga.criadoEm = agora;
        saga.atualizadoEm = agora;
        return saga;
    }

    public static SagaOrdemServico reconstituir(UUID id, UUID osId, EstadoSaga estado, UUID orcamentoId,
                                                UUID pagamentoId, UUID execucaoId, String linkPagamento,
                                                String motivoFalha, Long versao, Instant criadoEm,
                                                Instant atualizadoEm) {
        SagaOrdemServico saga = new SagaOrdemServico();
        saga.id = id;
        saga.osId = osId;
        saga.estado = estado;
        saga.orcamentoId = orcamentoId;
        saga.pagamentoId = pagamentoId;
        saga.execucaoId = execucaoId;
        saga.linkPagamento = linkPagamento;
        saga.motivoFalha = motivoFalha;
        saga.versao = versao;
        saga.criadoEm = criadoEm;
        saga.atualizadoEm = atualizadoEm;
        return saga;
    }

    public void mover(EstadoSaga esperado, EstadoSaga novo, Instant agora) {
        if (estado != esperado) throw new EstadoSagaInesperadoException(esperado, estado);
        this.estado = novo;
        this.atualizadoEm = agora;
    }

    public void definirOrcamentoId(UUID orcamentoId) { this.orcamentoId = orcamentoId; }
    public void definirPagamentoId(UUID pagamentoId) { this.pagamentoId = pagamentoId; }
    public void definirExecucaoId(UUID execucaoId) { this.execucaoId = execucaoId; }
    public void definirLinkPagamento(String linkPagamento) { this.linkPagamento = linkPagamento; }
    public void definirMotivoFalha(String motivoFalha) { this.motivoFalha = motivoFalha; }

    public UUID getId() { return id; }
    public UUID getOsId() { return osId; }
    public EstadoSaga getEstado() { return estado; }
    public UUID getOrcamentoId() { return orcamentoId; }
    public UUID getPagamentoId() { return pagamentoId; }
    public UUID getExecucaoId() { return execucaoId; }
    public String getLinkPagamento() { return linkPagamento; }
    public String getMotivoFalha() { return motivoFalha; }
    public Long getVersao() { return versao; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
