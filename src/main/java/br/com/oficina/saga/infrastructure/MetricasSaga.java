package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.EstadoSaga;
import br.com.oficina.saga.gateways.MetricasSagaGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas da saga, lidas pelo nri-prometheus e usadas no dashboard e nos alertas do New Relic:
 * <ul>
 *     <li>{@code oficina_saga_ativas{estado}}: gauge, sagas ativas por estado.</li>
 *     <li>{@code oficina_saga_finalizadas_total{resultado}}: counter, CONCLUIDA, COMPENSADA ou FALHOU.</li>
 * </ul>
 */
@Component
public class MetricasSaga implements MetricasSagaGateway {

    private static final Logger log = LoggerFactory.getLogger(MetricasSaga.class);

    private final SagaJpaRepository repository;
    private final Map<EstadoSaga, AtomicLong> ativas = new EnumMap<>(EstadoSaga.class);
    private final Map<EstadoSaga, Counter> finalizadas = new EnumMap<>(EstadoSaga.class);
    private final AtomicBoolean emFalha = new AtomicBoolean();

    public MetricasSaga(SagaJpaRepository repository, MeterRegistry registry) {
        this.repository = repository;
        Arrays.stream(EstadoSaga.values()).forEach(estado -> {
            if (estado.ativo()) {
                AtomicLong valor = new AtomicLong();
                ativas.put(estado, valor);
                registry.gauge("oficina.saga.ativas", Tags.of("estado", estado.name()), valor);
            } else {
                finalizadas.put(estado, registry.counter("oficina.saga.finalizadas", "resultado", estado.name()));
            }
        });
    }

    @Override
    public void sagaFinalizada(EstadoSaga estadoFinal) {
        Counter counter = finalizadas.get(estadoFinal);
        if (counter != null) {
            counter.increment();
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void atualizar() {
        try {
            ativas.forEach((estado, valor) -> valor.set(repository.countByEstado(estado)));
            emFalha.set(false);
        } catch (RuntimeException e) {
            if (emFalha.compareAndSet(false, true)) {
                log.warn("Não foi possível contar as sagas ativas; mantém o último valor: {}", e.getMessage());
            }
        }
    }
}
