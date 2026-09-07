package br.com.oficina.peca.controllers;

import br.com.oficina.peca.presenters.PecaResponse;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PecaControllerIT extends BaseControllerIT {

    private static final String BASE_URL = "/pecas";

    private CadastrarPecaRequest buildRequest() {
        return new CadastrarPecaRequest("Filtro de Óleo", "Filtro para motor",
                new BigDecimal("25.90"), 10);
    }

    @Test
    void shouldReturn201WhenCreatingPecaWithValidRequest() {
        ResponseEntity<PecaResponse> response = authPost(BASE_URL, buildRequest(), PecaResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Filtro de Óleo", response.getBody().nome());
    }

    @Test
    void shouldReturn422WhenCreatingPecaWithBlankNome() {
        CadastrarPecaRequest request = new CadastrarPecaRequest("", "desc", new BigDecimal("25.90"), 10);

        ResponseEntity<Map> response = authPost(BASE_URL, request, Map.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenListingPecas() {
        ResponseEntity<PecaResponse[]> response = authGet(BASE_URL, PecaResponse[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldReturn200WhenGettingPecaByIdAfterCreating() {
        CadastrarPecaRequest request = new CadastrarPecaRequest("Vela de Ignição", "Vela NGK",
                new BigDecimal("15.50"), 20);

        ResponseEntity<PecaResponse> created = authPost(BASE_URL, request, PecaResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        ResponseEntity<PecaResponse> response = authGet(BASE_URL + "/" + id, PecaResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().id());
    }

    @Test
    void shouldReturn200WhenAdjustingStockWithPositiveQuantity() {
        CadastrarPecaRequest request = new CadastrarPecaRequest("Correia Dentada", "Correia para motor 1.0",
                new BigDecimal("89.90"), 5);

        ResponseEntity<PecaResponse> created = authPost(BASE_URL, request, PecaResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        AjustarEstoqueRequest estoqueRequest = new AjustarEstoqueRequest(10);
        ResponseEntity<PecaResponse> response = authPatch(BASE_URL + "/" + id + "/estoque",
                estoqueRequest, PecaResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(15, response.getBody().quantidadeEstoque());
    }

    @Test
    void shouldReturn400WhenAdjustingStockThatWouldGoNegative() {
        CadastrarPecaRequest request = new CadastrarPecaRequest("Amortecedor Dianteiro",
                "Amortecedor para eixo dianteiro", new BigDecimal("299.90"), 2);

        ResponseEntity<PecaResponse> created = authPost(BASE_URL, request, PecaResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        AjustarEstoqueRequest estoqueRequest = new AjustarEstoqueRequest(-10);
        ResponseEntity<Map> response = authPatch(BASE_URL + "/" + id + "/estoque", estoqueRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn204WhenDeletingPeca() {
        CadastrarPecaRequest request = new CadastrarPecaRequest("Pastilha de Freio", "Pastilha traseira",
                new BigDecimal("45.00"), 8);

        ResponseEntity<PecaResponse> created = authPost(BASE_URL, request, PecaResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        ResponseEntity<Void> response = authDelete(BASE_URL + "/" + id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldReturn401WhenAccessingWithoutToken() {
        ResponseEntity<Object> response = restTemplate.getForEntity(BASE_URL, Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
