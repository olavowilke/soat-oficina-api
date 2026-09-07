package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.NotificacaoGateway;
import br.com.oficina.ordemservico.gateways.NotificacaoStatusOrdem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adapter de notificação por e-mail (SMTP/SendGrid). Ativo em produção quando
 * {@code app.notificacao.email.enabled=true}. Requer {@code spring.mail.*} configurado
 * (host, credenciais). Uma falha de envio é registrada e relançada como
 * {@link MailException} — o {@code NotificadorStatusOrdem} isola o erro da transação.
 */
@Component
@ConditionalOnProperty(name = "app.notificacao.email.enabled", havingValue = "true")
public class EmailNotificacaoGateway implements NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacaoGateway.class);

    private final JavaMailSender mailSender;
    private final String remetente;
    private final MetricasOrdemServico metricas;

    public EmailNotificacaoGateway(JavaMailSender mailSender,
                                   @Value("${app.notificacao.email.remetente:nao-responder@oficina.com}") String remetente,
                                   MetricasOrdemServico metricas) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.metricas = metricas;
    }

    @Override
    public void notificarAtualizacaoStatus(NotificacaoStatusOrdem notificacao) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(notificacao.destinatarioEmail());
        mensagem.setSubject("Atualização da sua Ordem de Serviço " + notificacao.ordemServicoId());
        mensagem.setText(corpo(notificacao));
        try {
            mailSender.send(mensagem);
            log.info("E-mail de status enviado para {} (OS {} → {})",
                    notificacao.destinatarioEmail(), notificacao.ordemServicoId(), notificacao.novoStatus());
        } catch (MailException ex) {
            // Métrica de negócio (Tarefa 6): painel "Erros e falhas nas
            // integrações externas" (dashboard-oficina.json). O use case
            // chamador (NotificadorStatusOrdem) já isola esta exceção da
            // transação — relançar aqui preserva esse comportamento.
            metricas.registrarFalhaIntegracao("email");
            log.error("Falha ao enviar e-mail de status para {} (OS {} → {}): {}",
                    notificacao.destinatarioEmail(), notificacao.ordemServicoId(), notificacao.novoStatus(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private String corpo(NotificacaoStatusOrdem notificacao) {
        String nome = notificacao.destinatarioNome() != null ? notificacao.destinatarioNome() : "Cliente";
        return "Olá, " + nome + ".\n\n"
                + "A sua Ordem de Serviço " + notificacao.ordemServicoId()
                + " foi atualizada para o status: " + descricao(notificacao.novoStatus()) + ".\n\n"
                + "Atenciosamente,\nOficina Mecânica";
    }

    private String descricao(StatusOS status) {
        return switch (status) {
            case AGUARDANDO_APROVACAO -> "Aguardando aprovação do orçamento";
            case EM_EXECUCAO -> "Em execução";
            case FINALIZADA -> "Finalizada";
            case ENTREGUE -> "Entregue";
            case CANCELADA -> "Cancelada";
            default -> status.name();
        };
    }
}
