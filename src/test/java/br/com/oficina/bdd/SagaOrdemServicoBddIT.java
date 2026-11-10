package br.com.oficina.bdd;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;

/**
 * Roda os cenários Cucumber da saga. O nome termina em IT: o Failsafe o executa e o JaCoCo o conta.
 *
 * <p>Por que não usar {@code @Suite}: com o Failsafe 3.5.3, uma falha de cenário dentro de uma
 * suíte não falha o build (o relatório mostra "Tests run: 0"). Aqui o Launcher roda o engine do
 * Cucumber e o teste falha se qualquer cenário falhar.
 */
class SagaOrdemServicoBddIT {

    private static final int CENARIOS = 4;

    @Test
    void cenariosDaSagaPassam() {
        LauncherDiscoveryRequest pedido = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClasspathResource("features"))
                .configurationParameter(GLUE_PROPERTY_NAME, "br.com.oficina.bdd")
                .configurationParameter(PLUGIN_PROPERTY_NAME, "summary")
                .build();
        SummaryGeneratingListener resumo = new SummaryGeneratingListener();

        LauncherFactory.create().execute(pedido, resumo);

        var r = resumo.getSummary();
        assertThat(r.getFailures()).as("falhas").isEmpty();
        assertThat(r.getTestsSucceededCount()).as("cenários que passaram").isEqualTo(CENARIOS);
    }
}
