package br.com.oficina.cliente.controllers;

import br.com.oficina.cliente.entities.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CadastrarClienteRequest(
        @NotBlank String nome,
        @Email String email,
        String telefone,
        @NotBlank String documento,
        @NotNull TipoDocumento tipoDocumento
) {}
