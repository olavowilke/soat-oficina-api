package br.com.oficina.shared.infrastructure.mensageria;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liga o trace distribuído do New Relic às mensagens SQS. Sem agente, a API
 * do New Relic não faz nada: {@link #cabecalhosSaida()} devolve um mapa vazio.
 */
@Component
public class TracingMensagens {

    /** Cabeçalhos de trace da transação atual, para enviar como atributos da mensagem. */
    public Map<String, String> cabecalhosSaida() {
        MapaHeaders headers = new MapaHeaders();
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        return new HashMap<>(headers.valores);
    }

    /** Liga a transação atual ao trace de quem enviou a mensagem. O newrelic-api 8.16.0 não tem TransportType.Queue; usa Other. */
    public void aceitar(Map<String, String> cabecalhos) {
        if (cabecalhos == null || cabecalhos.isEmpty()) return;
        MapaHeaders headers = new MapaHeaders();
        headers.valores.putAll(cabecalhos);
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Other, headers);
    }

    private static final class MapaHeaders implements Headers {

        private final Map<String, String> valores = new HashMap<>();

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.MESSAGE;
        }

        @Override
        public String getHeader(String nome) {
            return valores.get(nome);
        }

        @Override
        public Collection<String> getHeaders(String nome) {
            String valor = valores.get(nome);
            return valor == null ? List.of() : List.of(valor);
        }

        @Override
        public void setHeader(String nome, String valor) {
            valores.put(nome, valor);
        }

        @Override
        public void addHeader(String nome, String valor) {
            valores.put(nome, valor);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return valores.keySet();
        }

        @Override
        public boolean containsHeader(String nome) {
            return valores.containsKey(nome);
        }
    }
}
