package br.com.oficina.peca.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PecaTest {

    @Test
    void novoShouldCreatePecaWithCorrectValues() {
        Peca peca = Peca.novo("Filtro de Óleo", "Filtro para motor", new BigDecimal("25.90"), 10);

        assertNotNull(peca.getId());
        assertEquals("Filtro de Óleo", peca.getNome());
        assertEquals("Filtro para motor", peca.getDescricao());
        assertEquals(new BigDecimal("25.90"), peca.getPrecoUnitario());
        assertEquals(10, peca.getQuantidadeEstoque());
        assertTrue(peca.isAtivo());
        assertNotNull(peca.getCriadoEm());
        assertNotNull(peca.getAtualizadoEm());
    }

    @Test
    void novoWithBlankNomeShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Peca.novo("", "desc", new BigDecimal("10.00"), 5));
    }

    @Test
    void novoWithNullNomeShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Peca.novo(null, "desc", new BigDecimal("10.00"), 5));
    }

    @Test
    void novoWithZeroPrecoDeveriaLancarIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Peca.novo("Filtro", "desc", BigDecimal.ZERO, 5));
    }

    @Test
    void novoWithNegativePrecoDeveriaLancarIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Peca.novo("Filtro", "desc", new BigDecimal("-1.00"), 5));
    }

    @Test
    void novoWithNegativeEstoqueInicialShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Peca.novo("Filtro", "desc", new BigDecimal("10.00"), -1));
    }

    @Test
    void ajustarEstoqueWithPositiveQuantityShouldIncreaseStock() {
        Peca peca = Peca.novo("Filtro", "desc", new BigDecimal("10.00"), 5);

        peca.ajustarEstoque(10);

        assertEquals(15, peca.getQuantidadeEstoque());
    }

    @Test
    void ajustarEstoqueWithNegativeQuantityShouldDecreaseStock() {
        Peca peca = Peca.novo("Filtro", "desc", new BigDecimal("10.00"), 10);

        peca.ajustarEstoque(-3);

        assertEquals(7, peca.getQuantidadeEstoque());
    }

    @Test
    void ajustarEstoqueThatWouldMakeStockNegativeShouldThrowIllegalArgumentException() {
        Peca peca = Peca.novo("Filtro", "desc", new BigDecimal("10.00"), 5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                peca.ajustarEstoque(-10));

        assertTrue(ex.getMessage().contains("Estoque insuficiente"));
    }

    @Test
    void ajustarEstoqueZeroingStockExactlyShouldBeAllowed() {
        Peca peca = Peca.novo("Filtro", "desc", new BigDecimal("10.00"), 5);

        peca.ajustarEstoque(-5);

        assertEquals(0, peca.getQuantidadeEstoque());
    }

    @Test
    void desativarShouldSetAtivoFalse() {
        Peca peca = Peca.novo("Filtro", "desc", new BigDecimal("10.00"), 5);

        peca.desativar();

        assertFalse(peca.isAtivo());
    }
}
