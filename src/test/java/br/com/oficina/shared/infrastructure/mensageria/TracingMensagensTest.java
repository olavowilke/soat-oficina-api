package br.com.oficina.shared.infrastructure.mensageria;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TracingMensagensTest {

    private final TracingMensagens tracing = new TracingMensagens();

    @Test
    void semAgenteOsCabecalhosDeSaidaFicamVazios() {
        assertThat(tracing.cabecalhosSaida()).isEmpty();
    }

    @Test
    void semAgenteAceitarNaoFazNada() {
        assertThatCode(() -> tracing.aceitar(Map.of("newrelic", "abc"))).doesNotThrowAnyException();
        assertThatCode(() -> tracing.aceitar(Map.of())).doesNotThrowAnyException();
        assertThatCode(() -> tracing.aceitar(null)).doesNotThrowAnyException();
    }
}
