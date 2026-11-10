package br.com.oficina.shared.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SegredosPadraoValidator")
class SegredosPadraoValidatorTest {

    private static final String JWT_PADRAO =
            "ZmljYXAtb2ZpY2luYS1hcGktc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmch";
    private static final String SENHA_PADRAO = "admin123";
    private static final String WEBHOOK_PADRAO = "dev-webhook-token-change-me";

    private static final String JWT_PROPRIO = "um-segredo-proprio-com-pelo-menos-256-bits-de-tamanho!";
    private static final String SENHA_PROPRIA = "senha-propria-do-ambiente";
    private static final String WEBHOOK_PROPRIO = "token-proprio-do-ambiente";

    @Test
    @DisplayName("aceita os valores padrao no perfil dev")
    void aceitaPadraoEmDev() {
        assertThatCode(() -> new SegredosPadraoValidator("dev", JWT_PADRAO, SENHA_PADRAO, WEBHOOK_PADRAO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("aceita os valores padrao no perfil test")
    void aceitaPadraoEmTest() {
        assertThatCode(() -> new SegredosPadraoValidator("test", JWT_PADRAO, SENHA_PADRAO, WEBHOOK_PADRAO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recusa a subir em prod com o segredo JWT publicado no repositorio")
    void recusaJwtPadraoEmProd() {
        assertThatThrownBy(() -> new SegredosPadraoValidator("prod", JWT_PADRAO, SENHA_PROPRIA, WEBHOOK_PROPRIO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("recusa a subir em homolog com a senha de admin publicada no repositorio")
    void recusaSenhaAdminPadraoEmHomolog() {
        assertThatThrownBy(() -> new SegredosPadraoValidator("homolog", JWT_PROPRIO, SENHA_PADRAO, WEBHOOK_PROPRIO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("recusa a subir em prod com o token de webhook publicado no repositorio")
    void recusaWebhookPadraoEmProd() {
        assertThatThrownBy(() -> new SegredosPadraoValidator("prod", JWT_PROPRIO, SENHA_PROPRIA, WEBHOOK_PADRAO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEBHOOK_ORCAMENTO_TOKEN");
    }

    @Test
    @DisplayName("aponta todos os valores padrao de uma vez, nao so o primeiro")
    void listaTodosOsPendentes() {
        assertThatThrownBy(() -> new SegredosPadraoValidator("prod", JWT_PADRAO, SENHA_PADRAO, WEBHOOK_PADRAO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageContaining("WEBHOOK_ORCAMENTO_TOKEN");
    }

    @Test
    @DisplayName("recusa valor em branco fora de dev - variavel de ambiente esquecida")
    void recusaValorEmBrancoForaDeDev() {
        assertThatThrownBy(() -> new SegredosPadraoValidator("prod", "  ", SENHA_PROPRIA, WEBHOOK_PROPRIO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("aceita quando todos os valores sao proprios do ambiente")
    void aceitaValoresProprios() {
        assertThatCode(() -> new SegredosPadraoValidator("prod", JWT_PROPRIO, SENHA_PROPRIA, WEBHOOK_PROPRIO))
                .doesNotThrowAnyException();
    }
}
