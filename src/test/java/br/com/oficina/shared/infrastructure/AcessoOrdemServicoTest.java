package br.com.oficina.shared.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AcessoOrdemServico")
class AcessoOrdemServicoTest {

    private static final UUID DONO = UUID.randomUUID();
    private static final UUID OUTRO = UUID.randomUUID();

    private final AcessoOrdemServico acesso = new AcessoOrdemServico();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String role, UUID clienteId) {
        var auth = new UsernamePasswordAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(clienteId == null ? null : clienteId.toString());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("ADMIN acessa a ordem de qualquer cliente")
    void adminAcessaQualquerOrdem() {
        autenticar("ADMIN", null);

        assertThatCode(() -> acesso.exigirAcesso(DONO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MECANICO acessa a ordem de qualquer cliente")
    void mecanicoAcessaQualquerOrdem() {
        autenticar("MECANICO", null);

        assertThatCode(() -> acesso.exigirAcesso(DONO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CLIENTE acessa a propria ordem")
    void clienteAcessaPropriaOrdem() {
        autenticar("CLIENTE", DONO);

        assertThatCode(() -> acesso.exigirAcesso(DONO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CLIENTE nao acessa a ordem de outro cliente")
    void clienteNaoAcessaOrdemDeOutro() {
        autenticar("CLIENTE", OUTRO);

        assertThatThrownBy(() -> acesso.exigirAcesso(DONO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CLIENTE sem clienteId no token nao acessa nada")
    void clienteSemIdentidadeNaoAcessa() {
        autenticar("CLIENTE", null);

        assertThatThrownBy(() -> acesso.exigirAcesso(DONO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("requisicao sem autenticacao nao acessa nada")
    void semAutenticacaoNaoAcessa() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> acesso.exigirAcesso(DONO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ordem sem cliente definido nao e acessivel por cliente algum")
    void ordemSemClienteNaoEAcessivelPorCliente() {
        autenticar("CLIENTE", DONO);

        assertThatThrownBy(() -> acesso.exigirAcesso(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
