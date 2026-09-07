package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cobre o ponto de extensão de métricas em {@link OrdemServicoGatewayImpl#save}.
 * <p>
 * O caso mais importante aqui ({@link #deveRegistrarTempoNoStatusAnteriorMesmoComAliasingDoHibernate})
 * reproduz deliberadamente o comportamento real do Hibernate, não um mock
 * ingênuo: {@code OrdemServicoData} tem {@code @Id} sem {@code @GeneratedValue},
 * então o Spring Data SEMPRE chama {@code merge()} (nunca {@code persist()});
 * dentro do mesmo {@code EntityManager}, {@code merge()} devolve a MESMA
 * instância gerenciada que um {@code findById()} anterior já havia carregado,
 * já mutada com os novos valores. Um mock onde {@code findById} e {@code save}
 * devolvem objetos DISTINTOS nunca reproduz esse aliasing e deixaria passar
 * uma implementação que lê o "status anterior" depois de já ter sido
 * sobrescrito pelo save — por isso o stub de {@code save} abaixo muta a MESMA
 * instância que o stub de {@code findById} devolveu, e a devolve de novo.
 */
@ExtendWith(MockitoExtension.class)
class OrdemServicoGatewayImplTest {

    @Mock
    private OrdemServicoJpaRepository jpaRepository;

    private MeterRegistry registry;
    private OrdemServicoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gateway = new OrdemServicoGatewayImpl(jpaRepository, new MetricasOrdemServico(registry));
    }

    @Test
    void deveRegistrarTempoNoStatusAnteriorMesmoComAliasingDoHibernate() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime atualizadoEmAntigo = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime atualizadoEmNovo = LocalDateTime.of(2026, 1, 1, 10, 30); // 30 min depois

        OrdemServicoData gerenciadaPeloEntityManager = new OrdemServicoData();
        gerenciadaPeloEntityManager.setId(id);
        gerenciadaPeloEntityManager.setClienteId(clienteId);
        gerenciadaPeloEntityManager.setVeiculoId(veiculoId);
        gerenciadaPeloEntityManager.setStatus(StatusOS.EM_DIAGNOSTICO);
        gerenciadaPeloEntityManager.setCriadoEm(criadoEm);
        gerenciadaPeloEntityManager.setAtualizadoEm(atualizadoEmAntigo);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(gerenciadaPeloEntityManager));
        // Simula merge(): muta a MESMA instância que findById() devolveu — é
        // exatamente isso que o Hibernate faz dentro do mesmo EntityManager.
        when(jpaRepository.save(any(OrdemServicoData.class))).thenAnswer(invocation -> {
            gerenciadaPeloEntityManager.setStatus(StatusOS.AGUARDANDO_APROVACAO);
            gerenciadaPeloEntityManager.setAtualizadoEm(atualizadoEmNovo);
            return gerenciadaPeloEntityManager;
        });

        OrdemServico osComNovoStatus = OrdemServico.reconstituir(
                id, clienteId, veiculoId, StatusOS.AGUARDANDO_APROVACAO, null,
                List.of(), List.of(), criadoEm, atualizadoEmNovo, null, null);

        gateway.save(osComNovoStatus);

        assertThat(registry.timer("oficina.ordens.tempo.status", "status", "EM_DIAGNOSTICO")
                .totalTime(TimeUnit.MINUTES)).isEqualTo(30.0);
        // Não é criação: não existia (na visão do gateway) um "antes" vazio.
        assertThat(registry.find("oficina.ordens.criadas").counter()).isNull();
    }

    @Test
    void naoDeveRegistrarTransicaoQuandoStatusPermaneceIgual() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime atualizadoEmAntigo = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime atualizadoEmNovo = LocalDateTime.of(2026, 1, 1, 9, 5);

        OrdemServicoData gerenciadaPeloEntityManager = new OrdemServicoData();
        gerenciadaPeloEntityManager.setId(id);
        gerenciadaPeloEntityManager.setClienteId(clienteId);
        gerenciadaPeloEntityManager.setVeiculoId(veiculoId);
        gerenciadaPeloEntityManager.setStatus(StatusOS.RECEBIDA);
        gerenciadaPeloEntityManager.setCriadoEm(criadoEm);
        gerenciadaPeloEntityManager.setAtualizadoEm(atualizadoEmAntigo);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(gerenciadaPeloEntityManager));
        when(jpaRepository.save(any(OrdemServicoData.class))).thenAnswer(invocation -> {
            // Só um campo qualquer muda (ex.: adicionar peça); status permanece.
            gerenciadaPeloEntityManager.setAtualizadoEm(atualizadoEmNovo);
            return gerenciadaPeloEntityManager;
        });

        OrdemServico osSemMudancaDeStatus = OrdemServico.reconstituir(
                id, clienteId, veiculoId, StatusOS.RECEBIDA, null,
                List.of(), List.of(), criadoEm, atualizadoEmNovo, null, null);

        gateway.save(osSemMudancaDeStatus);

        assertThat(registry.find("oficina.ordens.criadas").counter()).isNull();
        assertThat(registry.find("oficina.ordens.tempo.status").timer()).isNull();
    }

    @Test
    void deveRegistrarCriacaoQuandoNaoExisteRegistroAnterior() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.of(2026, 1, 1, 9, 0);

        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        OrdemServicoData salva = new OrdemServicoData();
        salva.setId(id);
        salva.setClienteId(clienteId);
        salva.setVeiculoId(veiculoId);
        salva.setStatus(StatusOS.RECEBIDA);
        salva.setCriadoEm(agora);
        salva.setAtualizadoEm(agora);
        when(jpaRepository.save(any(OrdemServicoData.class))).thenReturn(salva);

        OrdemServico osNova = OrdemServico.reconstituir(
                id, clienteId, veiculoId, StatusOS.RECEBIDA, null,
                List.of(), List.of(), agora, agora, null, null);

        gateway.save(osNova);

        assertThat(registry.counter("oficina.ordens.criadas", "origem", "api").count()).isEqualTo(1.0);
        // Criação não deve, além de contar como criação, também tentar
        // registrar uma transição de status (não há "status anterior").
        assertThat(registry.find("oficina.ordens.tempo.status").timer()).isNull();
    }
}
