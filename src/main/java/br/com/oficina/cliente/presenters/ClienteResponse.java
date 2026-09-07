package br.com.oficina.cliente.presenters;

import br.com.oficina.cliente.entities.TipoDocumento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String documento,
        TipoDocumento tipoDocumento,
        boolean ativo,
        LocalDateTime criadoEm
) {}
