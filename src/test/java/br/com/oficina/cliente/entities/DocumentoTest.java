package br.com.oficina.cliente.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoTest {

    @Test
    void shouldAcceptValidCPF() {
        Documento doc = new Documento("52998224725", TipoDocumento.CPF);
        assertNotNull(doc);
        assertEquals("52998224725", doc.numero());
    }

    @Test
    void shouldAcceptValidCNPJ() {
        Documento doc = new Documento("11222333000181", TipoDocumento.CNPJ);
        assertNotNull(doc);
        assertEquals("11222333000181", doc.numero());
    }

    @Test
    void shouldThrowForInvalidCPF() {
        assertThrows(IllegalArgumentException.class,
                () -> new Documento("12345678900", TipoDocumento.CPF));
    }

    @Test
    void shouldThrowForInvalidCNPJ() {
        assertThrows(IllegalArgumentException.class,
                () -> new Documento("12345678000100", TipoDocumento.CNPJ));
    }

    @Test
    void shouldThrowForAllSameDigitsCPF() {
        assertThrows(IllegalArgumentException.class,
                () -> new Documento("11111111111", TipoDocumento.CPF));
    }

    @Test
    void shouldStripFormattingBeforeValidation() {
        Documento doc = new Documento("529.982.247-25", TipoDocumento.CPF);
        assertNotNull(doc);
        assertEquals("52998224725", doc.numero());
    }

    @Test
    void shouldReturnOnlyDigitsFromGetNumero() {
        Documento doc = new Documento("529.982.247-25", TipoDocumento.CPF);
        assertEquals("52998224725", doc.numero());
        assertFalse(doc.numero().contains("."));
        assertFalse(doc.numero().contains("-"));
    }

    @Test
    void shouldReturnCorrectTipo() {
        Documento cpf = new Documento("52998224725", TipoDocumento.CPF);
        assertEquals(TipoDocumento.CPF, cpf.tipo());

        Documento cnpj = new Documento("11222333000181", TipoDocumento.CNPJ);
        assertEquals(TipoDocumento.CNPJ, cnpj.tipo());
    }
}
