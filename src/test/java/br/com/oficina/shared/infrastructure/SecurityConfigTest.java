package br.com.oficina.shared.infrastructure;

import br.com.oficina.auth.controllers.AuthController;
import br.com.oficina.auth.infrastructure.JwtService;
import br.com.oficina.auth.presenters.AuthPresenter;
import br.com.oficina.auth.usecases.LoginUseCase;
import br.com.oficina.auth.usecases.RegistrarUsuarioUseCase;
import br.com.oficina.ordemservico.controllers.OrdemServicoController;
import br.com.oficina.ordemservico.presenters.OrdemServicoPresenter;
import br.com.oficina.ordemservico.presenters.StatusOSPresenter;
import br.com.oficina.ordemservico.usecases.AbrirOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.AdicionarPecaUseCase;
import br.com.oficina.ordemservico.usecases.AdicionarServicoUseCase;
import br.com.oficina.ordemservico.usecases.AprovarOrcamentoUseCase;
import br.com.oficina.ordemservico.usecases.AvancarStatusUseCase;
import br.com.oficina.ordemservico.usecases.BuscarOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.CriarOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.ListarOrdensServicoUseCase;
import br.com.oficina.ordemservico.usecases.MonitorarTempoMedioUseCase;
import br.com.oficina.ordemservico.usecases.RecusarOrcamentoUseCase;
import br.com.oficina.ordemservico.usecases.RemoverPecaUseCase;
import br.com.oficina.servico.controllers.ServicoController;
import br.com.oficina.servico.presenters.ServicoPresenter;
import br.com.oficina.servico.usecases.AtualizarServicoUseCase;
import br.com.oficina.servico.usecases.BuscarServicoUseCase;
import br.com.oficina.servico.usecases.CadastrarServicoUseCase;
import br.com.oficina.servico.usecases.ListarServicosUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de fatia web (sem contêiner, sem Docker — só o slice do Spring MVC) para os matchers
 * de autorização de {@link SecurityConfig}.
 *
 * <p>Tokens de cliente (role {@code CLIENTE}, emitidos por {@code lambda-auth} a partir só do
 * CPF — sem nenhum dos papéis de {@link br.com.oficina.auth.entities.Role}) não alcançam rota
 * de operador. A revisão de segurança da Fase 3 mostrou que {@code /ordens-servico/**} caía em
 * {@code anyRequest().authenticated()}: qualquer CPF válido lia e alterava a ordem de qualquer
 * outro cliente, e drenava estoque de peça pela rota de adicionar peça. Os testes abaixo fixam
 * a divisão nova: operação de oficina é de operador; o cliente só chega às rotas da própria
 * ordem, e lá a posse é verificada por {@link AcessoOrdemServico}.
 *
 * <p>Também fixam que {@code POST /auth/register} deixou de ser público — ele cria conta
 * MECANICO, então criava operador a partir de uma requisição não autenticada.
 */
@WebMvcTest(controllers = {ServicoController.class, OrdemServicoController.class, AuthController.class})
@Import({SecurityConfig.class, AcessoOrdemServico.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Dependências exigidas por SecurityConfig — nenhum desses caminhos é exercitado por estes
    // testes (não há requisição com cabeçalho Authorization: @WithMockUser popula o
    // SecurityContext antes da cadeia de filtros rodar).
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserDetailsService userDetailsService;

    // Dependencias de AuthController (rota sob teste: POST /auth/register)
    @MockBean
    private LoginUseCase loginUseCase;
    @MockBean
    private RegistrarUsuarioUseCase registrarUsuario;
    @MockBean
    private AuthPresenter authPresenter;

    // Dependências de ServicoController (rota de operador sob teste: catálogo de serviços)
    @MockBean
    private CadastrarServicoUseCase cadastrarServico;
    @MockBean
    private AtualizarServicoUseCase atualizarServico;
    @MockBean
    private BuscarServicoUseCase buscarServico;
    @MockBean
    private ListarServicosUseCase listarServicos;
    @MockBean
    private br.com.oficina.servico.usecases.RemoverServicoUseCase removerServicoCatalogo;
    @MockBean
    private ServicoPresenter servicoPresenter;

    // Dependências de OrdemServicoController (rota de cliente sob teste: /ordens-servico)
    @MockBean
    private CriarOrdemServicoUseCase criarOrdemServico;
    @MockBean
    private AbrirOrdemServicoUseCase abrirOrdemServico;
    @MockBean
    private AdicionarServicoUseCase adicionarServico;
    @MockBean
    private br.com.oficina.ordemservico.usecases.RemoverServicoUseCase removerServicoOs;
    @MockBean
    private AdicionarPecaUseCase adicionarPeca;
    @MockBean
    private RemoverPecaUseCase removerPeca;
    @MockBean
    private AvancarStatusUseCase avancarStatus;
    @MockBean
    private AprovarOrcamentoUseCase aprovarOrcamento;
    @MockBean
    private RecusarOrcamentoUseCase recusarOrcamento;
    @MockBean
    private BuscarOrdemServicoUseCase buscarOrdemServico;
    @MockBean
    private ListarOrdensServicoUseCase listarOrdensServico;
    @MockBean
    private MonitorarTempoMedioUseCase monitorarTempoMedio;
    @MockBean
    private OrdemServicoPresenter ordemServicoPresenter;
    @MockBean
    private StatusOSPresenter statusOSPresenter;

    @Test
    @WithMockUser(roles = "MECANICO")
    void tokenDeOperadorAlcancaRotaDeCatalogo() throws Exception {
        when(listarServicos.execute()).thenReturn(List.of());
        when(servicoPresenter.present(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/servicos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void tokenDeClienteNaoAlcancaRotaDeCatalogo() throws Exception {
        mockMvc.perform(get("/servicos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void operadorListaTodasAsOrdens() throws Exception {
        when(listarOrdensServico.execute(null, null)).thenReturn(List.of());
        when(ordemServicoPresenter.present(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico"))
                .andExpect(status().isOk());
    }

    // --- rotas de operacao da oficina: so operador ------------------------------

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoListaTodasAsOrdens() throws Exception {
        mockMvc.perform(get("/ordens-servico"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoCriaOrdem() throws Exception {
        mockMvc.perform(post("/ordens-servico").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoAbreOrdem() throws Exception {
        mockMvc.perform(post("/ordens-servico/abertura").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // Esta e a rota que drenava estoque: AdicionarPecaUseCase chama
    // peca.ajustarEstoque(-quantidade) e persiste.
    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoAdicionaPecaEmOrdem() throws Exception {
        mockMvc.perform(post("/ordens-servico/" + UUID.randomUUID() + "/pecas")
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoAdicionaServicoEmOrdem() throws Exception {
        mockMvc.perform(post("/ordens-servico/" + UUID.randomUUID() + "/servicos")
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoAvancaStatusDaOrdem() throws Exception {
        mockMvc.perform(patch("/ordens-servico/" + UUID.randomUUID() + "/status")
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoVeMonitoramento() throws Exception {
        mockMvc.perform(get("/ordens-servico/monitoramento/tempo-medio-execucao"))
                .andExpect(status().isForbidden());
    }

    // --- rotas do proprio cliente: matcher libera, posse decide ------------------

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteAlcancaOMatcherDaPropriaOrdem() throws Exception {
        // O matcher nao pode barrar: quem decide aqui e AcessoOrdemServico, dentro
        // do controller. 403 neste ponto significaria matcher errado.
        mockMvc.perform(get("/ordens-servico/" + UUID.randomUUID()))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteAlcancaOMatcherDaAprovacaoDeOrcamento() throws Exception {
        mockMvc.perform(post("/ordens-servico/" + UUID.randomUUID() + "/aprovar-orcamento"))
                .andExpect(status().is(not(403)));
    }

    // --- criacao de operador deixou de ser publica ------------------------------

    @Test
    void registroDeOperadorNaoEPublico() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"segredo123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoRegistraOperador() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"segredo123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoRegistraOperador() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"segredo123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginContinuaPublico() throws Exception {
        // Nao autenticado: o matcher deixa passar. O resultado depende do
        // AuthenticationManager mockado, mas nunca pode ser 401/403 de matcher.
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().is(not(401)));
    }
}
