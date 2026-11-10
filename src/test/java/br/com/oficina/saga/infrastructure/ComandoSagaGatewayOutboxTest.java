package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.shared.infrastructure.mensageria.OutboxData;
import br.com.oficina.shared.infrastructure.mensageria.OutboxJpaRepository;
import br.com.oficina.shared.infrastructure.mensageria.TracingMensagens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComandoSagaGatewayOutboxTest {

    @Mock private OutboxJpaRepository outbox;
    @Mock private TracingMensagens tracing;

    private OutboxData gravar() {
        ComandoSagaGatewayOutbox gateway = new ComandoSagaGatewayOutbox(outbox, new ObjectMapper(), tracing);
        SagaOrdemServico saga = SagaOrdemServico.iniciar(UUID.randomUUID(), Instant.now());
        gateway.estornarPagamento(saga, "motivo");
        ArgumentCaptor<OutboxData> captor = ArgumentCaptor.forClass(OutboxData.class);
        verify(outbox).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void comandoGuardaOCabecalhoDeTraceDoMomentoDaGravacao() {
        when(tracing.cabecalhosSaida()).thenReturn(Map.of("newrelic", "nr-na-gravacao"));

        assertThat(gravar().getTraceHeaders()).isEqualTo("nr-na-gravacao");
    }

    @Test
    void semTraceAColunaFicaNula() {
        when(tracing.cabecalhosSaida()).thenReturn(Map.of());

        assertThat(gravar().getTraceHeaders()).isNull();
    }
}
