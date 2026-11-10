package br.com.oficina.bdd;

import br.com.oficina.shared.infrastructure.mensageria.MensagemSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Faz o papel do Billing e da Execução nas filas. Lê comandos das filas de comando e
 * escreve respostas na fila de respostas.
 */
class FilasDeTeste {

    private final SqsAsyncClient sqs;
    private final ObjectMapper mapper;
    private final String filaRespostas;
    private final Runnable antesDeLer;
    private UUID osAtual;

    /**
     * @param antesDeLer roda antes de cada leitura. Os ITs deixam o relay da outbox lento (24 h),
     *                   então o teste o chama aqui para empurrar os comandos pendentes.
     */
    FilasDeTeste(SqsAsyncClient sqs, ObjectMapper mapper, String filaRespostas, Runnable antesDeLer) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.filaRespostas = filaRespostas;
        this.antesDeLer = antesDeLer;
    }

    /** Daqui em diante, só as mensagens desta OS contam. As de outras OS são apagadas. */
    void filtrarPor(UUID osId) {
        this.osAtual = osId;
    }

    void esvaziar(String... filas) {
        for (String fila : filas) {
            String url = urlDa(fila);
            executar(sqs.purgeQueue(r -> r.queueUrl(url)));
        }
    }

    /** Espera um comando do tipo pedido para a OS atual. Apaga a mensagem que casa. */
    MensagemSaga receberComando(String fila, String type, Duration max) {
        String url = urlDa(fila);
        Instant limite = Instant.now().plus(max);
        while (Instant.now().isBefore(limite)) {
            antesDeLer.run();
            var resposta = executar(sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10)
                    .waitTimeSeconds(1).messageAttributeNames("All")));
            for (Message m : resposta.messages()) {
                MensagemSaga comando = ler(m.body());
                boolean daOs = osAtual != null && osAtual.equals(comando.osId());
                if (!daOs) {
                    apagar(url, m);
                } else if (type.equals(comando.type())) {
                    apagar(url, m);
                    return comando;
                }
            }
        }
        throw new AssertionError("Comando " + type + " não chegou em " + fila + " em " + max.toSeconds() + " s");
    }

    /** Envia uma resposta com o envelope completo, no mesmo sagaId, osId e correlationId do comando. */
    void responder(MensagemSaga comando, String type, Map<String, Object> payload) {
        MensagemSaga resposta = new MensagemSaga(UUID.randomUUID(), comando.sagaId(), comando.osId(), type,
                comando.correlationId(), Instant.now(), mapper.valueToTree(payload));
        String corpo;
        try {
            corpo = mapper.writeValueAsString(resposta);
        } catch (Exception e) {
            throw new IllegalStateException("Envelope inválido", e);
        }
        String url = urlDa(filaRespostas);
        executar(sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo)
                .messageAttributes(Map.of(
                        "type", atributo(type),
                        "correlationId", atributo(String.valueOf(comando.correlationId()))))));
    }

    private static MessageAttributeValue atributo(String valor) {
        return MessageAttributeValue.builder().dataType("String").stringValue(valor).build();
    }

    private MensagemSaga ler(String corpo) {
        try {
            return mapper.readValue(corpo, MensagemSaga.class);
        } catch (Exception e) {
            throw new IllegalStateException("Mensagem ilegível na fila: " + corpo, e);
        }
    }

    private void apagar(String url, Message m) {
        executar(sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle())));
    }

    private String urlDa(String fila) {
        return executar(sqs.getQueueUrl(r -> r.queueName(fila))).queueUrl();
    }

    private static <T> T executar(CompletableFuture<T> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
