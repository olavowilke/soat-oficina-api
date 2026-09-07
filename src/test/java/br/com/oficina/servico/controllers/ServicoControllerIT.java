package br.com.oficina.servico.controllers;

import br.com.oficina.servico.presenters.ServicoResponse;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServicoControllerIT extends BaseControllerIT {

    private static final String BASE_PATH = "/servicos";

    private ServicoResponse cadastrarServico(String nome, BigDecimal preco, int minutos) {
        CadastrarServicoRequest request = new CadastrarServicoRequest(nome, "desc", preco, minutos);
        ResponseEntity<ServicoResponse> response = authPost(BASE_PATH, request, ServicoResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    @Test
    void shouldReturn201WhenCreatingServico() {
        ServicoResponse servico = cadastrarServico("Troca de óleo", new BigDecimal("150.00"), 30);
        assertNotNull(servico.id());
        assertEquals("Troca de óleo", servico.nome());
        assertEquals(new BigDecimal("150.00"), servico.preco());
    }

    @Test
    void shouldReturn422WhenCreatingServicoWithInvalidPrice() {
        CadastrarServicoRequest invalid = new CadastrarServicoRequest(
                "Serviço ruim", "desc", new BigDecimal("0.00"), 30);
        ResponseEntity<Object> response = authPost(BASE_PATH, invalid, Object.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenListingServicos() {
        cadastrarServico("Alinhamento", new BigDecimal("80.00"), 45);
        cadastrarServico("Balanceamento", new BigDecimal("60.00"), 30);

        ResponseEntity<ServicoResponse[]> response = authGet(BASE_PATH, ServicoResponse[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
    }

    @Test
    void listarNaoDeveRetornarServicosDesativados() {
        ServicoResponse ativo = cadastrarServico("Ativo", new BigDecimal("100.00"), 20);
        ServicoResponse aDesativar = cadastrarServico("Desativado", new BigDecimal("50.00"), 15);

        assertEquals(HttpStatus.NO_CONTENT, authDelete(BASE_PATH + "/" + aDesativar.id()).getStatusCode());

        ResponseEntity<ServicoResponse[]> response = authGet(BASE_PATH, ServicoResponse[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);
        assertEquals(ativo.id(), response.getBody()[0].id());
    }

    @Test
    void shouldReturn200WhenGettingServicoById() {
        ServicoResponse created = cadastrarServico("Revisão", new BigDecimal("250.00"), 90);
        ResponseEntity<ServicoResponse> response = authGet(
                BASE_PATH + "/" + created.id(), ServicoResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(created.id(), response.getBody().id());
    }

    @Test
    void shouldReturn404WhenGettingNonExistentServico() {
        ResponseEntity<Object> response = authGet(
                BASE_PATH + "/" + UUID.randomUUID(), Object.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturn400WhenGettingServicoWithMalformedUuid() {
        // Regression: MethodArgumentTypeMismatchException must map to 400, not 500.
        ResponseEntity<Object> response = authGet(BASE_PATH + "/not-a-uuid", Object.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenUpdatingServico() {
        ServicoResponse created = cadastrarServico("Diagnóstico", new BigDecimal("120.00"), 45);
        AtualizarServicoRequest update = new AtualizarServicoRequest(
                "Diagnóstico completo", "Scanner + análise", new BigDecimal("180.00"), 60);

        ResponseEntity<ServicoResponse> response = authPut(
                BASE_PATH + "/" + created.id(), update, ServicoResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Diagnóstico completo", response.getBody().nome());
        assertEquals(new BigDecimal("180.00"), response.getBody().preco());
    }

    @Test
    void shouldReturn204WhenDeletingServico() {
        ServicoResponse created = cadastrarServico("Lavagem", new BigDecimal("40.00"), 20);
        ResponseEntity<Void> response = authDelete(BASE_PATH + "/" + created.id());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldReturn401WhenAccessingWithoutToken() {
        ResponseEntity<Object> response = restTemplate.getForEntity(BASE_PATH, Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
