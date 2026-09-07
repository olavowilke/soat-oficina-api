package br.com.oficina.auth.presenters;

import br.com.oficina.auth.entities.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(UUID id, String username, Role role, boolean ativo, LocalDateTime criadoEm) {}
