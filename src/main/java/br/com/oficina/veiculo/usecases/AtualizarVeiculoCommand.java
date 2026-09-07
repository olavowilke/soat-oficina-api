package br.com.oficina.veiculo.usecases;

public record AtualizarVeiculoCommand(
        String marca,
        String modelo,
        int ano,
        String cor
) {}
