package br.com.oficina.ordemservico.entities;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemPeca {

    private UUID id;
    private UUID pecaId;
    private String nomePeca;
    private int quantidade;
    private BigDecimal valorUnitario;

    private ItemPeca() {}

    public static ItemPeca novo(UUID pecaId, String nomePeca, int quantidade, BigDecimal valorUnitario) {
        if (pecaId == null) throw new IllegalArgumentException("PecaId não pode ser nulo");
        if (nomePeca == null || nomePeca.isBlank()) throw new IllegalArgumentException("Nome da peça não pode ser vazio");
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor unitário deve ser maior que zero");
        }
        ItemPeca item = new ItemPeca();
        item.id = UUID.randomUUID();
        item.pecaId = pecaId;
        item.nomePeca = nomePeca;
        item.quantidade = quantidade;
        item.valorUnitario = valorUnitario;
        return item;
    }

    public static ItemPeca reconstituir(UUID id, UUID pecaId, String nomePeca, int quantidade, BigDecimal valorUnitario) {
        ItemPeca item = new ItemPeca();
        item.id = id;
        item.pecaId = pecaId;
        item.nomePeca = nomePeca;
        item.quantidade = quantidade;
        item.valorUnitario = valorUnitario;
        return item;
    }

    public BigDecimal getSubtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId() { return id; }
    public UUID getPecaId() { return pecaId; }
    public String getNomePeca() { return nomePeca; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
}
