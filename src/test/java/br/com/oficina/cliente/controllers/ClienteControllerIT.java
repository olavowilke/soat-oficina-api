package br.com.oficina.cliente.controllers;

import br.com.oficina.cliente.entities.TipoDocumento;
import br.com.oficina.cliente.presenters.ClienteResponse;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClienteControllerIT extends BaseControllerIT {

    private static final String VALID_CPF = "52998224725";
    private static final String BASE_URL = "/clientes";

    private CadastrarClienteRequest buildRequest(String cpf) {
        return new CadastrarClienteRequest("João Silva", "joao@example.com", "11999999999",
                cpf, TipoDocumento.CPF);
    }

    @Test
    void shouldReturn201WhenCreatingClienteWithValidRequest() {
        ResponseEntity<ClienteResponse> response = authPost(BASE_URL, buildRequest(VALID_CPF), ClienteResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("João Silva", response.getBody().nome());
    }

    @Test
    void shouldReturn422WhenCreatingClienteWithMissingNome() {
        CadastrarClienteRequest request = new CadastrarClienteRequest("", "joao@example.com",
                "11999999999", "01234567890", TipoDocumento.CPF);

        ResponseEntity<Map> response = authPost(BASE_URL, request, Map.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    void shouldReturn400WhenCreatingClienteWithInvalidCPF() {
        CadastrarClienteRequest request = new CadastrarClienteRequest("João Silva", "joao@example.com",
                "11999999999", "12345678900", TipoDocumento.CPF);

        ResponseEntity<Map> response = authPost(BASE_URL, request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenListingClientes() {
        ResponseEntity<ClienteResponse[]> response = authGet(BASE_URL, ClienteResponse[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldReturn200WhenGettingClienteByIdAfterCreating() {
        String uniqueCpf = "39053344705";
        CadastrarClienteRequest request = new CadastrarClienteRequest("Maria Souza", "maria@example.com",
                "11888888888", uniqueCpf, TipoDocumento.CPF);

        ResponseEntity<ClienteResponse> created = authPost(BASE_URL, request, ClienteResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        ResponseEntity<ClienteResponse> response = authGet(BASE_URL + "/" + id, ClienteResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().id());
    }

    @Test
    void shouldReturn404WhenGettingUnknownCliente() {
        UUID unknownId = UUID.randomUUID();
        ResponseEntity<Map> response = authGet(BASE_URL + "/" + unknownId, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenUpdatingCliente() {
        String uniqueCpf = "71428793860";
        CadastrarClienteRequest createRequest = new CadastrarClienteRequest("Carlos Lima", "carlos@example.com",
                "11777777777", uniqueCpf, TipoDocumento.CPF);

        ResponseEntity<ClienteResponse> created = authPost(BASE_URL, createRequest, ClienteResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UUID id = created.getBody().id();

        AtualizarClienteRequest updateRequest = new AtualizarClienteRequest("Carlos Lima Atualizado",
                "carlos.novo@example.com", "11666666666");

        ResponseEntity<ClienteResponse> response = authPut(BASE_URL + "/" + id, updateRequest, ClienteResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Carlos Lima Atualizado", response.getBody().nome());
    }

    @Test
    void shouldReturn204WhenDeletingCliente() {
        String uniqueCpf = "87654321007";
        CadastrarClienteRequest createRequest = new CadastrarClienteRequest("Ana Costa", "ana@example.com",
                "11555555555", uniqueCpf, TipoDocumento.CPF);

        ResponseEntity<ClienteResponse> created = authPost(BASE_URL, createRequest, ClienteResponse.class);
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
