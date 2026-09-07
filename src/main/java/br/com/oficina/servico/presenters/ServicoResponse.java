package br.com.oficina.servico.presenters;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        int tempoEstimadoMinutos,
        boolean ativo,
        LocalDateTime criadoEm
) {}
