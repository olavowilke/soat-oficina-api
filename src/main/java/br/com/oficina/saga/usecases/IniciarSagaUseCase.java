package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.ordemservico.usecases.NotificadorStatusOrdem;
import br.com.oficina.saga.entities.SagaJaAtivaException;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.ComandoSagaGateway;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Envia a OS para orçamento e inicia a saga. Tudo na mesma transação:
 * a OS, a saga e o comando na outbox saem juntos ou não saem.
 */
@Service
public class IniciarSagaUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final SagaGateway sagaGateway;
    private final ComandoSagaGateway comandos;
    private final NotificadorStatusOrdem notificador;
    private final Clock clock;

    public IniciarSagaUseCase(OrdemServicoGateway ordemServicoGateway, SagaGateway sagaGateway,
                              ComandoSagaGateway comandos, NotificadorStatusOrdem notificador, Clock clock) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.sagaGateway = sagaGateway;
        this.comandos = comandos;
        this.notificador = notificador;
        this.clock = clock;
    }

    @Transactional
    public SagaOrdemServico execute(UUID osId) {
        OrdemServico os = ordemServicoGateway.findById(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        if (sagaGateway.buscarAtivaPorOsId(osId).isPresent()) {
            throw new SagaJaAtivaException(osId);
        }
        os.enviarParaOrcamento();
        OrdemServico salva = ordemServicoGateway.save(os);
        SagaOrdemServico saga = sagaGateway.salvar(SagaOrdemServico.iniciar(osId, clock.instant()));
        comandos.gerarOrcamento(saga, salva);
        notificador.notificar(salva);
        return saga;
    }
}
