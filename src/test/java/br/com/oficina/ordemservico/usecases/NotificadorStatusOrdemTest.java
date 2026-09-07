package br.com.oficina.ordemservico.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.NotificacaoGateway;
import br.com.oficina.ordemservico.gateways.NotificacaoStatusOrdem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificadorStatusOrdemTest {

    @Mock
    private ClienteGateway clienteGateway;
    @Mock
    private NotificacaoGateway notificacaoGateway;

    @InjectMocks
    private NotificadorStatusOrdem notificador;

    private OrdemServico osComStatus(StatusOS status, UUID clienteId) {
        return OrdemServico.reconstituir(UUID.randomUUID(), clienteId, UUID.randomUUID(),
                status, null, List.of(), List.of(), null, null, null, null);
    }

    @Test
    void deveNotificarEmTransicaoRelevante() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "maria@ex.com", "119", null,
                true, null, null);
        OrdemServico os = osComStatus(StatusOS.EM_EXECUCAO, clienteId);
        when(clienteGateway.findById(clienteId)).thenReturn(Optional.of(cliente));

        notificador.notificar(os);

        ArgumentCaptor<NotificacaoStatusOrdem> captor = ArgumentCaptor.forClass(NotificacaoStatusOrdem.class);
        verify(notificacaoGateway).notificarAtualizacaoStatus(captor.capture());
        assertThat(captor.getValue().destinatarioEmail()).isEqualTo("maria@ex.com");
        assertThat(captor.getValue().novoStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void naoDeveNotificarEmStatusNaoRelevante() {
        OrdemServico os = osComStatus(StatusOS.RECEBIDA, UUID.randomUUID());

        notificador.notificar(os);

        verify(notificacaoGateway, never()).notificarAtualizacaoStatus(any());
    }

    @Test
    void naoDeveNotificarQuandoClienteSemEmail() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = Cliente.reconstituir(clienteId, "Sem Email", null, "119", null,
                true, null, null);
        OrdemServico os = osComStatus(StatusOS.FINALIZADA, clienteId);
        when(clienteGateway.findById(clienteId)).thenReturn(Optional.of(cliente));

        notificador.notificar(os);

        verify(notificacaoGateway, never()).notificarAtualizacaoStatus(any());
    }

    @Test
    void naoDevePropagarFalhaDeEnvio() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "maria@ex.com", "119", null,
                true, null, null);
        OrdemServico os = osComStatus(StatusOS.FINALIZADA, clienteId);
        when(clienteGateway.findById(clienteId)).thenReturn(Optional.of(cliente));
        doThrow(new RuntimeException("SMTP down")).when(notificacaoGateway).notificarAtualizacaoStatus(any());

        assertThatCode(() -> notificador.notificar(os)).doesNotThrowAnyException();
    }
}
