package br.com.oficina.peca.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Peca {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;
    private int quantidadeEstoque;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private Peca() {}

    public static Peca novo(String nome, String descricao, BigDecimal precoUnitario, int quantidadeEstoqueInicial) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da peça não pode ser vazio");
        }
        if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário da peça deve ser maior que zero");
        }
        if (quantidadeEstoqueInicial < 0) {
            throw new IllegalArgumentException("Quantidade inicial de estoque não pode ser negativa");
        }
        Peca peca = new Peca();
        peca.id = UUID.randomUUID();
        peca.nome = nome;
        peca.descricao = descricao;
        peca.precoUnitario = precoUnitario;
        peca.quantidadeEstoque = quantidadeEstoqueInicial;
        peca.ativo = true;
        peca.criadoEm = LocalDateTime.now();
        peca.atualizadoEm = LocalDateTime.now();
        return peca;
    }

    public static Peca reconstituir(UUID id, String nome, String descricao, BigDecimal precoUnitario,
                                    int quantidadeEstoque, boolean ativo,
                                    LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        Peca peca = new Peca();
        peca.id = id;
        peca.nome = nome;
        peca.descricao = descricao;
        peca.precoUnitario = precoUnitario;
        peca.quantidadeEstoque = quantidadeEstoque;
        peca.ativo = ativo;
        peca.criadoEm = criadoEm;
        peca.atualizadoEm = atualizadoEm;
        return peca;
    }

    public void atualizar(String nome, String descricao, BigDecimal precoUnitario) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da peça não pode ser vazio");
        }
        if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário da peça deve ser maior que zero");
        }
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = precoUnitario;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void ajustarEstoque(int quantidade) {
        int novoEstoque = this.quantidadeEstoque + quantidade;
        if (novoEstoque < 0) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente. Disponível: " + this.quantidadeEstoque + ", solicitado: " + Math.abs(quantidade));
        }
        this.quantidadeEstoque = novoEstoque;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidadeEstoque() { return quantidadeEstoque; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
