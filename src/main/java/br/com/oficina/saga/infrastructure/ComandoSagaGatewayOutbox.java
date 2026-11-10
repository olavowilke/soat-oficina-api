package br.com.oficina.saga.infrastructure;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.shared.infrastructure.CorrelationIdFilter;
import br.com.oficina.shared.infrastructure.mensageria.OutboxData;
import br.com.oficina.shared.infrastructure.mensageria.OutboxJpaRepository;
import br.com.oficina.shared.infrastructure.mensageria.TracingMensagens;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Grava cada comando na outbox. A linha entra na transação de quem chama, então
 * o estado da saga e o comando saem juntos ou não saem. O OutboxRelay envia depois.
 */
@Component
public class ComandoSagaGatewayOutbox implements ComandoSagaGateway {

    private static final String BILLING = "billing-commands";
    private static final String EXECUCAO = "execution-commands";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper mapper;
    private final TracingMensagens tracing;

    public ComandoSagaGatewayOutbox(OutboxJpaRepository outbox, ObjectMapper mapper, TracingMensagens tracing) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.tracing = tracing;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void gerarOrcamento(SagaOrdemServico saga, OrdemServico os) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("clienteId", os.getClienteId().toString());
        ArrayNode itens = payload.putArray("itens");
        for (ItemServico s : os.getItensServico()) {
            itens.add(item("SERVICO", s.getServicoId(), s.getNomeServico(), 1, s.getValorCobrado()));
        }
        for (ItemPeca p : os.getItensPeca()) {
            itens.add(item("PECA", p.getPecaId(), p.getNomePeca(), p.getQuantidade(), p.getValorUnitario()));
        }
        gravar(BILLING, "GerarOrcamento", saga, payload);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void iniciarExecucao(SagaOrdemServico saga, OrdemServico os) {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode servicos = payload.putArray("servicos");
        for (ItemServico s : os.getItensServico()) {
            servicos.addObject().put("descricao", s.getNomeServico()).put("quantidade", 1);
        }
        payload.put("observacoes", os.getObservacoes());
        gravar(EXECUCAO, "IniciarExecucao", saga, payload);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void estornarPagamento(SagaOrdemServico saga, String motivo) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("motivo", motivo);
        gravar(BILLING, "EstornarPagamento", saga, payload);
    }

    private ObjectNode item(String tipo, UUID referenciaId, String descricao, int quantidade, BigDecimal valor) {
        ObjectNode item = mapper.createObjectNode();
        item.put("tipo", tipo);
        item.put("referenciaId", referenciaId.toString());
        item.put("descricao", descricao);
        item.put("quantidade", quantidade);
        item.put("valorUnitario", valor.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return item;
    }

    private void gravar(String destino, String tipo, SagaOrdemServico saga, ObjectNode payload) {
        OutboxData linha = new OutboxData();
        linha.setId(UUID.randomUUID());
        linha.setDestino(destino);
        linha.setTipo(tipo);
        linha.setSagaId(saga.getId());
        linha.setOsId(saga.getOsId());
        linha.setCorrelationId(correlationIdAtual());
        linha.setPayload(payload.toString());
        linha.setCriadoEm(Instant.now());
        linha.setTraceHeaders(tracing.cabecalhosSaida().get("newrelic"));
        outbox.save(linha);
    }

    private String correlationIdAtual() {
        String id = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        return (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    }
}
