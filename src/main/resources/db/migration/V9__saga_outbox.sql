CREATE TABLE saga_ordem_servico (
    id UUID PRIMARY KEY,
    os_id UUID NOT NULL REFERENCES ordens_servico(id),
    estado VARCHAR(30) NOT NULL,
    orcamento_id UUID,
    pagamento_id UUID,
    execucao_id UUID,
    link_pagamento VARCHAR(500),
    motivo_falha VARCHAR(300),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);

-- No máximo uma saga ativa por OS.
CREATE UNIQUE INDEX ux_saga_os_ativa ON saga_ordem_servico (os_id)
    WHERE estado IN ('AGUARDANDO_ORCAMENTO', 'AGUARDANDO_APROVACAO', 'AGUARDANDO_PAGAMENTO',
                     'AGUARDANDO_EXECUCAO', 'EM_EXECUCAO', 'ESTORNANDO');

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    destino VARCHAR(30) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    saga_id UUID NOT NULL,
    os_id UUID NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    enviado_em TIMESTAMPTZ
);

CREATE INDEX ix_outbox_pendentes ON outbox (enviado_em) WHERE enviado_em IS NULL;

CREATE TABLE mensagens_processadas (
    message_id UUID PRIMARY KEY,
    processada_em TIMESTAMPTZ NOT NULL
);
