package br.com.oficina.shared.infrastructure.mensageria;

import br.com.oficina.saga.usecases.ProcessarRespostaSagaUseCase;
import br.com.oficina.saga.usecases.RespostaAdiantadaException;
import br.com.oficina.saga.usecases.RespostaSaga;
import br.com.oficina.shared.infrastructure.CorrelationIdFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Consome as respostas de Billing e Execution. Numa única transação: registra a
 * mensagem como processada, converte o envelope e chama o caso de uso. Se algo falha,
 * a transação desfaz o registro e a mensagem não é confirmada: a fila a entrega de novo.
 * Um envelope malformado ou um tipo desconhecido lança erro e segue para a DLQ.
 */
@Component
public class RespostasListener {

    private static final Logger log = LoggerFactory.getLogger(RespostasListener.class);
    private static final String MDC_SAGA_ID = "sagaId";

    private final ObjectMapper mapper;
    private final MensagemProcessadaJpaRepository processadas;
    private final ProcessarRespostaSagaUseCase useCase;
    private final TracingMensagens tracing;
    private final TransactionTemplate tx;

    public RespostasListener(ObjectMapper mapper, MensagemProcessadaJpaRepository processadas,
                             ProcessarRespostaSagaUseCase useCase, TracingMensagens tracing,
                             TransactionTemplate tx) {
        this.mapper = mapper;
        this.processadas = processadas;
        this.useCase = useCase;
        this.tracing = tracing;
        this.tx = tx;
    }

    @SqsListener("${oficina.filas.respostas}")
    @Trace(dispatcher = true)
    public void receber(String corpo, @Header(name = "newrelic", required = false) String newrelic) {
        if (newrelic != null && !newrelic.isBlank()) {
            tracing.aceitar(Map.of("newrelic", newrelic));
        }
        MensagemSaga mensagem;
        try {
            mensagem = mapper.readValue(corpo, MensagemSaga.class);
            if (mensagem.messageId() == null || mensagem.sagaId() == null || mensagem.type() == null) {
                throw new IllegalArgumentException("envelope sem messageId, sagaId ou type");
            }
        } catch (Exception e) {
            log.error("Envelope malformado na fila de respostas: {}", e.toString());
            throw new IllegalArgumentException("Envelope malformado", e);
        }
        colocarNoMdc(mensagem);
        try {
            tx.executeWithoutResult(status -> tratar(mensagem));
        } catch (RuntimeException e) {
            // Conflito de versão é corrida esperada: a nova entrega resolve. Outro erro é falha real.
            if (e instanceof RespostaAdiantadaException) {
                // O caso de uso já logou o WARN. Resposta adiantada é redelivery esperada, não erro.
                throw e;
            }
            if (e instanceof ObjectOptimisticLockingFailureException) {
                log.warn("Conflito de versão em {} da saga {} (mensagem {}); volta para a fila",
                        mensagem.type(), mensagem.sagaId(), mensagem.messageId());
            } else {
                log.error("Falha ao tratar {} da saga {} (mensagem {}); volta para a fila: {}",
                        mensagem.type(), mensagem.sagaId(), mensagem.messageId(), e.toString());
            }
            throw e;
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID);
            MDC.remove(MDC_SAGA_ID);
        }
    }

    private void tratar(MensagemSaga mensagem) {
        if (processadas.registrar(mensagem.messageId(), Instant.now()) == 0) {
            log.info("Mensagem {} já processada; ignorada", mensagem.messageId());
            return;
        }
        Map<String, Object> payload = mensagem.payload() == null || mensagem.payload().isNull()
                ? Map.of()
                : mapper.convertValue(mensagem.payload(), new TypeReference<Map<String, Object>>() { });
        useCase.execute(new RespostaSaga(mensagem.messageId(), mensagem.sagaId(), mensagem.osId(),
                mensagem.type(), payload));
    }

    /** Nunca grava valor vazio no MDC. */
    private void colocarNoMdc(MensagemSaga mensagem) {
        String correlationId = mensagem.correlationId();
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID, correlationId);
        }
        MDC.put(MDC_SAGA_ID, mensagem.sagaId().toString());
    }
}
