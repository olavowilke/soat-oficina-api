package br.com.oficina.shared;

import br.com.oficina.auth.controllers.LoginRequest;
import br.com.oficina.auth.presenters.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseControllerIT {

    // Singleton container: started once for the JVM and reused across all IT classes,
    // so Spring's cached ApplicationContext keeps pointing at a live database.
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("oficina_test")
            .withUsername("oficina")
            .withPassword("oficina");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Singleton container is reused across all IT classes, so we must reset
        // state between tests to avoid unique-constraint collisions and stale rows.
        jdbcTemplate.execute("TRUNCATE TABLE itens_peca, itens_servico, ordens_servico, " +
                "veiculos, clientes, pecas, servicos RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("DELETE FROM usuarios WHERE username <> 'admin'");
    }

    protected String getAdminToken() {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/auth/login", loginRequest, AuthResponse.class);
        AuthResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException(
                    "Admin login falhou — status " + response.getStatusCode() +
                    ". Verifique se AdminInitializer rodou.");
        }
        return body.token();
    }

    protected HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        return headers;
    }

    protected <T> ResponseEntity<T> authGet(String url, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), responseType);
    }

    protected <T> ResponseEntity<T> authPost(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    protected <T> ResponseEntity<T> authPut(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), responseType);
    }

    protected <T> ResponseEntity<T> authPatch(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), responseType);
    }

    protected ResponseEntity<Void> authDelete(String url) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Void.class);
    }
}
