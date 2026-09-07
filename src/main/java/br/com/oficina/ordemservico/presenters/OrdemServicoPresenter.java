package br.com.oficina.ordemservico.presenters;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Presenter do contexto de ordens de serviço: converte a saída dos Use Cases
 * ({@link OrdemServico}) no ViewModel completo {@link OrdemServicoResponse}.
 * Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class OrdemServicoPresenter {

    public OrdemServicoResponse present(OrdemServico os) {
        return new OrdemServicoResponse(
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId(),
                os.getStatus(),
                os.getObservacoes(),
                os.getItensServico().stream().map(this::present).toList(),
                os.getItensPeca().stream().map(this::present).toList(),
                os.calcularOrcamentoTotal(),
                os.getCriadoEm(),
                os.getAtualizadoEm(),
                os.getInicioExecucao(),
                os.getFimExecucao()
        );
    }

    public List<OrdemServicoResponse> present(List<OrdemServico> ordens) {
        return ordens.stream().map(this::present).toList();
    }

    public AbrirOrdemServicoResponse presentAbertura(OrdemServico os) {
        return new AbrirOrdemServicoResponse(os.getId(), os.getStatus(), os.calcularOrcamentoTotal());
    }

    public TempoMedioExecucaoResponse presentTempoMedio(OptionalDouble media) {
        return media.isPresent()
                ? TempoMedioExecucaoResponse.comValor(media.getAsDouble())
                : TempoMedioExecucaoResponse.semDados();
    }

    private ItemServicoResponse present(ItemServico item) {
        return new ItemServicoResponse(item.getId(), item.getServicoId(),
                item.getNomeServico(), item.getValorCobrado());
    }

    private ItemPecaResponse present(ItemPeca item) {
        return new ItemPecaResponse(item.getId(), item.getPecaId(), item.getNomePeca(),
                item.getQuantidade(), item.getValorUnitario(), item.getSubtotal());
    }
}
