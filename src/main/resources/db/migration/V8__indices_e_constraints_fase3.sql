-- Fase 3: ajustes de modelo identificados na revisao formal do schema
-- (ver infra-database/docs/modelo-dados.md). idx_veiculos_cliente_id e a FK
-- de veiculos.cliente_id ja existiam desde V3 e nao sao repetidos aqui.

-- Indices de apoio a consultas frequentes (fila de atendimento por status,
-- ordens por cliente/veiculo, itens de uma ordem).
CREATE INDEX IF NOT EXISTS idx_ordens_servico_status ON ordens_servico (status);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_cliente ON ordens_servico (cliente_id);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_veiculo ON ordens_servico (veiculo_id);
CREATE INDEX IF NOT EXISTS idx_itens_servico_ordem ON itens_servico (ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_itens_peca_ordem ON itens_peca (ordem_servico_id);

-- FKs faltantes: as colunas ja existiam como UUID NOT NULL, mas sem
-- REFERENCES, permitindo registros orfaos. Postgres nao aceita
-- "ADD CONSTRAINT IF NOT EXISTS", entao o guard fica no bloco DO.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ordens_servico_cliente'
    ) THEN
        ALTER TABLE ordens_servico
            ADD CONSTRAINT fk_ordens_servico_cliente
            FOREIGN KEY (cliente_id) REFERENCES clientes (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ordens_servico_veiculo'
    ) THEN
        ALTER TABLE ordens_servico
            ADD CONSTRAINT fk_ordens_servico_veiculo
            FOREIGN KEY (veiculo_id) REFERENCES veiculos (id);
    END IF;
END $$;

-- CHECKs de quantidade/valores positivos (mesmo guard, pelo mesmo motivo).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_servicos_preco_positivo'
    ) THEN
        ALTER TABLE servicos
            ADD CONSTRAINT chk_servicos_preco_positivo CHECK (preco > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_servicos_tempo_positivo'
    ) THEN
        ALTER TABLE servicos
            ADD CONSTRAINT chk_servicos_tempo_positivo CHECK (tempo_estimado_minutos > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_pecas_preco_unitario_positivo'
    ) THEN
        ALTER TABLE pecas
            ADD CONSTRAINT chk_pecas_preco_unitario_positivo CHECK (preco_unitario > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_pecas_quantidade_estoque_nao_negativa'
    ) THEN
        ALTER TABLE pecas
            ADD CONSTRAINT chk_pecas_quantidade_estoque_nao_negativa CHECK (quantidade_estoque >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_itens_servico_valor_cobrado_nao_negativo'
    ) THEN
        ALTER TABLE itens_servico
            ADD CONSTRAINT chk_itens_servico_valor_cobrado_nao_negativo CHECK (valor_cobrado >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_itens_peca_quantidade_positiva'
    ) THEN
        ALTER TABLE itens_peca
            ADD CONSTRAINT chk_itens_peca_quantidade_positiva CHECK (quantidade > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_itens_peca_valor_unitario_nao_negativo'
    ) THEN
        ALTER TABLE itens_peca
            ADD CONSTRAINT chk_itens_peca_valor_unitario_nao_negativo CHECK (valor_unitario >= 0);
    END IF;
END $$;
