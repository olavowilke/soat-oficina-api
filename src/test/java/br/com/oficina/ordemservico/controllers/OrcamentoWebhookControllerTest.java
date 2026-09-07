package br.com.oficina.ordemservico.controllers;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.presenters.OrdemServicoPresenter;
import br.com.oficina.ordemservico.usecases.DecisaoOrcamento;
import br.com.oficina.ordemservico.usecases.ProcessarDecisaoOrcamentoUseCase;
import br.com.oficina.shared.domain.AutenticacaoWebhookException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrcamentoWebhookControllerTest {

    private static final String TOKEN = "segredo-123";

    @Mock
    private ProcessarDecisaoOrcamentoUseCase processarDecisao;
    @Mock
    private OrdemServicoPresenter presenter;

    private OrcamentoWebhookController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new OrcamentoWebhookController(processarDecisao, presenter, TOKEN);
    }

    @Test
    void deveRejeitarTokenAusente() {
        DecisaoOrcamentoWebhookRequest req =
                new DecisaoOrcamentoWebhookRequest(UUID.randomUUID(), DecisaoOrcamento.APROVADO);

        assertThatThrownBy(() -> controller.receberDecisao(null, req))
                .isInstanceOf(AutenticacaoWebhookException.class);
        verify(processarDecisao, never()).execute(any(), any());
    }

    @Test
    void deveRejeitarTokenInvalido() {
        DecisaoOrcamentoWebhookRequest req =
                new DecisaoOrcamentoWebhookRequest(UUID.randomUUID(), DecisaoOrcamento.APROVADO);

        assertThatThrownBy(() -> controller.receberDecisao("errado", req))
                .isInstanceOf(AutenticacaoWebhookException.class);
        verify(processarDecisao, never()).execute(any(), any());
    }

    @Test
    void deveProcessarComTokenValido() {
        UUID osId = UUID.randomUUID();
        DecisaoOrcamentoWebhookRequest req =
                new DecisaoOrcamentoWebhookRequest(osId, DecisaoOrcamento.APROVADO);
        OrdemServico os = OrdemServico.nova(UUID.randomUUID(), UUID.randomUUID(), null);
        when(processarDecisao.execute(osId, DecisaoOrcamento.APROVADO)).thenReturn(os);

        controller.receberDecisao(TOKEN, req);

        verify(processarDecisao).execute(eq(osId), eq(DecisaoOrcamento.APROVADO));
    }
}
