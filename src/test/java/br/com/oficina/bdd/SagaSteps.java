package br.com.oficina.bdd;

import br.com.oficina.auth.controllers.LoginRequest;
import br.com.oficina.auth.presenters.AuthResponse;
import br.com.oficina.cliente.controllers.CadastrarClienteRequest;
import br.com.oficina.cliente.entities.TipoDocumento;
import br.com.oficina.cliente.presenters.ClienteResponse;
import br.com.oficina.ordemservico.controllers.AdicionarPecaRequest;
import br.com.oficina.ordemservico.controllers.AdicionarServicoRequest;
import br.com.oficina.ordemservico.controllers.AvancarStatusRequest;
import br.com.oficina.ordemservico.controllers.CriarOrdemServicoRequest;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.presenters.OrdemServicoResponse;
import br.com.oficina.peca.controllers.CadastrarPecaRequest;
import br.com.oficina.peca.presenters.PecaResponse;
import br.com.oficina.saga.presenters.SagaResponse;
import br.com.oficina.servico.controllers.CadastrarServicoRequest;
import br.com.oficina.servico.presenters.ServicoResponse;
import br.com.oficina.shared.infrastructure.mensageria.MensagemSaga;
import br.com.oficina.shared.infrastructure.mensageria.OutboxRelay;
import br.com.oficina.veiculo.controllers.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.presenters.VeiculoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Passos do BDD da saga. O Billing e a Execução são dublês (veja {@link FilasDeTeste}).
 *
 * <p>Regra de espera: o orquestrador ignora (com WARN) uma resposta fora de ordem. Por isso cada passo
 * "Quando o Billing/a Execução responde X" primeiro espera, por até 10 s, a saga chegar ao estado que X
 * pede, e só então envia. Os passos "a OS fica em" e "a saga fica em" também esperam até 10 s.
 */
public class SagaSteps {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    /** Estado da saga que cada resposta espera, como o ProcessarRespostaSagaUseCase define. */
    private static final Map<String, String> ESTADO_ESPERADO = Map.ofEntries(
            Map.entry("OrcamentoGerado", "AGUARDANDO_ORCAMENTO"),
            Map.entry("OrcamentoAprovado", "AGUARDANDO_APROVACAO"),
            Map.entry("OrcamentoRecusado", "AGUARDANDO_APROVACAO"),
            Map.entry("PagamentoConfirmado", "AGUARDANDO_PAGAMENTO"),
            Map.entry("PagamentoFalhou", "AGUARDANDO_PAGAMENTO"),
            Map.entry("ExecucaoAceita", "AGUARDANDO_EXECUCAO"),
            Map.entry("ExecucaoRejeitada", "AGUARDANDO_EXECUCAO"),
            Map.entry("ExecucaoFinalizada", "EM_EXECUCAO"),
            Map.entry("PagamentoEstornado", "ESTORNANDO"));

    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SqsAsyncClient sqs;
    @Autowired private OutboxRelay relay;
    @Value("${oficina.filas.respostas}") private String filaRespostas;
    @Value("${oficina.filas.billing-comandos}") private String filaBilling;
    @Value("${oficina.filas.execucao-comandos}") private String filaExecucao;

    private FilasDeTeste filas;
    private String token;
    private UUID pecaId;
    private UUID osId;
    private UUID sagaId;

    @Before
    public void limparCenario() {
        jdbc.execute("TRUNCATE TABLE itens_peca, itens_servico, ordens_servico, "
                + "veiculos, clientes, pecas, servicos, saga_ordem_servico, outbox, mensagens_processadas "
                + "RESTART IDENTITY CASCADE");
        jdbc.execute("DELETE FROM usuarios WHERE username <> 'admin'");
        filas = new FilasDeTeste(sqs, mapper, filaRespostas, relay::enviarPendentes);
        filas.esvaziar(filaBilling, filaExecucao, filaRespostas);
        token = null;
        osId = null;
        sagaId = null;
    }

    // ---------- Dado ----------

    @Dado("uma peça {string} com {int} unidades em estoque")
    public void umaPeca(String nome, int estoque) {
        pecaId = post("/pecas", new CadastrarPecaRequest(nome, nome, new BigDecimal("25.00"), estoque),
                PecaResponse.class).id();
    }

