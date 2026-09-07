package br.com.oficina.auth.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testes do {@link JwtAuthenticationFilter}: popular o {@link org.springframework.security.core.context.SecurityContext}
 * com a identidade do cliente (quando o token trouxer {@code clienteId}) e manter o
 * comportamento existente para tokens de operador (sem essa claim).
 */
class JwtAuthenticationFilterTest {

    private static final String SEGREDO =
            "ZmljYXAtb2ZpY2luYS1hcGktc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmch";

    private final JwtService jwtService = new JwtService(SEGREDO, 60_000L);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private String tokenDeCliente() {
        return Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", "3f1a...").claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();
    }

    private String tokenComClienteIdNaoString() {
        return Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", 12345).claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();
    }

    private String tokenComRoleNaoString() {
        return Jwts.builder()
                .subject("52998224725")
                .claim("role", 42)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();
    }

    private String tokenComSubjectNaoString() throws Exception {
        // O builder do JJWT valida o tipo de "sub" e rejeitaria um Long, então o
        // token é montado manualmente para simular um payload já existente nesse
        // formato — o parser aceita normalmente, pois a validação de tipo do
        // builder não se aplica à leitura.
        long agoraSegundos = System.currentTimeMillis() / 1000;
        String payload = "{\"sub\":52998224725,\"role\":\"CLIENTE\",\"iat\":" + agoraSegundos
                + ",\"exp\":" + (agoraSegundos + 60) + "}";
        return tokenAssinadoManualmente(payload);
    }

    private String tokenAssinadoManualmente(String payloadJson) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(payloadJson);
        String signingInput = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SEGREDO.getBytes(UTF_8), "HmacSHA256"));
        String assinatura = base64UrlBytes(mac.doFinal(signingInput.getBytes(UTF_8)));

        return signingInput + "." + assinatura;
    }

    private String base64Url(String json) {
        return base64UrlBytes(json.getBytes(UTF_8));
    }

    private String base64UrlBytes(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private MockHttpServletRequest requisicaoCom(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorizationHeader);
        return request;
    }

    @Test
    void devePopularSecurityContextComClienteIdQuandoTokenTemAClaim() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);

        filtro.doFilter(requisicaoCom("Bearer " + tokenDeCliente()), resposta, cadeia);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("52998224725");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENTE");
        assertThat(auth.getDetails()).isEqualTo("3f1a...");
    }

    @Test
    void deveManterComportamentoAtualParaTokenDeOperadorSemClienteId() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);
        String tokenOperador = jwtService.generateToken("admin", "ADMIN");

        filtro.doFilter(requisicaoCom("Bearer " + tokenOperador), resposta, cadeia);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("admin");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(auth.getDetails()).isNull();
    }

    @Test
    void deveContinuarSemQuebrarQuandoClienteIdNaoForString() throws Exception {
        // Regressão: clienteId não-string (ex.: número) lançava RequiredTypeException,
        // que escapava do filtro e virava 500 em vez de seguir a cadeia sem cliente.
        MockHttpServletRequest requisicao = requisicaoCom("Bearer " + tokenComClienteIdNaoString());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("52998224725");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENTE");
        assertThat(auth.getDetails()).isNull();
    }

    @Test
    void deveSeguirCadeiaSemAutenticarQuandoRoleNaoForString() throws Exception {
        // Regressão: role não-string (ex.: número) lançava RequiredTypeException,
        // que escapava do filtro e virava 500. Um token com role ilegível é um
        // token sem role utilizável — o filtro não deve autenticar com uma
        // authority derivada de lixo (nem montar "ROLE_null"); a requisição segue
        // a cadeia sem SecurityContext populado.
        MockHttpServletRequest requisicao = requisicaoCom("Bearer " + tokenComRoleNaoString());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveSeguirCadeiaSemAutenticarQuandoSubjectNaoForString() throws Exception {
        // Investigado como possível "terceira porta" do bug de role/clienteId. Confirmado
        // (ver JwtServiceTest.deveInvalidarTokenComSubjectNaoString) que "sub" é claim
        // registrada no JJWT e tem o tipo validado já no parse — um sub não-string derruba
        // isTokenValid, não extractUsername. Este teste já passava antes de qualquer
        // alteração deste round: documenta que o filtro segue a cadeia sem autenticar
        // através do gate isTokenValid já existente, não de uma guarda nova em extractUsername.
        MockHttpServletRequest requisicao = requisicaoCom("Bearer " + tokenComSubjectNaoString());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        FilterChain cadeia = mock(FilterChain.class);

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
