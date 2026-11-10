package br.com.oficina.shared.infrastructure.mensageria;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.BaseControllerIT;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class RespostasListenerIT extends BaseControllerIT {

    @Autowired private SqsTemplate sqsTemplate;
    @Autowired private SqsAsyncClient sqs;
    @Autowired private SagaGateway sagaGateway;
    @Autowired private OrdemServicoGateway ordemServicoGateway;
    @Autowired private PecaGateway pecaGateway;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private RespostasListener listener;
    @Value("${oficina.filas.respostas}") private String filaRespostas;

    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void esvaziarFila() throws Exception {
        jdbc.execute("ALTER TABLE saga_ordem_servico DROP CONSTRAINT IF EXISTS ck_teste_rollback");
        String url = sqs.getQueueUrl(r -> r.queueName(filaRespostas)).get().queueUrl();
        sqs.purgeQueue(r -> r.queueUrl(url)).get();
        logger = (Logger) LoggerFactory.getLogger(RespostasListener.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void depois() {
        logger.detachAppender(logs);
    }

    private record Cenario(Peca peca, OrdemServico os, SagaOrdemServico saga) {}

    /** OS em AGUARDANDO_APROVACAO com 2 unidades de uma peça (estoque já em 8). */
    private Cenario cenarioAguardandoAprovacao() {
        UUID[] ids = inserirClienteEVeiculo();
        Peca peca = pecaGateway.save(Peca.novo("Filtro", "Filtro de óleo", new BigDecimal("25.00"), 8));
        OrdemServico os = OrdemServico.reconstituir(UUID.randomUUID(), ids[0], ids[1], StatusOS.AGUARDANDO_APROVACAO,
                null, List.of(), List.of(ItemPeca.novo(peca.getId(), "Filtro", 2, new BigDecimal("25.00"))),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null, null);
        os = ordemServicoGateway.save(os);
        SagaOrdemServico saga = SagaOrdemServico.reconstituir(UUID.randomUUID(), os.getId(),
                EstadoSaga.AGUARDANDO_APROVACAO, UUID.randomUUID(), null, null, null, null, null,
                Instant.now(), Instant.now());
        saga = sagaGateway.salvar(saga);
        return new Cenario(peca, os, saga);
    }

    private String envelope(UUID messageId, UUID sagaId, UUID osId, String tipo, Map<String, Object> payload)
            throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("messageId", messageId.toString());
        n.put("sagaId", sagaId.toString());
        n.put("osId", osId.toString());
        n.put("type", tipo);
        n.put("correlationId", "corr-it");
        n.put("occurredAt", "2026-11-09T12:00:00Z");
        n.set("payload", mapper.valueToTree(payload));
        return n.toString();
    }

    private void enviar(String corpo, String tipo) {
        sqsTemplate.send(to -> to.queue(filaRespostas).payload(corpo).header("type", tipo));
    }

    private int estoque(UUID pecaId) {
        return jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE id = ?", Integer.class, pecaId);
    }

    private String estadoSaga(UUID sagaId) {
        return jdbc.queryForObject("SELECT estado FROM saga_ordem_servico WHERE id = ?", String.class, sagaId);
    }

    private int processadas() {
        return jdbc.queryForObject("SELECT count(*) FROM mensagens_processadas", Integer.class);
    }

    @Test
    void respostaDuplicadaDevolveEstoqueUmaVez() throws Exception {
        Cenario c = cenarioAguardandoAprovacao();
        UUID messageId = UUID.randomUUID();
        String corpo = envelope(messageId, c.saga().getId(), c.os().getId(), "OrcamentoRecusado",
                Map.of("orcamentoId", UUID.randomUUID().toString()));

        enviar(corpo, "OrcamentoRecusado");
        enviar(corpo, "OrcamentoRecusado");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(
                () -> assertThat(estadoSaga(c.saga().getId())).isEqualTo("COMPENSADA"));
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(estoque(c.peca().getId())).isEqualTo(10);
            assertThat(processadas()).isEqualTo(1);
        });
        assertThat(jdbc.queryForObject("SELECT status FROM ordens_servico WHERE id = ?", String.class,
                c.os().getId())).isEqualTo("CANCELADA");
    }

    @Test
    void sagaDesconhecidaEhDescartada() throws Exception {
        UUID messageId = UUID.randomUUID();
        enviar(envelope(messageId, UUID.randomUUID(), UUID.randomUUID(), "OrcamentoGerado",
                Map.of("orcamentoId", UUID.randomUUID().toString())), "OrcamentoGerado");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(processadas()).isEqualTo(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saga_ordem_servico", Integer.class)).isZero();
    }

    /**
     * Uma restrição temporária faz a gravação da saga falhar DEPOIS do insert em mensagens_processadas.
     * A transação desfaz tudo; a fila reentrega e a segunda tentativa, sem a restrição, move a saga uma vez.
     * (Uma restrição no banco vale para qualquer contexto Spring que consuma a fila; um spy valeria só para um.)
     */
    @Test
    void falhaDepoisDoRegistroDesfazORegistroEARedeliveryMoveASagaUmaVez() throws Exception {
        Cenario c = cenarioAguardandoAprovacao();
        jdbc.execute("ALTER TABLE saga_ordem_servico ADD CONSTRAINT ck_teste_rollback "
                + "CHECK (estado <> 'COMPENSADA') NOT VALID");
        try {
            enviar(envelope(UUID.randomUUID(), c.saga().getId(), c.os().getId(), "OrcamentoRecusado",
                    Map.of("orcamentoId", UUID.randomUUID().toString())), "OrcamentoRecusado");

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(logs.list)
                    .anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.ERROR)));
            // A transação desfez o registro, a baixa da saga e a devolução de estoque.
            assertThat(processadas()).isZero();
            assertThat(estadoSaga(c.saga().getId())).isEqualTo("AGUARDANDO_APROVACAO");
            assertThat(estoque(c.peca().getId())).isEqualTo(8);
        } finally {
            jdbc.execute("ALTER TABLE saga_ordem_servico DROP CONSTRAINT IF EXISTS ck_teste_rollback");
        }
        // A fila reentrega depois do tempo de visibilidade e a segunda tentativa funciona.
        await().atMost(Duration.ofSeconds(90)).untilAsserted(
                () -> assertThat(estadoSaga(c.saga().getId())).isEqualTo("COMPENSADA"));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(estoque(c.peca().getId())).isEqualTo(10);
            assertThat(processadas()).isEqualTo(1);
        });
    }

    @Test
    void payloadInvalidoEmTipoConhecidoEhConfirmadoSemMudarNada() throws Exception {
        Cenario c = cenarioAguardandoAprovacao();
        UUID messageId = UUID.randomUUID();

        listener.receber(envelope(messageId, c.saga().getId(), c.os().getId(), "OrcamentoAprovado",
                Map.of("pagamentoId", "nao-e-uuid", "linkPagamento", "x")), null);

        assertThat(processadas()).isEqualTo(1);
        assertThat(estadoSaga(c.saga().getId())).isEqualTo("AGUARDANDO_APROVACAO");
    }

    @Test
    void envelopeMalformadoLancaEErroELogado() {
        assertThatThrownBy(() -> listener.receber("isto nao e json", null)).isInstanceOf(RuntimeException.class);
        assertThat(processadas()).isZero();
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.ERROR));
    }

    @Test
    void tipoDesconhecidoLancaENaoMarcaProcessada() throws Exception {
        Cenario c = cenarioAguardandoAprovacao();
        String corpo = envelope(UUID.randomUUID(), c.saga().getId(), c.os().getId(), "Inventada", Map.of());

        assertThatThrownBy(() -> listener.receber(corpo, "   ")).isInstanceOf(RuntimeException.class);

        assertThat(processadas()).isZero();
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.ERROR));
    }
}
