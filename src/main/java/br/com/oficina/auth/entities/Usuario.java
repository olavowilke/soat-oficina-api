package br.com.oficina.auth.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Usuario {

    private UUID id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean ativo;
    private LocalDateTime criadoEm;

    private Usuario() {}

    public static Usuario novo(String username, String passwordHash, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username não pode ser vazio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("PasswordHash não pode ser vazio");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role não pode ser nula");
        }
        Usuario u = new Usuario();
        u.id = UUID.randomUUID();
        u.username = username;
        u.passwordHash = passwordHash;
        u.role = role;
        u.ativo = true;
        u.criadoEm = LocalDateTime.now();
        return u;
    }

    public static Usuario reconstituir(UUID id, String username, String passwordHash, Role role,
                                       boolean ativo, LocalDateTime criadoEm) {
        Usuario u = new Usuario();
        u.id = id;
        u.username = username;
        u.passwordHash = passwordHash;
        u.role = role;
        u.ativo = ativo;
        u.criadoEm = criadoEm;
        return u;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
