package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessarDecisaoOrcamentoUseCaseTest {

    @Mock
    private OrdemServicoGateway gateway;

    @Mock
    private NotificadorStatusOrdem notificador;

    @InjectMocks
    private ProcessarDecisaoOrcamentoUseCase processar;

    private OrdemServico osAguardandoAprovacao() {
        OrdemServico os = OrdemServico.nova(UUID.randomUUID(), UUID.randomUUID(), null);
        os.avancarStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
        return os;
    }

    @Test
    void aprovadoDeveTransicionarParaEmExecucao() {
        OrdemServico os = osAguardandoAprovacao();
        when(gateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = processar.execute(os.getId(), DecisaoOrcamento.APROVADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        verify(gateway).save(any());
    }

    @Test
    void recusadoDeveTransicionarParaCancelada() {
        OrdemServico os = osAguardandoAprovacao();
        when(gateway.findById(os.getId())).thenReturn(Optional.of(os));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = processar.execute(os.getId(), DecisaoOrcamento.RECUSADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(gateway).save(any());
    }

    @Test
    void aprovadoDeveSerIdempotenteQuandoJaEmExecucao() {
        OrdemServico os = osAguardandoAprovacao();
        os.aprovarOrcamento(); // já EM_EXECUCAO
        when(gateway.findById(os.getId())).thenReturn(Optional.of(os));

        OrdemServico resultado = processar.execute(os.getId(), DecisaoOrcamento.APROVADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        verify(gateway, never()).save(any());
    }

    @Test
    void recusadoDeveSerIdempotenteQuandoJaCancelada() {
        OrdemServico os = osAguardandoAprovacao();
        os.recusarOrcamento(); // já CANCELADA
        when(gateway.findById(os.getId())).thenReturn(Optional.of(os));

        OrdemServico resultado = processar.execute(os.getId(), DecisaoOrcamento.RECUSADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(gateway, never()).save(any());
    }

    @Test
    void deveFalharQuandoEstadoInvalido() {
        OrdemServico os = OrdemServico.nova(UUID.randomUUID(), UUID.randomUUID(), null); // RECEBIDA
        when(gateway.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> processar.execute(os.getId(), DecisaoOrcamento.APROVADO))
                .isInstanceOf(IllegalStateException.class);
        verify(gateway, never()).save(any());
    }

    @Test
    void deveFalharQuandoOsNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(gateway.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processar.execute(id, DecisaoOrcamento.APROVADO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
