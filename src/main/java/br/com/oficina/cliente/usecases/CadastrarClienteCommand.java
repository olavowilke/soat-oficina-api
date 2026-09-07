package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.TipoDocumento;

public record CadastrarClienteCommand(
        String nome,
        String email,
        String telefone,
        String documento,
        TipoDocumento tipoDocumento
) {}
