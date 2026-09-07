package br.com.oficina.servico.usecases;

import java.math.BigDecimal;

public record CadastrarServicoCommand(
        String nome,
        String descricao,
        BigDecimal preco,
        int tempoEstimadoMinutos
) {}
