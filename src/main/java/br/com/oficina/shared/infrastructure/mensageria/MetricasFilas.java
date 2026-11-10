package br.com.oficina.shared.infrastructure.mensageria;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lê a cada 60 s o tamanho da DLQ deste serviço e expõe
 * {@code oficina_fila_dlq_mensagens{fila}}. Se a leitura falha, mantém o último valor
 * e escreve um WARN por sequência de falhas.
 */
@Component
public class MetricasFilas {

    private static final Logger log = LoggerFactory.getLogger(MetricasFilas.class);

    private final SqsAsyncClient sqs;
    private final String fila;
    private final AtomicLong mensagens = new AtomicLong();
    private final AtomicBoolean emFalha = new AtomicBoolean();
    private volatile String url;

    public MetricasFilas(SqsAsyncClient sqs, MeterRegistry registry,
                         @Value("${oficina.filas.respostas-dlq}") String fila) {
        this.sqs = sqs;
        this.fila = fila;
        registry.gauge("oficina.fila.dlq.mensagens", Tags.of("fila", fila), mensagens);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void atualizar() {
        try {
            if (url == null) {
                url = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(fila).build()).join().queueUrl();
            }
            String valor = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(url)
                            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                            .build())
                    .join().attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
            mensagens.set(Long.parseLong(valor));
            emFalha.set(false);
        } catch (RuntimeException e) {
            if (emFalha.compareAndSet(false, true)) {
                log.warn("Não foi possível ler o tamanho da DLQ {}; mantém o último valor: {}", fila, e.getMessage());
            }
        }
    }
}
