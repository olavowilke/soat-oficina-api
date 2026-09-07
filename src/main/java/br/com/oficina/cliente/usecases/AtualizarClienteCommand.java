package br.com.oficina.cliente.usecases;

public record AtualizarClienteCommand(
        String nome,
        String email,
        String telefone
) {}
