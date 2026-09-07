package br.com.oficina.ordemservico.gateways;

/**
 * Port de saída para notificação do cliente. O núcleo (Use Cases) apenas conhece
 * este contrato; o adapter concreto (e-mail SMTP, log, etc.) vive na Infra.
 */
public interface NotificacaoGateway {

    /**
     * Notifica o cliente sobre a atualização de status de uma OS. Implementações
     * não devem propagar exceções que quebrem a transação de negócio.
     */
    void notificarAtualizacaoStatus(NotificacaoStatusOrdem notificacao);
}
