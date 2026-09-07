CREATE TABLE ordens_servico (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    veiculo_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    observacoes TEXT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    inicio_execucao TIMESTAMP,
    fim_execucao TIMESTAMP
);

CREATE TABLE itens_servico (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id),
    servico_id UUID NOT NULL,
    nome_servico VARCHAR(255) NOT NULL,
    valor_cobrado NUMERIC(10,2) NOT NULL
);

CREATE TABLE itens_peca (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id),
    peca_id UUID NOT NULL,
    nome_peca VARCHAR(255) NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario NUMERIC(10,2) NOT NULL
);
