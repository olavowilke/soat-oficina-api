package br.com.oficina.shared.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Impede a aplicação de subir num ambiente real com os valores de exemplo que
 * estão publicados em {@code application.yml}.
 *
 * <p>Os quatro repositórios da Fase 3 são públicos. Qualquer pessoa lê o
 * {@code application.yml} e vê o segredo JWT, a senha do admin e o token do
 * webhook usados como padrão de desenvolvimento. Enquanto o ambiente real
 * definir as variáveis correspondentes, isso é inofensivo. Se alguém esquecer
 * uma delas, o ambiente sobe com uma credencial que está na internet:
 *
 * <ul>
 *     <li>segredo JWT publicado — o atacante assina o próprio token com a role
 *     ADMIN e passa pelo {@code JwtAuthenticationFilter};</li>
 *     <li>senha de admin publicada — {@code AdminInitializer} cria o usuário
 *     administrador com ela no primeiro start;</li>
 *     <li>token de webhook publicado — o atacante aprova ou recusa orçamento
 *     de qualquer ordem de serviço.</li>
 * </ul>
 *
 * <p>Este bean falha no construtor, então o contexto do Spring nem termina de
 * subir. É de propósito: um deploy que não sobe é melhor que um deploy aberto.
 * A checagem só vale fora de {@code dev} e {@code test} — nesses dois perfis
 * os valores de exemplo são o caminho normal de trabalho local.
 */
@Component
public class SegredosPadraoValidator {

    /** Perfis onde os valores de exemplo são legítimos. */
    private static final Set<String> PERFIS_LOCAIS = Set.of("dev", "test");

    static final String JWT_PADRAO =
            "ZmljYXAtb2ZpY2luYS1hcGktc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmch";
    static final String SENHA_ADMIN_PADRAO = "admin123";
    static final String WEBHOOK_PADRAO = "dev-webhook-token-change-me";

    public SegredosPadraoValidator(
            @Value("${spring.profiles.active:dev}") String perfilAtivo,
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.admin.password:}") String senhaAdmin,
            @Value("${app.webhook.orcamento.token:}") String tokenWebhook) {

        if (PERFIS_LOCAIS.contains(perfilAtivo.trim())) {
            return;
        }

        List<String> pendentes = new ArrayList<>();
        adicionarSePadrao(pendentes, "JWT_SECRET", jwtSecret, JWT_PADRAO);
        adicionarSePadrao(pendentes, "ADMIN_PASSWORD", senhaAdmin, SENHA_ADMIN_PADRAO);
        adicionarSePadrao(pendentes, "WEBHOOK_ORCAMENTO_TOKEN", tokenWebhook, WEBHOOK_PADRAO);

        if (!pendentes.isEmpty()) {
            throw new IllegalStateException(
                    "Perfil '" + perfilAtivo + "' esta usando valor de exemplo publicado no repositorio para: "
                            + String.join(", ", pendentes)
                            + ". Defina essas variaveis de ambiente com valores proprios antes de subir. "
                            + "Em Kubernetes elas vem do Secret 'oficina-secrets' "
                            + "(ver infra-k8s/.github/workflows/cd.yml).");
        }
    }

    // Valor em branco entra na lista junto com o padrão: os dois significam
    // "ninguém definiu a variável neste ambiente".
    private void adicionarSePadrao(List<String> pendentes, String variavel, String valor, String padrao) {
        if (valor == null || valor.isBlank() || valor.equals(padrao)) {
            pendentes.add(variavel);
        }
    }
}
