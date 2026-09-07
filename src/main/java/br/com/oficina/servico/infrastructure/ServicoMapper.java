package br.com.oficina.servico.infrastructure;

import br.com.oficina.servico.entities.Servico;

public class ServicoMapper {

    private ServicoMapper() {}

    public static ServicoData toData(Servico servico) {
        ServicoData data = new ServicoData();
        data.setId(servico.getId());
        data.setNome(servico.getNome());
        data.setDescricao(servico.getDescricao());
        data.setPreco(servico.getPreco());
        data.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
        data.setAtivo(servico.isAtivo());
        data.setCriadoEm(servico.getCriadoEm());
        data.setAtualizadoEm(servico.getAtualizadoEm());
        return data;
    }

    public static Servico toDomain(ServicoData data) {
        return Servico.reconstituir(
                data.getId(),
                data.getNome(),
                data.getDescricao(),
                data.getPreco(),
                data.getTempoEstimadoMinutos(),
                data.isAtivo(),
                data.getCriadoEm(),
                data.getAtualizadoEm()
        );
    }
}
