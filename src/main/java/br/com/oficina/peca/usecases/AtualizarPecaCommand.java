package br.com.oficina.peca.usecases;

import java.math.BigDecimal;

public record AtualizarPecaCommand(
        String nome,
        String descricao,
        BigDecimal precoUnitario
) {}
