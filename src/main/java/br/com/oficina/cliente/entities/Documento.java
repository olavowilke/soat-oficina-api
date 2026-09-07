package br.com.oficina.cliente.entities;

public record Documento(String numero, TipoDocumento tipo) {

    public Documento {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Número do documento não pode ser vazio");
        }
        numero = numero.replaceAll("[^0-9]", "");
        if (tipo == TipoDocumento.CPF) {
            if (!isValidCPF(numero)) {
                throw new IllegalArgumentException("CPF inválido: " + numero);
            }
        } else if (tipo == TipoDocumento.CNPJ) {
            if (!isValidCNPJ(numero)) {
                throw new IllegalArgumentException("CNPJ inválido: " + numero);
            }
        } else {
            throw new IllegalArgumentException("Tipo de documento inválido");
        }
    }

    public static boolean isValidCPF(String cpf) {
        if (cpf == null) return false;
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * (10 - i);
        }
        int remainder = sum % 11;
        int first = remainder < 2 ? 0 : 11 - remainder;
        if (first != (digits.charAt(9) - '0')) return false;

        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (digits.charAt(i) - '0') * (11 - i);
        }
        remainder = sum % 11;
        int second = remainder < 2 ? 0 : 11 - remainder;
        return second == (digits.charAt(10) - '0');
    }

    public static boolean isValidCNPJ(String cnpj) {
        if (cnpj == null) return false;
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * weights1[i];
        }
        int remainder = sum % 11;
        int first = remainder < 2 ? 0 : 11 - remainder;
        if (first != (digits.charAt(12) - '0')) return false;

        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += (digits.charAt(i) - '0') * weights2[i];
        }
        remainder = sum % 11;
        int second = remainder < 2 ? 0 : 11 - remainder;
        return second == (digits.charAt(13) - '0');
    }
}