    @E("uma OS em diagnóstico com o serviço {string} e {int} unidades de {string}")
    public void umaOsEmDiagnostico(String servico, int unidades, String peca) {
        UUID clienteId = post("/clientes", new CadastrarClienteRequest(
                "João Silva", "joao@email.com", "11999999999", "529.982.247-25", TipoDocumento.CPF),
                ClienteResponse.class).id();
        UUID veiculoId = post("/veiculos", new CadastrarVeiculoRequest(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata"), VeiculoResponse.class).id();
        UUID servicoId = post("/servicos", new CadastrarServicoRequest(
                servico, "Serviço de teste", new BigDecimal("150.00"), 30), ServicoResponse.class).id();
        osId = post("/ordens-servico", new CriarOrdemServicoRequest(clienteId, veiculoId, "Revisão"),
                OrdemServicoResponse.class).id();
        post("/ordens-servico/" + osId + "/servicos", new AdicionarServicoRequest(servicoId),
                OrdemServicoResponse.class);
        // Adicionar a peça reserva o estoque.
        post("/ordens-servico/" + osId + "/pecas", new AdicionarPecaRequest(pecaId, unidades),
                OrdemServicoResponse.class);
        var status = exchange(HttpMethod.PATCH, "/ordens-servico/" + osId + "/status",
                new AvancarStatusRequest(StatusOS.EM_DIAGNOSTICO), Object.class);
        assertThat(status.getStatusCode().is2xxSuccessful()).isTrue();
        filas.filtrarPor(osId);
    }

    // ---------- Quando ----------

    @Quando("o mecânico envia o orçamento")
    public void mecanicoEnviaOrcamento() {
        var response = exchange(HttpMethod.POST, "/ordens-servico/" + osId + "/enviar-orcamento", null,
                SagaResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        sagaId = response.getBody().id();
    }

    @Quando("o Billing responde {string}")
    public void billingResponde(String tipo) {
        responder(tipo, "");
    }

    @Quando("o Billing responde {string} com motivo {string}")
    public void billingRespondeComMotivo(String tipo, String motivo) {
        responder(tipo, motivo);
    }

    @Quando("a Execução responde {string}")
    public void execucaoResponde(String tipo) {
        responder(tipo, "CAPACIDADE");
    }

    // ---------- Então ----------

    @Então("o Billing recebe o comando {string} com {int} itens")
    public void billingRecebeComItens(String tipo, int itens) {
        MensagemSaga comando = filas.receberComando(filaBilling, tipo, ESPERA);
        assertThat(comando.payload().get("itens")).hasSize(itens);
    }

    @Então("o Billing recebe o comando {string}")
    public void billingRecebe(String tipo) {
        filas.receberComando(filaBilling, tipo, ESPERA);
    }

    @Então("a Execução recebe o comando {string}")
    public void execucaoRecebe(String tipo) {
        filas.receberComando(filaExecucao, tipo, ESPERA);
    }

    @Então("a OS fica em {string}")
    public void osFicaEm(String status) {
        await().atMost(ESPERA).untilAsserted(() -> assertThat(
                jdbc.queryForObject("SELECT status FROM ordens_servico WHERE id = ?", String.class, osId))
                .isEqualTo(status));
    }

    @E("a saga fica em {string}")
    public void sagaFicaEm(String estado) {
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoDaSaga()).isEqualTo(estado));
    }

    @E("o estoque de {string} é {int}")
    public void estoqueE(String peca, int esperado) {
        Integer atual = jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE nome = ?",
                Integer.class, peca);
        assertThat(atual).isEqualTo(esperado);
    }

    // ---------- Apoio ----------

    private void responder(String tipo, String motivo) {
        String esperado = ESTADO_ESPERADO.get(tipo);
        assertThat(esperado).as("tipo de resposta conhecido: " + tipo).isNotNull();
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoDaSaga()).isEqualTo(esperado));
        // Da resposta, só o sagaId, o osId e o correlationId do comando importam.
        MensagemSaga comando = new MensagemSaga(UUID.randomUUID(), sagaId, osId, "comando", "bdd-" + sagaId,
                Instant.now(), null);
        filas.responder(comando, tipo, payloadDe(tipo, motivo));
    }

    private Map<String, Object> payloadDe(String tipo, String motivo) {
        UUID orcamentoId = UUID.nameUUIDFromBytes(("orc-" + sagaId).getBytes());
        UUID pagamentoId = UUID.nameUUIDFromBytes(("pag-" + sagaId).getBytes());
        UUID execucaoId = UUID.nameUUIDFromBytes(("exe-" + sagaId).getBytes());
        Map<String, Object> p = new LinkedHashMap<>();
        switch (tipo) {
            case "OrcamentoGerado" -> {
                p.put("orcamentoId", orcamentoId);
                p.put("valorTotal", "0.00");
            }
            case "OrcamentoAprovado" -> {
                p.put("orcamentoId", orcamentoId);
                p.put("pagamentoId", pagamentoId);
                p.put("linkPagamento", "https://pagamento.exemplo/" + pagamentoId);
            }
            case "PagamentoConfirmado" -> {
                p.put("orcamentoId", orcamentoId);
                p.put("pagamentoId", pagamentoId);
            }
            case "PagamentoFalhou" -> {
                p.put("orcamentoId", orcamentoId);
                p.put("pagamentoId", pagamentoId);
                p.put("motivo", motivo);
            }
            case "ExecucaoAceita" -> p.put("execucaoId", execucaoId);
            case "ExecucaoRejeitada" -> p.put("motivo", motivo);
            case "ExecucaoFinalizada" -> {
                p.put("execucaoId", execucaoId);
                p.put("finalizadaEm", Instant.now().toString());
            }
            case "PagamentoEstornado" -> p.put("pagamentoId", pagamentoId);
            default -> { }
        }
        return p;
    }

    private String estadoDaSaga() {
        return jdbc.queryForList("SELECT estado FROM saga_ordem_servico WHERE id = ?", String.class, sagaId)
                .stream().findFirst().orElse("(sem saga)");
    }

    private String token() {
        if (token == null) {
            token = rest.postForEntity("/auth/login", new LoginRequest("admin", "admin123"), AuthResponse.class)
                    .getBody().token();
        }
        return token;
    }

    private <T> T post(String url, Object body, Class<T> tipo) {
        var response = exchange(HttpMethod.POST, url, body, tipo);
        assertThat(response.getStatusCode().is2xxSuccessful()).as("POST " + url).isTrue();
        return response.getBody();
    }

    private <T> ResponseEntity<T> exchange(HttpMethod metodo, String url, Object body, Class<T> tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, metodo, new HttpEntity<>(body, headers), tipo);
    }
}
