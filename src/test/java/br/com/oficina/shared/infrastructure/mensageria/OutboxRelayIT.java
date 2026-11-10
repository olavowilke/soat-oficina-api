package br.com.oficina.shared.infrastructure.mensageria;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.BaseControllerIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayIT extends BaseControllerIT {

    @Autowired private ComandoSagaGateway comandos;
    @Autowired private SagaGateway sagaGateway;
    @Autowired private OrdemServicoGateway ordemServicoGateway;
    @Autowired private OutboxRelay relay;
    @Autowired private SqsAsyncClient sqs;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private TransactionTemplate tx;
    @Value("${oficina.filas.billing-comandos}") private String filaBilling;
    @Value("${oficina.filas.execucao-comandos}") private String filaExecucao;

    private OrdemServico osComItens() {
        UUID[] ids = inserirClienteEVeiculo();
        OrdemServico os = OrdemServico.nova(ids[0], ids[1], "Barulho no freio");
        os.adicionarServico(ItemServico.novo(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.5")));
        os.adicionarPeca(ItemPeca.novo(UUID.randomUUID(), "Filtro", 2, new BigDecimal("25")));
        return ordemServicoGateway.save(os);
    }

    private List<Message> receber(String fila, int esperado) throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName(fila)).get().queueUrl();
        List<Message> todas = new ArrayList<>();
        for (int i = 0; i < 10 && todas.size() < esperado; i++) {
            todas.addAll(sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)
                    .messageAttributeNames("All")).get().messages());
        }
        return todas;
    }

    @Test
    void gerarOrcamentoGravaLinhaComItensDoContrato() throws Exception {
        OrdemServico os = osComItens();
        SagaOrdemServico saga = sagaGateway.salvar(SagaOrdemServico.iniciar(os.getId(), Instant.now()));
        MDC.put("correlationId", "corr-123");
        try {
            tx.executeWithoutResult(s -> comandos.gerarOrcamento(saga, os));
        } finally {
            MDC.remove("correlationId");
        }

        var linha = jdbc.queryForMap("SELECT destino, tipo, correlation_id, payload::text AS payload, enviado_em "
                + "FROM outbox WHERE saga_id = ?", saga.getId());
        assertThat(linha.get("destino")).isEqualTo("billing-commands");
        assertThat(linha.get("tipo")).isEqualTo("GerarOrcamento");
        assertThat(linha.get("correlation_id")).isEqualTo("corr-123");
        assertThat(linha.get("enviado_em")).isNull();

        JsonNode p = mapper.readTree((String) linha.get("payload"));
        assertThat(p.get("clienteId").asText()).isEqualTo(os.getClienteId().toString());
        assertThat(p.get("itens")).hasSize(2);
        JsonNode servico = p.get("itens").get(0);
        assertThat(servico.get("tipo").asText()).isEqualTo("SERVICO");
        assertThat(servico.get("referenciaId").asText())
                .isEqualTo(os.getItensServico().get(0).getServicoId().toString());
        assertThat(servico.get("descricao").asText()).isEqualTo("Troca de óleo");
        assertThat(servico.get("quantidade").asInt()).isEqualTo(1);
        assertThat(servico.get("valorUnitario").isTextual()).isTrue();
        assertThat(servico.get("valorUnitario").asText()).isEqualTo("150.50");
        JsonNode peca = p.get("itens").get(1);
        assertThat(peca.get("tipo").asText()).isEqualTo("PECA");
        assertThat(peca.get("quantidade").asInt()).isEqualTo(2);
        assertThat(peca.get("valorUnitario").asText()).isEqualTo("25.00");
    }

    @Test
    void semCorrelationIdNoMdcGeraUmUuid() {
        OrdemServico os = osComItens();
        SagaOrdemServico saga = sagaGateway.salvar(SagaOrdemServico.iniciar(os.getId(), Instant.now()));

        tx.executeWithoutResult(s -> comandos.estornarPagamento(saga, "OS cancelada"));

        String corr = jdbc.queryForObject("SELECT correlation_id FROM outbox WHERE saga_id = ?",
                String.class, saga.getId());
        assertThat(UUID.fromString(corr)).isNotNull();
    }

    @Test
    void relayEnviaParaFilaDoDestino() throws Exception {
        receber(filaBilling, 0);
        OrdemServico os = osComItens();
        SagaOrdemServico saga = sagaGateway.salvar(SagaOrdemServico.iniciar(os.getId(), Instant.now()));
        tx.executeWithoutResult(s -> {
            comandos.gerarOrcamento(saga, os);
            comandos.iniciarExecucao(saga, os);
            comandos.estornarPagamento(saga, "Pagamento duplicado");
        });

        relay.enviarPendentes();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE enviado_em IS NULL", Integer.class))
                .isZero();
        List<Message> billing = receber(filaBilling, 2);
        List<Message> execucao = receber(filaExecucao, 1);
        assertThat(billing.stream().map(m -> tipo(m.body())).toList())
                .containsExactlyInAnyOrder("GerarOrcamento", "EstornarPagamento");
        assertThat(execucao).hasSize(1);
        JsonNode exec = mapper.readTree(execucao.get(0).body());
        assertThat(exec.get("type").asText()).isEqualTo("IniciarExecucao");
        assertThat(exec.get("sagaId").asText()).isEqualTo(saga.getId().toString());
        assertThat(exec.get("osId").asText()).isEqualTo(os.getId().toString());
        assertThat(exec.get("messageId").asText()).isNotBlank();
        assertThat(exec.get("occurredAt").asText()).endsWith("Z");
        assertThat(exec.get("payload").get("servicos").get(0).get("descricao").asText()).isEqualTo("Troca de óleo");
        assertThat(exec.get("payload").get("observacoes").asText()).isEqualTo("Barulho no freio");
        var atributos = execucao.get(0).messageAttributes();
        assertThat(atributos.get("type").stringValue()).isEqualTo("IniciarExecucao");
        assertThat(atributos.get("correlationId").stringValue()).isNotBlank();
        assertThat(atributos).doesNotContainKey("newrelic");
    }

    @Test
    void falhaDeEnvioDeixaALinhaPendente() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO outbox (id, destino, tipo, saga_id, os_id, correlation_id, payload, criado_em) "
                        + "VALUES (?, 'destino-desconhecido', 'X', ?, ?, 'c', '{}'::jsonb, now())",
                id, UUID.randomUUID(), UUID.randomUUID());

        relay.enviarPendentes();

        assertThat(jdbc.queryForObject("SELECT enviado_em FROM outbox WHERE id = ?", Timestamp.class, id)).isNull();
    }

    private String tipo(String corpo) {
        try {
            return mapper.readTree(corpo).get("type").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
