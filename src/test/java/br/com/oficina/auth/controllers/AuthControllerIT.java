package br.com.oficina.auth.controllers;

import br.com.oficina.auth.entities.Role;
import br.com.oficina.auth.presenters.AuthResponse;
import br.com.oficina.auth.presenters.UsuarioResponse;
import br.com.oficina.shared.BaseControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerIT extends BaseControllerIT {

    @Test
    void shouldReturn201WhenRegisteringNewUser() {
        RegisterRequest request = new RegisterRequest("mecanico1", "senha123");
        ResponseEntity<UsuarioResponse> response = restTemplate.postForEntity(
                "/auth/register", request, UsuarioResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mecanico1", response.getBody().username());
        assertEquals(Role.MECANICO, response.getBody().role());
    }

    @Test
    void publicRegisterShouldAlwaysCreateMecanicoEvenIfClientAttemptsAdmin() {
        // Regression: public /auth/register previously accepted a role field and
        // allowed unauthenticated ADMIN creation. The field was removed, so any
        // extra fields are ignored and the created user must always be MECANICO.
        var body = new java.util.HashMap<String, Object>();
        body.put("username", "mecanico_admin_attempt");
        body.put("password", "senha123");
        body.put("role", "ADMIN"); // ignored server-side

        ResponseEntity<UsuarioResponse> response = restTemplate.postForEntity(
                "/auth/register", body, UsuarioResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.MECANICO, response.getBody().role());
    }

    @Test
    void shouldReturn200AndTokenWhenLoginWithAdminCredentials() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/auth/login", request, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().token());
        assertFalse(response.getBody().token().isBlank());
        assertEquals("ADMIN", response.getBody().role());
    }

    @Test
    void shouldReturn401WhenLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest("admin", "senhaerrada");
        ResponseEntity<Object> response = restTemplate.postForEntity(
                "/auth/login", request, Object.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn400WhenRegisteringDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("admin", "qualquersenha");
        ResponseEntity<Object> response = restTemplate.postForEntity(
                "/auth/register", request, Object.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn401WhenAccessingProtectedRouteWithoutToken() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/clientes", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenAccessingProtectedRouteWithValidToken() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/clientes", HttpMethod.GET, entity, Object.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenAccessingPublicStatusEndpointWithoutToken() {
        HttpHeaders headers = authHeaders();

        var clienteBody = new java.util.HashMap<String, Object>();
        clienteBody.put("nome", "João");
        clienteBody.put("email", "joao@test.com");
        clienteBody.put("telefone", "11999999999");
        clienteBody.put("documento", "529.982.247-25");
        clienteBody.put("tipoDocumento", "CPF");
        ResponseEntity<java.util.Map> clienteRes = restTemplate.exchange(
                "/clientes", HttpMethod.POST,
                new HttpEntity<>(clienteBody, headers), java.util.Map.class);
        assertNotNull(clienteRes.getBody(), "Criação de cliente falhou");
        java.util.UUID clienteId = java.util.UUID.fromString((String) clienteRes.getBody().get("id"));

        var veiculoBody = new java.util.HashMap<String, Object>();
        veiculoBody.put("clienteId", clienteId.toString());
        veiculoBody.put("placa", "XYZ-9999");
        veiculoBody.put("marca", "Honda");
        veiculoBody.put("modelo", "Civic");
        veiculoBody.put("ano", 2022);
        veiculoBody.put("cor", "Preto");
        ResponseEntity<java.util.Map> veiculoRes = restTemplate.exchange(
                "/veiculos", HttpMethod.POST,
                new HttpEntity<>(veiculoBody, headers), java.util.Map.class);
        assertNotNull(veiculoRes.getBody(), "Criação de veículo falhou");
        java.util.UUID veiculoId = java.util.UUID.fromString((String) veiculoRes.getBody().get("id"));

        var osBody = new java.util.HashMap<String, Object>();
        osBody.put("clienteId", clienteId.toString());
        osBody.put("veiculoId", veiculoId.toString());
        ResponseEntity<java.util.Map> osRes = restTemplate.exchange(
                "/ordens-servico", HttpMethod.POST,
                new HttpEntity<>(osBody, headers), java.util.Map.class);
        assertNotNull(osRes.getBody(), "Criação de OS falhou");
        String osId = (String) osRes.getBody().get("id");

        // Acessa o status público SEM token
        ResponseEntity<java.util.Map> publicResponse = restTemplate.getForEntity(
                "/public/ordens-servico/" + osId + "/status", java.util.Map.class);
        assertEquals(HttpStatus.OK, publicResponse.getStatusCode());
        assertNotNull(publicResponse.getBody());
        // Reduced response exposes only id + status, never line items
        assertEquals(osId, publicResponse.getBody().get("id"));
        assertNotNull(publicResponse.getBody().get("status"));
        assertNull(publicResponse.getBody().get("itensServico"),
                "Endpoint público não pode vazar itens de serviço");
        assertNull(publicResponse.getBody().get("itensPeca"),
                "Endpoint público não pode vazar itens de peça");
    }
}
