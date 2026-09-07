package br.com.oficina.veiculo.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Veiculo {

    private UUID id;
    private UUID clienteId;
    private Placa placa;
    private String marca;
    private String modelo;
    private int ano;
    private String cor;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private Veiculo() {}

    public static Veiculo novo(UUID clienteId, Placa placa, String marca, String modelo, int ano, String cor) {
        if (clienteId == null) {
            throw new IllegalArgumentException("ClienteId não pode ser nulo");
        }
        if (placa == null) {
            throw new IllegalArgumentException("Placa não pode ser nula");
        }
        if (marca == null || marca.isBlank()) {
            throw new IllegalArgumentException("Marca não pode ser vazia");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException("Modelo não pode ser vazio");
        }
        Veiculo veiculo = new Veiculo();
        veiculo.id = UUID.randomUUID();
        veiculo.clienteId = clienteId;
        veiculo.placa = placa;
        veiculo.marca = marca;
        veiculo.modelo = modelo;
        veiculo.ano = ano;
        veiculo.cor = cor;
        veiculo.criadoEm = LocalDateTime.now();
        veiculo.atualizadoEm = LocalDateTime.now();
        return veiculo;
    }

    public static Veiculo reconstituir(UUID id, UUID clienteId, Placa placa, String marca,
                                       String modelo, int ano, String cor,
                                       LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        Veiculo veiculo = new Veiculo();
        veiculo.id = id;
        veiculo.clienteId = clienteId;
        veiculo.placa = placa;
        veiculo.marca = marca;
        veiculo.modelo = modelo;
        veiculo.ano = ano;
        veiculo.cor = cor;
        veiculo.criadoEm = criadoEm;
        veiculo.atualizadoEm = atualizadoEm;
        return veiculo;
    }

    public void atualizar(String marca, String modelo, int ano, String cor) {
        if (marca == null || marca.isBlank()) {
            throw new IllegalArgumentException("Marca não pode ser vazia");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException("Modelo não pode ser vazio");
        }
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.cor = cor;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getClienteId() { return clienteId; }
    public Placa getPlaca() { return placa; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getAno() { return ano; }
    public String getCor() { return cor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
