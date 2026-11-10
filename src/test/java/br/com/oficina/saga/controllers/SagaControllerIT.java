package br.com.oficina.saga.controllers;

import br.com.oficina.auth.infrastructure.JwtService;
import br.com.oficina.cliente.controllers.CadastrarClienteRequest;
import br.com.oficina.cliente.entities.TipoDocumento;
import br.com.oficina.cliente.presenters.ClienteResponse;
import br.com.oficina.ordemservico.controllers.AdicionarServicoRequest;
import br.com.oficina.ordemservico.controllers.AvancarStatusRequest;
import br.com.oficina.ordemservico.controllers.CriarOrdemServicoRequest;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.presenters.OrdemServicoResponse;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.presenters.SagaResponse;
import br.com.oficina.servico.controllers.CadastrarServicoRequest;
import br.com.oficina.servico.presenters.ServicoResponse;
import br.com.oficina.shared.BaseControllerIT;
import br.com.oficina.veiculo.controllers.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.presenters.VeiculoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class SagaControllerIT extends BaseControllerIT {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtService jwtService;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        clienteId = authPost("/clientes", new CadastrarClienteRequest(
                "João Silva", "joao@email.com", "11999999999", "529.982.247-25", TipoDocumento.CPF),
                ClienteResponse.class).getBody().id();
        veiculoId = authPost("/veiculos", new CadastrarVeiculoRequest(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata"), VeiculoResponse.class).getBody().id();
        servicoId = authPost("/servicos", new CadastrarServicoRequest(
                "Troca de óleo", "Óleo 5W30", new BigDecimal("150.00"), 30), ServicoResponse.class).getBody().id();
    }

    private UUID osEmDiagnostico(boolean comItem) {
        UUID id = authPost("/ordens-servico", new CriarOrdemServicoRequest(clienteId, veiculoId, "Revisão"),
                OrdemServicoResponse.class).getBody().id();
        if (comItem) {
            authPost("/ordens-servico/" + id + "/servicos", new AdicionarServicoRequest(servicoId),
                    OrdemServicoResponse.class);
        }
        authPatch("/ordens-servico/" + id + "/status", new AvancarStatusRequest(StatusOS.EM_DIAGNOSTICO),
                Object.class);
        return id;
    }

    private ResponseEntity<SagaResponse> enviar(UUID osId) {
        return authPost("/ordens-servico/" + osId + "/enviar-orcamento", null, SagaResponse.class);
    }

    @Test
    void enviaOrcamentoCriaSagaEComando() {
        UUID osId = osEmDiagnostico(true);

        ResponseEntity<SagaResponse> response = enviar(osId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().estado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        assertThat(authGet("/ordens-servico/" + osId, OrdemServicoResponse.class).getBody().status())
                .isEqualTo(StatusOS.AGUARDANDO_ORCAMENTO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE tipo = 'GerarOrcamento'", Integer.class))
                .isEqualTo(1);
        ResponseEntity<SagaResponse[]> sagas = authGet("/ordens-servico/" + osId + "/saga", SagaResponse[].class);
        assertThat(sagas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sagas.getBody()).extracting(SagaResponse::id).containsExactly(response.getBody().id());
    }

    @Test
    void enviarDuasVezesRetorna409() {
        UUID osId = osEmDiagnostico(true);
        enviar(osId);

        assertThat(enviar(osId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saga_ordem_servico", Integer.class)).isEqualTo(1);
    }

    @Test
    void doisEnvioSimultaneosGeramUmaSagaEUm409() throws Exception {
        UUID osId = osEmDiagnostico(true);
        HttpHeaders headers = authHeaders();
        CountDownLatch largada = new CountDownLatch(1);
        Callable<HttpStatus> envio = () -> {
            largada.await();
            return HttpStatus.valueOf(restTemplate.exchange("/ordens-servico/" + osId + "/enviar-orcamento",
                    HttpMethod.POST, new HttpEntity<>(headers), Object.class).getStatusCode().value());
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<HttpStatus>> futuros = new ArrayList<>();
            futuros.add(pool.submit(envio));
            futuros.add(pool.submit(envio));
            largada.countDown();
            List<HttpStatus> status = new ArrayList<>();
            for (Future<HttpStatus> f : futuros) status.add(f.get());
            assertThat(status).containsExactlyInAnyOrder(HttpStatus.ACCEPTED, HttpStatus.CONFLICT);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saga_ordem_servico WHERE os_id = ?", Integer.class, osId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE os_id = ? AND tipo = 'GerarOrcamento'",
                Integer.class, osId)).isEqualTo(1);
    }

    @Test
    void enviarSemItensRetorna409() {
        UUID osId = osEmDiagnostico(false);

        assertThat(enviar(osId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saga_ordem_servico", Integer.class)).isZero();
    }

    @Test
    void osInexistenteRetorna404() {
        assertThat(enviar(UUID.randomUUID()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(authGet("/ordens-servico/" + UUID.randomUUID() + "/saga", Object.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clienteNaoPodeEnviar() {
        UUID osId = osEmDiagnostico(true);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken("52998224725", "CLIENTE"));

        ResponseEntity<Object> post = restTemplate.exchange("/ordens-servico/" + osId + "/enviar-orcamento",
                HttpMethod.POST, new HttpEntity<>(headers), Object.class);
        ResponseEntity<Object> get = restTemplate.exchange("/ordens-servico/" + osId + "/saga",
                HttpMethod.GET, new HttpEntity<>(headers), Object.class);

        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
