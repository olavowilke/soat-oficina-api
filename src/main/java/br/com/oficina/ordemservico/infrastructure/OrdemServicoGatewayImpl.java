package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

@Repository
public class OrdemServicoGatewayImpl implements OrdemServicoGateway {

    private final OrdemServicoJpaRepository jpaRepository;
    private final MetricasOrdemServico metricas;

    public OrdemServicoGatewayImpl(OrdemServicoJpaRepository jpaRepository, MetricasOrdemServico metricas) {
        this.jpaRepository = jpaRepository;
        this.metricas = metricas;
    }

    /**
     * Persiste a OS (criação ou atualização) e emite as métricas de negócio
     * correspondentes (Tarefa 6). O estado anterior é lido antes de salvar
     * para permitir distinguir "criação" de "transição de status" só a
     * partir deste ponto de extensão — sem tocar nos Use Cases, que não
     * podem depender de Micrometer (ArchUnit).
     *
     * <p><b>Cuidado com aliasing do Hibernate:</b> {@link OrdemServicoData}
     * tem {@code @Id} sem {@code @GeneratedValue}, então o Spring Data
     * SEMPRE chama {@code merge()} (nunca {@code persist()}) — e
     * {@code merge()}, dentro do mesmo {@code EntityManager}, devolve a
     * MESMA instância gerenciada que este {@code findById()} acabou de
     * carregar, já mutada com os novos valores. Por isso o status e o
     * {@code atualizadoEm} "de antes" são capturados aqui como VALORES
     * (variáveis locais), antes do {@code save()} — guardar apenas a
     * referência ao objeto faria a comparação abaixo comparar o objeto já
     * mutado com ele mesmo, e a métrica de tempo por status nunca dispararia
     * (ver {@code OrdemServicoGatewayImplTest#deveRegistrarTempoNoStatusAnteriorMesmoComAliasingDoHibernate}).
     */
    @Override
    public OrdemServico save(OrdemServico ordemServico) {
        OrdemServicoData data = OrdemServicoMapper.toData(ordemServico);
        try {
            Optional<OrdemServicoData> existente = jpaRepository.findById(data.getId());
            Optional<StatusOS> statusAnterior = existente.map(OrdemServicoData::getStatus);
            Optional<LocalDateTime> atualizadoEmAnterior = existente.map(OrdemServicoData::getAtualizadoEm);

            OrdemServicoData salvo = jpaRepository.save(data);
            registrarMetricas(statusAnterior, atualizadoEmAnterior, salvo);
            return OrdemServicoMapper.toDomain(salvo);
        } catch (RuntimeException ex) {
            // Falha no processamento da OS: métrica de negócio consumida pela
            // condição de alerta "falha_processamento_os" (newrelic.tf) —
            // nunca deveria ser positiva em operação normal.
            metricas.registrarFalhaProcessamento();
            throw ex;
        }
    }

    private void registrarMetricas(Optional<StatusOS> statusAnterior,
                                    Optional<LocalDateTime> atualizadoEmAnterior,
                                    OrdemServicoData salvo) {
        if (statusAnterior.isEmpty()) {
            metricas.registrarOrdemCriada("api");
            return;
        }
        if (statusAnterior.get() != salvo.getStatus()) {
            LocalDateTime inicioDoStatusAnterior = atualizadoEmAnterior.orElse(salvo.getAtualizadoEm());
            Duration tempoNoStatusAnterior = Duration.between(inicioDoStatusAnterior, salvo.getAtualizadoEm());
            metricas.registrarTransicaoStatus(statusAnterior.get(), tempoNoStatusAnterior);
        }
    }

    @Override
    public Optional<OrdemServico> findById(UUID id) {
        return jpaRepository.findById(id).map(OrdemServicoMapper::toDomain);
    }

    @Override
    public List<OrdemServico> findAll() {
        return jpaRepository.findAll().stream().map(OrdemServicoMapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream().map(OrdemServicoMapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByStatus(StatusOS status) {
        return jpaRepository.findByStatus(status).stream().map(OrdemServicoMapper::toDomain).toList();
    }

    @Override
    public OptionalDouble tempoMedioExecucaoMinutos() {
        Double media = jpaRepository.findAverageExecutionMinutes();
        return media == null ? OptionalDouble.empty() : OptionalDouble.of(media);
    }
}
