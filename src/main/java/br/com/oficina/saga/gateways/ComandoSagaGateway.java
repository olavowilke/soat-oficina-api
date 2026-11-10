package br.com.oficina.saga.gateways;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.saga.entities.SagaOrdemServico;

/**
 * Port dos comandos que a saga envia a Billing e Execution. Cada método grava
 * o comando na outbox, na transação do chamador. O envio ao SQS é assíncrono.
 */
public interface ComandoSagaGateway {

    void gerarOrcamento(SagaOrdemServico saga, OrdemServico os);

    void iniciarExecucao(SagaOrdemServico saga, OrdemServico os);

    void estornarPagamento(SagaOrdemServico saga, String motivo);
}
