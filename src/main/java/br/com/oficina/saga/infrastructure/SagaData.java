package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.EstadoSaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_ordem_servico")
@Getter
@Setter
@NoArgsConstructor
public class SagaData {

    @Id
    private UUID id;

    @Column(name = "os_id", nullable = false)
    private UUID osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSaga estado;

    @Column(name = "orcamento_id")
    private UUID orcamentoId;

    @Column(name = "pagamento_id")
    private UUID pagamentoId;

    @Column(name = "execucao_id")
    private UUID execucaoId;

    @Column(name = "link_pagamento", length = 500)
    private String linkPagamento;

    @Column(name = "motivo_falha", length = 300)
    private String motivoFalha;

    @Version
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
