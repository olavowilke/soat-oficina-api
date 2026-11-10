package br.com.oficina.shared.infrastructure.mensageria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
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
 * <p>Se o envio de uma linha falha, ela fica pendente com espera exponencial
 * (até 5 minutos) e o relay segue com as outras sagas. As linhas seguintes da mesma
 * saga esperam: a consulta só libera uma linha sem anterior pendente da mesma saga. Se o processo cai depois do envio e antes
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
    private final TransactionTemplate tx;
    private final String filaBilling;
    private final String filaExecucao;

    public OutboxRelay(OutboxJpaRepository repository, SqsTemplate sqsTemplate, ObjectMapper mapper,
                       TracingMensagens tracing, TransactionTemplate tx,
                       @Value("${oficina.filas.billing-comandos}") String filaBilling,
                       @Value("${oficina.filas.execucao-comandos}") String filaExecucao) {
        this.repository = repository;
        this.sqsTemplate = sqsTemplate;
        this.mapper = mapper;
        this.tracing = tracing;
        this.tx = tx;
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

    /**
     * Uma passada, em uma transação própria (TransactionTemplate, não proxy: o
     * {@link #executar()} chama este método em {@code this}).
     */
    public void enviarPendentes() {
        tx.executeWithoutResult(status -> enviarPendentesNaTransacao());
    }

    private void enviarPendentesNaTransacao() {
        List<OutboxData> pendentes = repository.buscarPendentesComTrava();
        Set<UUID> sagasComFalha = new HashSet<>();
        for (OutboxData linha : pendentes) {
            if (sagasComFalha.contains(linha.getSagaId())) continue;
            try {
                enviar(linha);
                linha.setEnviadoEm(Instant.now());
                linha.setProximaTentativaEm(null);
            } catch (RuntimeException | JsonProcessingException e) {
                sagasComFalha.add(linha.getSagaId());
                int tentativa = linha.getTentativas() + 1;
                Duration espera = esperaDaTentativa(tentativa);
                linha.setTentativas(tentativa);
                linha.setProximaTentativaEm(Instant.now().plus(espera));
                log.warn("Falha ao enviar a linha {} da outbox ({}), tentativa {}; nova tentativa em {} s: {}",
                        linha.getId(), linha.getTipo(), tentativa, espera.toSeconds(), e.toString());
            }
        }
    }

    /** Espera exponencial: 2, 4, 8 ... segundos, no máximo 5 minutos. */
    static Duration esperaDaTentativa(int tentativa) {
        long segundos = tentativa >= 9 ? 300 : Math.min(300L, 1L << tentativa);
        return Duration.ofSeconds(segundos);
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
