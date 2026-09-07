package br.com.oficina.auth.usecases;

import br.com.oficina.auth.entities.Role;

public record LoginResult(String token, String username, Role role) {}
