package br.com.oficina.peca.usecases;

import java.math.BigDecimal;

public record CadastrarPecaCommand(
        String nome,
        String descricao,
        BigDecimal precoUnitario,
        int quantidadeEstoqueInicial
) {}
