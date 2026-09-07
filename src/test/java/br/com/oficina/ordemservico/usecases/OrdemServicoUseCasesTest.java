package br.com.oficina.ordemservico.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoUseCasesTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;
    @Mock
    private ServicoGateway servicoGateway;
    @Mock
    private PecaGateway pecaGateway;
    @Mock
    private ClienteGateway clienteGateway;
    @Mock
    private VeiculoGateway veiculoGateway;
    @Mock
    private NotificadorStatusOrdem notificador;

    private CriarOrdemServicoUseCase criar;
    private AbrirOrdemServicoUseCase abrir;
    private AdicionarServicoUseCase adicionarServico;
    private AdicionarPecaUseCase adicionarPeca;
    private AvancarStatusUseCase avancarStatus;
    private AprovarOrcamentoUseCase aprovarOrcamento;
    private RecusarOrcamentoUseCase recusarOrcamento;
    private BuscarOrdemServicoUseCase buscar;

    private UUID clienteId;
    private UUID veiculoId;

    @BeforeEach
    void setUp() {
        criar = new CriarOrdemServicoUseCase(ordemServicoGateway, clienteGateway, veiculoGateway);
        abrir = new AbrirOrdemServicoUseCase(ordemServicoGateway, servicoGateway, pecaGateway,
                clienteGateway, veiculoGateway);
        adicionarServico = new AdicionarServicoUseCase(ordemServicoGateway, servicoGateway);
        adicionarPeca = new AdicionarPecaUseCase(ordemServicoGateway, pecaGateway);
        avancarStatus = new AvancarStatusUseCase(ordemServicoGateway, notificador);
        aprovarOrcamento = new AprovarOrcamentoUseCase(ordemServicoGateway, notificador);
        recusarOrcamento = new RecusarOrcamentoUseCase(ordemServicoGateway, notificador);
        buscar = new BuscarOrdemServicoUseCase(ordemServicoGateway);
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
    }

    @Test
    void deveCriarOrdemServico() {
        CriarOrdemServicoCommand command = new CriarOrdemServicoCommand(clienteId, veiculoId, "Revisão");
        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.existsById(veiculoId)).thenReturn(true);
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico os = criar.execute(command);

        assertThat(os).isNotNull();
        assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.getClienteId()).isEqualTo(clienteId);
        verify(ordemServicoGateway).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarOSComClienteInexistente() {
        CriarOrdemServicoCommand command = new CriarOrdemServicoCommand(clienteId, veiculoId, "Revisão");
        when(clienteGateway.existsById(clienteId)).thenReturn(false);

        assertThatThrownBy(() -> criar.execute(command))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Cliente");
        verify(ordemServicoGateway, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarOSComVeiculoInexistente() {
        CriarOrdemServicoCommand command = new CriarOrdemServicoCommand(clienteId, veiculoId, "Revisão");
        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.existsById(veiculoId)).thenReturn(false);

        assertThatThrownBy(() -> criar.execute(command))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Veículo");
        verify(ordemServicoGateway, never()).save(any());
    }

    @Test
    void deveAbrirOrdemServicoConsolidadaComServicosEPecas() {
        Servico servico = Servico.reconstituir(UUID.randomUUID(), "Troca de óleo", null,
                new BigDecimal("150.00"), 60, true, null, null);
        Peca peca = Peca.reconstituir(UUID.randomUUID(), "Filtro", null,
                new BigDecimal("50.00"), 10, true, null, null);

        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.existsById(veiculoId)).thenReturn(true);
        when(servicoGateway.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(pecaGateway.findById(peca.getId())).thenReturn(Optional.of(peca));
        when(pecaGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                clienteId, veiculoId, "Revisão completa",
                List.of(servico.getId()),
                List.of(new ItemPecaInput(peca.getId(), 3)));

        OrdemServico os = abrir.execute(command);

        assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.getItensServico()).hasSize(1);
        assertThat(os.getItensPeca()).hasSize(1);
        // 150 (serviço) + 3 * 50 (peças) = 300
        assertThat(os.calcularOrcamentoTotal()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
        verify(ordemServicoGateway).save(any());
    }

    @Test
    void deveFalharAberturaQuandoEstoqueInsuficiente() {
        Peca peca = Peca.reconstituir(UUID.randomUUID(), "Filtro", null,
                new BigDecimal("50.00"), 2, true, null, null);

        when(clienteGateway.existsById(clienteId)).thenReturn(true);
        when(veiculoGateway.existsById(veiculoId)).thenReturn(true);
        when(pecaGateway.findById(peca.getId())).thenReturn(Optional.of(peca));

        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                clienteId, veiculoId, null, List.of(),
                List.of(new ItemPecaInput(peca.getId(), 5)));

        assertThatThrownBy(() -> abrir.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estoque insuficiente");
        verify(ordemServicoGateway, never()).save(any());
    }

    @Test
    void deveFalharAberturaComClienteInexistente() {
        when(clienteGateway.existsById(clienteId)).thenReturn(false);

        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                clienteId, veiculoId, null, List.of(), List.of());

        assertThatThrownBy(() -> abrir.execute(command))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Cliente");
        verify(ordemServicoGateway, never()).save(any());
    }

    @Test
    void deveAdicionarServico() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        Servico servico = Servico.reconstituir(UUID.randomUUID(), "Troca de óleo", null,
                new BigDecimal("150.00"), 60, true, null, null);

        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(servicoGateway.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdicionarServicoCommand command = new AdicionarServicoCommand(os.getId(), servico.getId());
        OrdemServico resultado = adicionarServico.execute(command);

        assertThat(resultado.getItensServico()).hasSize(1);
        assertThat(resultado.getItensServico().get(0).getNomeServico()).isEqualTo("Troca de óleo");
    }

    @Test
    void deveLancarExcecaoAoAdicionarServicoInexistente() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(servicoGateway.findById(any())).thenReturn(Optional.empty());

        AdicionarServicoCommand command = new AdicionarServicoCommand(os.getId(), UUID.randomUUID());
        assertThatThrownBy(() -> adicionarServico.execute(command))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAdicionarPecaEDescontarEstoque() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        Peca peca = Peca.reconstituir(UUID.randomUUID(), "Filtro", null,
                new BigDecimal("50.00"), 10, true, null, null);

        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(pecaGateway.findById(peca.getId())).thenReturn(Optional.of(peca));
        when(pecaGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdicionarPecaCommand command = new AdicionarPecaCommand(os.getId(), peca.getId(), 3);
        OrdemServico resultado = adicionarPeca.execute(command);

        assertThat(resultado.getItensPeca()).hasSize(1);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    void deveLancarExcecaoAoAdicionarPecaSemEstoque() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        Peca peca = Peca.reconstituir(UUID.randomUUID(), "Filtro", null,
                new BigDecimal("50.00"), 2, true, null, null);

        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(pecaGateway.findById(peca.getId())).thenReturn(Optional.of(peca));

        AdicionarPecaCommand command = new AdicionarPecaCommand(os.getId(), peca.getId(), 5);
        assertThatThrownBy(() -> adicionarPeca.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void deveBuscarOrdemServicoPorId() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));

        OrdemServico resultado = buscar.execute(os.getId());
        assertThat(resultado.getId()).isEqualTo(os.getId());
    }

    @Test
    void deveLancarExcecaoQuandoOsNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        when(ordemServicoGateway.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buscar.execute(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAvancarStatus() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = avancarStatus.execute(os.getId(), StatusOS.EM_DIAGNOSTICO);
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveAprovarOrcamento() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = aprovarOrcamento.execute(os.getId());
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void deveRecusarOrcamento() {
        OrdemServico os = OrdemServico.nova(clienteId, veiculoId, null);
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemServicoGateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(ordemServicoGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = recusarOrcamento.execute(os.getId());
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }
}
