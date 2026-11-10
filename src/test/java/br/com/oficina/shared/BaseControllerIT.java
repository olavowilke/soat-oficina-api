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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseControllerIT {

    // Singleton container: started once for the JVM and reused across all IT classes,
    // so Spring's cached ApplicationContext keeps pointing at a live database.
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("oficina_test")
            .withUsername("oficina")
            .withPassword("oficina");

    // LocalStack (SQS) compartilhado, como o Postgres: o OutboxRelay e os ITs de mensageria usam estas filas.
    static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
                    .withServices(LocalStackContainer.Service.SQS);

    static {
        postgres.start();
        localstack.start();
        for (String fila : new String[]{"oficina-homolog-os-saga-replies",
                "oficina-homolog-billing-commands", "oficina-homolog-execution-commands"}) {
            try {
                var r = localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", fila);
                if (r.getExitCode() != 0) {
                    throw new IllegalStateException("Falha ao criar fila " + fila + ": " + r.getStderr());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao criar fila " + fila, e);
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cloud.aws.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        // O relay roda só quando o teste o chama (primeira execução atrasada em 24 h).
        registry.add("oficina.outbox.intervalo", () -> "PT24H");
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
                "veiculos, clientes, pecas, servicos, saga_ordem_servico, outbox, mensagens_processadas " +
                "RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("DELETE FROM usuarios WHERE username <> 'admin'");
    }

    /** Insere um cliente e um veículo direto no banco; devolve {clienteId, veiculoId}. */
    protected UUID[] inserirClienteEVeiculo() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        String sufixo = clienteId.toString().replace("-", "");
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO clientes (id, nome, documento, tipo_documento, ativo, criado_em, atualizado_em) "
                + "VALUES (?, 'Cliente Teste', ?, 'CPF', true, ?, ?)", clienteId, sufixo.substring(0, 11), agora, agora);
        jdbcTemplate.update("INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, criado_em, atualizado_em) "
                + "VALUES (?, ?, ?, 'Marca', 'Modelo', 2020, ?, ?)", veiculoId, clienteId,
                sufixo.substring(0, 7).toUpperCase(), agora, agora);
        return new UUID[]{clienteId, veiculoId};
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
