package br.com.oficina.veiculo.entities;

import java.util.regex.Pattern;

public record Placa(String valor) {

    private static final Pattern OLD_FORMAT = Pattern.compile("[A-Z]{3}[0-9]{4}");
    private static final Pattern MERCOSUL_FORMAT = Pattern.compile("[A-Z]{3}[0-9][A-Z][0-9]{2}");

    public Placa {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Placa não pode ser vazia");
        }
        valor = valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (valor.length() != 7) {
            throw new IllegalArgumentException("Placa inválida: deve ter 7 caracteres alfanuméricos");
        }
        if (!OLD_FORMAT.matcher(valor).matches() && !MERCOSUL_FORMAT.matcher(valor).matches()) {
            throw new IllegalArgumentException("Placa inválida: formato não reconhecido: " + valor);
        }
    }
}
