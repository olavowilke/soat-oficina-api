package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.StatusOS;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa {@link MetricasOrdemServico} isoladamente, com um {@link SimpleMeterRegistry}
 * em memória — sem subir contexto Spring nem exportar de verdade para o
 * Prometheus/New Relic. Cobre exatamente o contrato de nomes e tags que o
 * dashboard e as condições de alerta da Tarefa 3 esperam (ver
 * infra-k8s/observability/dashboard-oficina.json, infra-k8s/observability/alertas.md
 * e infra-k8s/terraform/newrelic.tf).
 */
class MetricasOrdemServicoTest {

    private MeterRegistry registry;
    private MetricasOrdemServico metricas;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricas = new MetricasOrdemServico(registry);
    }

    @Test
    void deveContarOrdemCriadaEOTempoPorStatus() {
        metricas.registrarOrdemCriada("abertura-consolidada");
        metricas.registrarTransicaoStatus(StatusOS.EM_DIAGNOSTICO, Duration.ofMinutes(30));

        assertThat(registry.counter("oficina.ordens.criadas", "origem", "abertura-consolidada").count()).isEqualTo(1.0);
        assertThat(registry.timer("oficina.ordens.tempo.status", "status", "EM_DIAGNOSTICO")
                .totalTime(TimeUnit.MINUTES)).isEqualTo(30.0);
    }

    @Test
    void deveContarFalhaDeIntegracaoPorNome() {
        metricas.registrarFalhaIntegracao("email");
        assertThat(registry.counter("oficina.integracoes.falhas", "integracao", "email").count()).isEqualTo(1.0);
    }

    @Test
    void deveContarFalhaNoProcessamentoDeOrdemDeServico() {
        // Métrica de negócio consumida pela condição de alerta
        // "falha_processamento_os" (infra-k8s/terraform/newrelic.tf) e pela
        // condição 3 de infra-k8s/observability/alertas.md — nunca deveria
        // ser positiva em operação normal, por isso não tem tag/facet.
        metricas.registrarFalhaProcessamento();
        assertThat(registry.counter("oficina.ordens.falhas").count()).isEqualTo(1.0);
    }
}
