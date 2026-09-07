package br.com.oficina.veiculo.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacaTest {

    @Test
    void shouldAcceptValidOldFormat() {
        Placa placa = new Placa("ABC1234");
        assertNotNull(placa);
        assertEquals("ABC1234", placa.valor());
    }

    @Test
    void shouldAcceptOldFormatWithHyphenAndNormalize() {
        Placa placa = new Placa("ABC-1234");
        assertNotNull(placa);
        assertEquals("ABC1234", placa.valor());
    }

    @Test
    void shouldAcceptValidMercosulFormat() {
        Placa placa = new Placa("ABC1D23");
        assertNotNull(placa);
        assertEquals("ABC1D23", placa.valor());
    }

    @Test
    void shouldThrowForInvalidPlate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Placa("INVALID1"));
    }

    @Test
    void shouldNormalizeLowercaseToUppercase() {
        Placa placa = new Placa("abc1234");
        assertEquals("ABC1234", placa.valor());
    }

    @Test
    void shouldReturnNormalizedValue() {
        Placa placa = new Placa("abc-1234");
        assertEquals("ABC1234", placa.valor());
    }
}
