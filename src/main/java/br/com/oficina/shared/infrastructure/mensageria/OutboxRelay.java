package br.com.oficina.shared.infrastructure.mensageria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Envia as linhas pendentes da outbox ao SQS. Cada passada pega até 50 linhas,
 * as mais antigas primeiro, com {@code FOR UPDATE SKIP LOCKED}.
 *
 * <p>Se o envio de uma linha falha, ela fica pendente e o relay segue com as
 * outras. Ele pula as linhas seguintes da mesma saga na passada, para não
 * inverter a ordem dos comandos dela. Se o processo cai depois do envio e antes
 * do commit, a mensagem sai de novo. Os consumidores ignoram {@code messageId} repetido.
 */
@Component
public class OutboxRelay {

    static final String DESTINO_BILLING = "billing-commands";
    static final String DESTINO_EXECUCAO = "execution-commands";

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxJpaRepository repository;
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper mapper;
    private final TracingMensagens tracing;
    private final String filaBilling;
    private final String filaExecucao;

    public OutboxRelay(OutboxJpaRepository repository, SqsTemplate sqsTemplate, ObjectMapper mapper,
                       TracingMensagens tracing,
                       @Value("${oficina.filas.billing-comandos}") String filaBilling,
                       @Value("${oficina.filas.execucao-comandos}") String filaExecucao) {
        this.repository = repository;
        this.sqsTemplate = sqsTemplate;
        this.mapper = mapper;
        this.tracing = tracing;
        this.filaBilling = filaBilling;
        this.filaExecucao = filaExecucao;
    }

    @Scheduled(fixedDelayString = "${oficina.outbox.intervalo:PT1S}",
            initialDelayString = "${oficina.outbox.intervalo:PT1S}")
    public void executar() {
        try {
            enviarPendentes();
        } catch (RuntimeException e) {
            log.error("Falha na passada do relay da outbox", e);
        }
    }

    @Transactional
    public void enviarPendentes() {
        List<OutboxData> pendentes = repository.buscarPendentesComTrava();
        Set<UUID> sagasComFalha = new HashSet<>();
        for (OutboxData linha : pendentes) {
            if (sagasComFalha.contains(linha.getSagaId())) continue;
            try {
                enviar(linha);
                linha.setEnviadoEm(Instant.now());
            } catch (RuntimeException | JsonProcessingException e) {
                sagasComFalha.add(linha.getSagaId());
                log.warn("Falha ao enviar a linha {} da outbox ({}); segue pendente: {}", linha.getId(), linha.getTipo(), e.toString());
            }
        }
    }

    private void enviar(OutboxData linha) throws JsonProcessingException {
        String fila = filaDoDestino(linha.getDestino());
        MensagemSaga mensagem = new MensagemSaga(linha.getId(), linha.getSagaId(), linha.getOsId(), linha.getTipo(),
                linha.getCorrelationId(), linha.getCriadoEm(), mapper.readTree(linha.getPayload()));
        String corpo = mapper.writeValueAsString(mensagem);
        Map<String, String> trace = tracing.cabecalhosSaida();
        sqsTemplate.send(to -> {
            to.queue(fila).payload(corpo)
                    .header("type", linha.getTipo())
                    .header("correlationId", linha.getCorrelationId());
            String newrelic = trace.get("newrelic");
            if (newrelic != null) to.header("newrelic", newrelic);
        });
    }

    private String filaDoDestino(String destino) {
        return switch (destino) {
            case DESTINO_BILLING -> filaBilling;
            case DESTINO_EXECUCAO -> filaExecucao;
            default -> throw new IllegalStateException("Destino de outbox desconhecido: " + destino);
        };
    }
}
