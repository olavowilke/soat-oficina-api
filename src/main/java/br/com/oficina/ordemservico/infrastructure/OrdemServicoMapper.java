package br.com.oficina.ordemservico.infrastructure;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;

import java.util.List;

public class OrdemServicoMapper {

    private OrdemServicoMapper() {}

    public static OrdemServicoData toData(OrdemServico os) {
        OrdemServicoData data = new OrdemServicoData();
        data.setId(os.getId());
        data.setClienteId(os.getClienteId());
        data.setVeiculoId(os.getVeiculoId());
        data.setStatus(os.getStatus());
        data.setObservacoes(os.getObservacoes());
        data.setCriadoEm(os.getCriadoEm());
        data.setAtualizadoEm(os.getAtualizadoEm());
        data.setInicioExecucao(os.getInicioExecucao());
        data.setFimExecucao(os.getFimExecucao());

        data.getItensServico().clear();
        for (ItemServico item : os.getItensServico()) {
            ItemServicoData itemData = new ItemServicoData();
            itemData.setId(item.getId());
            itemData.setOrdemServico(data);
            itemData.setServicoId(item.getServicoId());
            itemData.setNomeServico(item.getNomeServico());
            itemData.setValorCobrado(item.getValorCobrado());
            data.getItensServico().add(itemData);
        }

        data.getItensPeca().clear();
        for (ItemPeca item : os.getItensPeca()) {
            ItemPecaData itemData = new ItemPecaData();
            itemData.setId(item.getId());
            itemData.setOrdemServico(data);
            itemData.setPecaId(item.getPecaId());
            itemData.setNomePeca(item.getNomePeca());
            itemData.setQuantidade(item.getQuantidade());
            itemData.setValorUnitario(item.getValorUnitario());
            data.getItensPeca().add(itemData);
        }

        return data;
    }

    public static OrdemServico toDomain(OrdemServicoData data) {
        List<ItemServico> itensServico = data.getItensServico().stream()
                .map(i -> ItemServico.reconstituir(i.getId(), i.getServicoId(), i.getNomeServico(), i.getValorCobrado()))
                .toList();

        List<ItemPeca> itensPeca = data.getItensPeca().stream()
                .map(i -> ItemPeca.reconstituir(i.getId(), i.getPecaId(), i.getNomePeca(), i.getQuantidade(), i.getValorUnitario()))
                .toList();

        return OrdemServico.reconstituir(
                data.getId(),
                data.getClienteId(),
                data.getVeiculoId(),
                data.getStatus(),
                data.getObservacoes(),
                itensServico,
                itensPeca,
                data.getCriadoEm(),
                data.getAtualizadoEm(),
                data.getInicioExecucao(),
                data.getFimExecucao()
        );
    }
}
