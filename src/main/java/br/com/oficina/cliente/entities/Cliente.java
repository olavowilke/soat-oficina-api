package br.com.oficina.cliente.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Cliente {

    private UUID id;
    private String nome;
    private String email;
    private String telefone;
    private Documento documento;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private Cliente() {}

    public static Cliente novo(String nome, String email, String telefone, Documento documento) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente não pode ser vazio");
        }
        Cliente cliente = new Cliente();
        cliente.id = UUID.randomUUID();
        cliente.nome = nome;
        cliente.email = email;
        cliente.telefone = telefone;
        cliente.documento = documento;
        cliente.ativo = true;
        cliente.criadoEm = LocalDateTime.now();
        cliente.atualizadoEm = LocalDateTime.now();
        return cliente;
    }

    public static Cliente reconstituir(UUID id, String nome, String email, String telefone,
                                       Documento documento, boolean ativo,
                                       LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        Cliente cliente = new Cliente();
        cliente.id = id;
        cliente.nome = nome;
        cliente.email = email;
        cliente.telefone = telefone;
        cliente.documento = documento;
        cliente.ativo = ativo;
        cliente.criadoEm = criadoEm;
        cliente.atualizadoEm = atualizadoEm;
        return cliente;
    }

    public void atualizar(String nome, String email, String telefone) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente não pode ser vazio");
        }
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public Documento getDocumento() { return documento; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
