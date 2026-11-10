package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.EstadoSaga;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricasSagaTest {

    private final SagaJpaRepository repository = mock(SagaJpaRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private MetricasSaga metricas;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        metricas = new MetricasSaga(repository, registry);
        logger = (Logger) LoggerFactory.getLogger(MetricasSaga.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    private double ativas(EstadoSaga estado) {
        return registry.get("oficina.saga.ativas").tag("estado", estado.name()).gauge().value();
    }

    private double finalizadas(EstadoSaga resultado) {
        return registry.get("oficina.saga.finalizadas").tag("resultado", resultado.name()).counter().count();
    }

    @Test
    void todosOsEstadosAtivosExistemComZeroAntesDaPrimeiraLeitura() {
        for (EstadoSaga estado : EstadoSaga.values()) {
            if (estado.ativo()) {
                assertThat(ativas(estado)).isZero();
            }
        }
    }

    @Test
    void gaugePorEstadoLeOsContadoresDoRepositorio() {
        when(repository.countByEstado(any())).thenReturn(0L);
        when(repository.countByEstado(EstadoSaga.AGUARDANDO_PAGAMENTO)).thenReturn(4L);
        when(repository.countByEstado(EstadoSaga.EM_EXECUCAO)).thenReturn(2L);

        metricas.atualizar();

        assertThat(ativas(EstadoSaga.AGUARDANDO_PAGAMENTO)).isEqualTo(4.0);
        assertThat(ativas(EstadoSaga.EM_EXECUCAO)).isEqualTo(2.0);
        assertThat(ativas(EstadoSaga.ESTORNANDO)).isZero();
    }

    @Test
    void emFalhaMantemOUltimoValorELogaUmWarnPorSequencia() {
        when(repository.countByEstado(any())).thenReturn(0L);
        when(repository.countByEstado(EstadoSaga.EM_EXECUCAO)).thenReturn(2L);
        metricas.atualizar();
        when(repository.countByEstado(any())).thenThrow(new IllegalStateException("banco fora"));

        metricas.atualizar();
        metricas.atualizar();

        assertThat(ativas(EstadoSaga.EM_EXECUCAO)).isEqualTo(2.0);
        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.WARN).hasSize(1);
    }

    @Test
    void contadorSobeUmaVezPorResultadoTerminal() {
        metricas.sagaFinalizada(EstadoSaga.CONCLUIDA);
        metricas.sagaFinalizada(EstadoSaga.FALHOU);
        metricas.sagaFinalizada(EstadoSaga.FALHOU);
        metricas.sagaFinalizada(EstadoSaga.COMPENSADA);

        assertThat(finalizadas(EstadoSaga.CONCLUIDA)).isEqualTo(1.0);
        assertThat(finalizadas(EstadoSaga.COMPENSADA)).isEqualTo(1.0);
        assertThat(finalizadas(EstadoSaga.FALHOU)).isEqualTo(2.0);
    }

    @Test
    void estadoAtivoNaoMexeNoContador() {
        metricas.sagaFinalizada(EstadoSaga.EM_EXECUCAO);

        assertThat(finalizadas(EstadoSaga.CONCLUIDA)).isZero();
        assertThat(finalizadas(EstadoSaga.COMPENSADA)).isZero();
        assertThat(finalizadas(EstadoSaga.FALHOU)).isZero();
    }
}
