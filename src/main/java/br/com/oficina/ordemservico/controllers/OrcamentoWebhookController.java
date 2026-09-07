package br.com.oficina.ordemservico.controllers;

import br.com.oficina.ordemservico.presenters.OrdemServicoPresenter;
import br.com.oficina.ordemservico.presenters.OrdemServicoResponse;
import br.com.oficina.ordemservico.usecases.ProcessarDecisaoOrcamentoUseCase;
import br.com.oficina.shared.domain.ApiError;
import br.com.oficina.shared.domain.AutenticacaoWebhookException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe notificações externas de decisão de orçamento (aprovação/recusa).
 * <p>
 * A origem é autenticada por um token compartilhado (Secret) enviado no header
 * {@code X-Webhook-Token} — a rota não usa JWT, por isso fica sob {@code /webhooks/**}
 * (liberada no {@code SecurityConfig}) e valida o token manualmente.
 */
@RestController
@Tag(name = "Webhooks", description = "Integrações de entrada de sistemas externos")
public class OrcamentoWebhookController {

    static final String HEADER_TOKEN = "X-Webhook-Token";

    private final ProcessarDecisaoOrcamentoUseCase processarDecisao;
    private final OrdemServicoPresenter presenter;
    private final String tokenEsperado;

    public OrcamentoWebhookController(ProcessarDecisaoOrcamentoUseCase processarDecisao,
                                      OrdemServicoPresenter presenter,
                                      @Value("${app.webhook.orcamento.token}") String tokenEsperado) {
        this.processarDecisao = processarDecisao;
        this.presenter = presenter;
        this.tokenEsperado = tokenEsperado;
    }

    @Operation(summary = "Webhook de decisão de orçamento",
            description = """
                    Recebe de um sistema externo a decisão (APROVADO/RECUSADO) sobre o orçamento
                    de uma OS. Autenticado pelo header X-Webhook-Token (token compartilhado).
                    Idempotente: reentregas para uma OS já decidida retornam 200 sem efeito duplo.
                    Aprovado → EM_EXECUCAO; Recusado → CANCELADA (exige AGUARDANDO_APROVACAO).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decisão processada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token de webhook ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não está aguardando aprovação",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirements
    @PostMapping("/webhooks/orcamento")
    public ResponseEntity<OrdemServicoResponse> receberDecisao(
            @RequestHeader(value = HEADER_TOKEN, required = false) String token,
            @Valid @RequestBody DecisaoOrcamentoWebhookRequest request) {
        validarToken(token);
        return ResponseEntity.ok(presenter.present(
                processarDecisao.execute(request.ordemServicoId(), request.decisao())));
    }

    private void validarToken(String token) {
        if (token == null || tokenEsperado == null || tokenEsperado.isBlank()
                || !constantTimeEquals(token, tokenEsperado)) {
            throw new AutenticacaoWebhookException("Token de webhook ausente ou inválido");
        }
    }

    /** Comparação em tempo constante para não vazar o token por timing. */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
