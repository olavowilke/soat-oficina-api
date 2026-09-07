package br.com.oficina.veiculo.usecases;

import java.util.UUID;

public record CadastrarVeiculoCommand(
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        int ano,
        String cor
) {}
