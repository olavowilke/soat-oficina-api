package br.com.oficina.servico.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Servico {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int tempoEstimadoMinutos;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private Servico() {}

    public static Servico novo(String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do serviço não pode ser vazio");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço do serviço deve ser maior que zero");
        }
        if (tempoEstimadoMinutos <= 0) {
            throw new IllegalArgumentException("Tempo estimado deve ser maior que zero");
        }
        Servico servico = new Servico();
        servico.id = UUID.randomUUID();
        servico.nome = nome;
        servico.descricao = descricao;
        servico.preco = preco;
        servico.tempoEstimadoMinutos = tempoEstimadoMinutos;
        servico.ativo = true;
        servico.criadoEm = LocalDateTime.now();
        servico.atualizadoEm = LocalDateTime.now();
        return servico;
    }

    public static Servico reconstituir(UUID id, String nome, String descricao, BigDecimal preco,
                                       int tempoEstimadoMinutos, boolean ativo,
                                       LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        Servico servico = new Servico();
        servico.id = id;
        servico.nome = nome;
        servico.descricao = descricao;
        servico.preco = preco;
        servico.tempoEstimadoMinutos = tempoEstimadoMinutos;
        servico.ativo = ativo;
        servico.criadoEm = criadoEm;
        servico.atualizadoEm = atualizadoEm;
        return servico;
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do serviço não pode ser vazio");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço do serviço deve ser maior que zero");
        }
        if (tempoEstimadoMinutos <= 0) {
            throw new IllegalArgumentException("Tempo estimado deve ser maior que zero");
        }
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public int getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
