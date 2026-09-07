package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.StatusOS;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Métricas de negócio da Ordem de Serviço, emitidas via Micrometer, expostas
 * em {@code /api/actuator/prometheus} e coletadas pelo nri-prometheus do
 * cluster (não pelo agente Java do New Relic, que só instrumenta APM). Vive
 * deliberadamente na camada de infraestrutura — é chamada pelos adapters de
 * saída ({@link OrdemServicoGatewayImpl}, {@link EmailNotificacaoGateway}),
 * nunca pelos Use Cases: injetar Micrometer no núcleo violaria a regra de
 * dependência da Clean Architecture cobrada por
 * {@code br.com.oficina.architecture.ArchitectureTest}.
 *
 * <p>Os nomes e tags abaixo são um contrato com o dashboard e as condições de
 * alerta do New Relic já provisionados (Tarefa 3/Terraform):
 * <ul>
 *     <li>{@code oficina.ordens.criadas} (counter, tag {@code origem}) —
 *     painel "Volume diário de ordens de serviço criadas"
 *     (dashboard-oficina.json).</li>
 *     <li>{@code oficina.ordens.tempo.status} (timer, tag {@code status}) —
 *     painel "Tempo médio de execução por status da OS".</li>
 *     <li>{@code oficina.integracoes.falhas} (counter, tag {@code integracao}) —
 *     painel "Erros e falhas nas integrações externas".</li>
 *     <li>{@code oficina.ordens.falhas} (counter, sem tag) — condição de
 *     alerta "falha_processamento_os" (newrelic.tf) e condição 3 de
 *     alertas.md: qualquer ocorrência acima de zero já é crítica.</li>
 * </ul>
 * Mudar um nome ou uma tag aqui sem atualizar aqueles arquivos quebra os
 * painéis e os alertas silenciosamente.
 */
@Component
public class MetricasOrdemServico {

    private final MeterRegistry registry;

    public MetricasOrdemServico(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registra a criação de uma nova Ordem de Serviço.
     *
     * @param origem identifica o fluxo de criação (ex.: {@code "api"}).
     */
    public void registrarOrdemCriada(String origem) {
        registry.counter("oficina.ordens.criadas", "origem", origem).increment();
    }

    /**
     * Registra o tempo que uma OS permaneceu no status informado antes de
     * transicionar para o próximo.
     *
     * @param status  o status que a OS está deixando.
     * @param duracao o tempo que a OS permaneceu nesse status.
     */
    public void registrarTransicaoStatus(StatusOS status, Duration duracao) {
        registry.timer("oficina.ordens.tempo.status", "status", status.name())
                .record(duracao);
    }

    /**
     * Registra a falha de uma integração externa (ex.: envio de e-mail).
     *
     * @param integracao identifica a integração que falhou (ex.: {@code "email"}).
     */
    public void registrarFalhaIntegracao(String integracao) {
        registry.counter("oficina.integracoes.falhas", "integracao", integracao).increment();
    }

    /**
     * Registra uma falha no processamento (criação/atualização) de uma OS —
     * métrica de negócio que nunca deveria ser positiva em operação normal.
     */
    public void registrarFalhaProcessamento() {
        registry.counter("oficina.ordens.falhas").increment();
    }
}
