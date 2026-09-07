package br.com.oficina.peca.presenters;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        String nome,
        String descricao,
        BigDecimal precoUnitario,
        int quantidadeEstoque,
        boolean ativo,
        LocalDateTime criadoEm
) {}
