package br.com.oficina.ordemservico.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.NotificacaoGateway;
import br.com.oficina.ordemservico.gateways.NotificacaoStatusOrdem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Orquestra a notificação de status ao cliente após uma transição da OS.
 * <p>
 * Resolve o e-mail do cliente e delega o envio ao {@link NotificacaoGateway}.
 * Falhas de notificação são registradas mas <b>nunca</b> propagadas — a comunicação
 * ao cliente é um efeito colateral que não pode quebrar a transação de negócio.
 */
@Component
public class NotificadorStatusOrdem {

    private static final Logger log = LoggerFactory.getLogger(NotificadorStatusOrdem.class);

    /** Transições cuja mudança de status é relevante comunicar ao cliente. */
    private static final Set<StatusOS> STATUS_NOTIFICAVEIS = EnumSet.of(
            StatusOS.AGUARDANDO_APROVACAO,
            StatusOS.EM_EXECUCAO,
            StatusOS.FINALIZADA,
            StatusOS.ENTREGUE,
            StatusOS.CANCELADA);

    private final ClienteGateway clienteGateway;
    private final NotificacaoGateway notificacaoGateway;

    public NotificadorStatusOrdem(ClienteGateway clienteGateway,
                                  NotificacaoGateway notificacaoGateway) {
        this.clienteGateway = clienteGateway;
        this.notificacaoGateway = notificacaoGateway;
    }

    public void notificar(OrdemServico ordemServico) {
        if (ordemServico == null || !STATUS_NOTIFICAVEIS.contains(ordemServico.getStatus())) {
            return;
        }
        try {
            Optional<Cliente> cliente = clienteGateway.findById(ordemServico.getClienteId());
            if (cliente.isEmpty() || cliente.get().getEmail() == null || cliente.get().getEmail().isBlank()) {
                log.warn("OS {} sem e-mail de cliente para notificar (status {})",
                        ordemServico.getId(), ordemServico.getStatus());
                return;
            }
            notificacaoGateway.notificarAtualizacaoStatus(new NotificacaoStatusOrdem(
                    ordemServico.getId(),
                    ordemServico.getStatus(),
                    cliente.get().getEmail(),
                    cliente.get().getNome()));
        } catch (RuntimeException ex) {
            log.error("Falha ao notificar mudança de status da OS {} (status {}): {}",
                    ordemServico.getId(), ordemServico.getStatus(), ex.getMessage(), ex);
        }
    }
}
