package br.com.oficina.shared.infrastructure;

import br.com.oficina.auth.infrastructure.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas
                        .requestMatchers("/health", "/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Login é público; registro NÃO. POST /auth/register cria conta
                        // com Role.MECANICO (ver AuthController) — como rota pública, ele
                        // transformava qualquer requisição em um operador da oficina, com
                        // acesso a /clientes/**, /veiculos/**, /servicos/** e /pecas/**.
                        // Operador novo passa a ser criado por um ADMIN, ou pelo
                        // AdminInitializer no primeiro start.
                        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("ADMIN")
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        // Webhooks: autenticados por token compartilhado no próprio controller
                        .requestMatchers("/webhooks/**").permitAll()
                        // Catálogo de serviços e estoque de peças, e cadastro de clientes/veículos:
                        // rotas de operador (ADMIN/MECANICO, os únicos valores de Role - ver
                        // br.com.oficina.auth.entities.Role). Tokens de cliente (role "CLIENTE",
                        // emitidos por lambda-auth a partir só do CPF, sem esses papéis) não devem
                        // conseguir precificar o catálogo, mexer no estoque ou administrar
                        // cadastros de outros clientes/veículos.
                        .requestMatchers("/servicos/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/pecas/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/veiculos/**").hasAnyRole("ADMIN", "MECANICO")
                        // Ordens de serviço, divididas por quem opera a oficina e quem é dono
                        // da ordem. Antes tudo aqui caía em anyRequest().authenticated(): um
                        // token de CLIENTE (emitido por lambda-auth só com o CPF) lia e
                        // alterava a ordem de qualquer outro cliente, e drenava estoque de
                        // peça — AdicionarPecaUseCase chama peca.ajustarEstoque(-quantidade).
                        //
                        // Operação da oficina: só operador. A ordem destes matchers importa —
                        // os literais vêm antes de qualquer curinga de {id}.
                        .requestMatchers("/ordens-servico/abertura").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/ordens-servico/monitoramento/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/ordens-servico/*/servicos/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/ordens-servico/*/pecas/**").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers("/ordens-servico/*/status").hasAnyRole("ADMIN", "MECANICO")
                        .requestMatchers(HttpMethod.POST, "/ordens-servico").hasAnyRole("ADMIN", "MECANICO")
                        // Listagem sem filtro devolve a oficina inteira: só operador.
                        .requestMatchers(HttpMethod.GET, "/ordens-servico").hasAnyRole("ADMIN", "MECANICO")
                        // Rotas da própria ordem. O papel não decide nada aqui: todo cliente
                        // tem o mesmo papel CLIENTE. Quem decide é AcessoOrdemServico, dentro
                        // do controller, comparando o dono da ordem com a claim clienteId do
                        // token.
                        .requestMatchers("/ordens-servico/*/aprovar-orcamento",
                                "/ordens-servico/*/recusar-orcamento").authenticated()
                        .requestMatchers(HttpMethod.GET, "/ordens-servico/*").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Map.of("status", 401, "error", "Unauthorized",
                                            "message", "Autenticação necessária")));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Map.of("status", 403, "error", "Forbidden",
                                            "message", "Acesso negado")));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
