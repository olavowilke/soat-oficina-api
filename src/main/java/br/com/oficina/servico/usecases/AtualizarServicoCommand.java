package br.com.oficina.servico.usecases;

import java.math.BigDecimal;

public record AtualizarServicoCommand(
        String nome,
        String descricao,
        BigDecimal preco,
        int tempoEstimadoMinutos
) {}
