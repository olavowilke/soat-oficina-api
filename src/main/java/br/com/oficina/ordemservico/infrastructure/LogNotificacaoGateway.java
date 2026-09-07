package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.gateways.NotificacaoGateway;
import br.com.oficina.ordemservico.gateways.NotificacaoStatusOrdem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de notificação padrão (dev/test): apenas registra em log, sem enviar
 * e-mail real. Ativo quando {@code app.notificacao.email.enabled} é falso ou ausente.
 */
@Component
@ConditionalOnProperty(name = "app.notificacao.email.enabled", havingValue = "false", matchIfMissing = true)
public class LogNotificacaoGateway implements NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(LogNotificacaoGateway.class);

    @Override
    public void notificarAtualizacaoStatus(NotificacaoStatusOrdem notificacao) {
        log.info("[NOTIFICACAO-MOCK] OS {} mudou para {} — e-mail para {} <{}> (envio real desabilitado)",
                notificacao.ordemServicoId(), notificacao.novoStatus(),
                notificacao.destinatarioNome(), notificacao.destinatarioEmail());
    }
}
