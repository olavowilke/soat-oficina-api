package br.com.oficina.ordemservico.entities;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemServico {

    private UUID id;
    private UUID servicoId;
    private String nomeServico;
    private BigDecimal valorCobrado;

    private ItemServico() {}

    public static ItemServico novo(UUID servicoId, String nomeServico, BigDecimal valorCobrado) {
        if (servicoId == null) throw new IllegalArgumentException("ServicoId não pode ser nulo");
        if (nomeServico == null || nomeServico.isBlank()) throw new IllegalArgumentException("Nome do serviço não pode ser vazio");
        if (valorCobrado == null || valorCobrado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor cobrado deve ser maior que zero");
        }
        ItemServico item = new ItemServico();
        item.id = UUID.randomUUID();
        item.servicoId = servicoId;
        item.nomeServico = nomeServico;
        item.valorCobrado = valorCobrado;
        return item;
    }

    public static ItemServico reconstituir(UUID id, UUID servicoId, String nomeServico, BigDecimal valorCobrado) {
        ItemServico item = new ItemServico();
        item.id = id;
        item.servicoId = servicoId;
        item.nomeServico = nomeServico;
        item.valorCobrado = valorCobrado;
        return item;
    }

    public UUID getId() { return id; }
    public UUID getServicoId() { return servicoId; }
    public String getNomeServico() { return nomeServico; }
    public BigDecimal getValorCobrado() { return valorCobrado; }
}
