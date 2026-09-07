package br.com.oficina.auth.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do {@link JwtService}: emissão/validação dos tokens de operador
 * (fluxo existente de {@code /auth/login}) e aceitação dos tokens emitidos
 * pela Lambda de autenticação de clientes (mesmo segredo, claims adicionais).
 */
class JwtServiceTest {

    private static final String SEGREDO =
            "ZmljYXAtb2ZpY2luYS1hcGktc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmch";

    private final JwtService jwtService = new JwtService(SEGREDO, 60_000L);

    @Test
    void deveAceitarTokenEmitidoPelaLambdaComClienteId() {
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", "3f1a...").claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("52998224725");
        assertThat(jwtService.extractClienteId(token)).contains("3f1a...");
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        String segredoDiferente = "b3V0cm8tc2VncmVkby1jb20tdGFtYW5oby1zdWZpY2llbnRlLXBhcmEtaHMyNTY";
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", "3f1a...").claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(segredoDiferente.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void deveManterCompatibilidadeComTokenDeOperadorSemClienteId() {
        // Tokens emitidos por /auth/login para admin/atendente não têm a claim
        // clienteId, nem iss/aud — precisam continuar válidos e sem cliente associado.
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractClienteId(token)).isEmpty();
    }

    @Test
    void deveRetornarVazioQuandoClienteIdNaoForString() {
        // Uma claim clienteId que não seja string (ex.: número) é uma identidade
        // de cliente inutilizável — o contrato é o mesmo de claim ausente:
        // Optional.empty(), sem lançar exceção nem invalidar o token.
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", 12345).claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractClienteId(token)).isEmpty();
    }

    @Test
    void deveRetornarVazioQuandoClienteIdForStringEmBranco() {
        // Uma claim clienteId presente mas em branco ("") não é uma identidade
        // utilizável — mesmo contrato de claim ausente: Optional.empty().
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("clienteId", "").claim("nome", "Ana").claim("role", "CLIENTE")
                .issuer("oficina-auth").audience().add("oficina-api").and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractClienteId(token)).isEmpty();
    }

    @Test
    void deveRetornarNuloQuandoRoleNaoForString() {
        // Mesmo bug do clienteId, um método acima: role com tipo incompatível
        // (ex.: número) não pode propagar RequiredTypeException — um token com
        // role ilegível é um token sem role utilizável.
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("role", 42)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractRole(token)).isNull();
    }

    @Test
    void deveRetornarNuloQuandoRoleForStringEmBranco() {
        // Mesmo tratamento já aplicado a clienteId em branco: role: "" produziria
        // a authority "ROLE_" — uma authority real que não corresponde a nada e
        // lê como autenticado. Deve ser tratado como role ausente: null.
        String token = Jwts.builder()
                .subject("52998224725")
                .claim("role", "")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractRole(token)).isNull();
    }

    @Test
    void deveInvalidarTokenComSubjectNaoString() throws Exception {
        // Investigado como possível "terceira porta" do bug de extractRole/extractClienteId
        // (Claims.getSubject() é declarado String na interface, mas a implementação faz um
        // checkcast direto). Na prática, isso não é alcançável: diferente de "role" e
        // "clienteId" (claims arbitrárias, sem validação até o get(key, Class) ser chamado),
        // "sub" é uma claim registrada pelo JJWT (Claims.SUBJECT) e tem o tipo validado já
        // na etapa de parse — um valor não-string falha o parseSignedClaims inteiro com
        // MalformedJwtException, antes de existir qualquer Claims para o checkcast rodar.
        // Confirmado via bytecode/execução real (jjwt-impl 0.12.3): DefaultJwtParser.parse
        // rejeita o token completo. isTokenValid (que já captura Exception amplamente)
        // trata isso como token inválido — extractUsername nunca chega a ser chamado pelo
        // filtro para um token assim. O builder do JJWT nem deixa construir esse payload
        // (valida "sub" na escrita também), por isso o token é montado manualmente aqui.
        long agoraSegundos = System.currentTimeMillis() / 1000;
        String payload = "{\"sub\":52998224725,\"role\":\"CLIENTE\",\"iat\":" + agoraSegundos
                + ",\"exp\":" + (agoraSegundos + 60) + "}";
        String token = tokenAssinadoManualmente(payload);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    /**
     * Monta e assina um JWT HS256 manualmente, fora do {@code Jwts.builder()}, cujas
     * validações de tipo por claim (ex.: {@code sub} deve ser String) impediriam
     * construir os payloads malformados que estes testes de regressão precisam.
     */
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
}
