package br.com.oficina.veiculo.presenters;

import java.time.LocalDateTime;
import java.util.UUID;

public record VeiculoResponse(
        UUID id,
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        int ano,
        String cor,
        LocalDateTime criadoEm
) {}
