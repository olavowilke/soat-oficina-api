package br.com.oficina.shared.infrastructure;

import br.com.oficina.auth.infrastructure.JwtService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de fatia web (sem contêiner, sem Docker — só o slice do Spring MVC) para os matchers
 * de autorização de {@link SecurityConfig} (achado I2 da revisão final): tokens de cliente
 * (role {@code CLIENTE}, emitidos por {@code lambda-auth} a partir só do CPF — sem nenhum dos
 * papéis de {@link br.com.oficina.auth.entities.Role}) não podem alcançar rotas de operador
 * (aqui representadas por {@code /servicos/**}, catálogo de serviços), mas continuam
 * alcançando {@code /ordens-servico/**} — a única rota pensada para ser usada por clientes
 * autenticados. Não há checagem de posse (um cliente ainda pode ler/avançar a OS de outro
 * cliente) — fora do escopo desta correção, ver nota em {@code SecurityConfig}.
 */
@WebMvcTest(controllers = {ServicoController.class, OrdemServicoController.class})
@Import(SecurityConfig.class)
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
    @WithMockUser(roles = "CLIENTE")
    void tokenDeClienteAindaAlcancaOrdensDeServico() throws Exception {
        when(listarOrdensServico.execute(null, null)).thenReturn(List.of());
        when(ordemServicoPresenter.present(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico"))
                .andExpect(status().isOk());
    }
}
