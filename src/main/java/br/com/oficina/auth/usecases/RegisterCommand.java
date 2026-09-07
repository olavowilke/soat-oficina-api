package br.com.oficina.auth.usecases;

import br.com.oficina.auth.entities.Role;

public record RegisterCommand(String username, String password, Role role) {}
