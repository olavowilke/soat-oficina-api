package br.com.oficina.peca.infrastructure;

import br.com.oficina.peca.entities.Peca;

public class PecaMapper {

    private PecaMapper() {}

    public static PecaData toData(Peca peca) {
        PecaData data = new PecaData();
        data.setId(peca.getId());
        data.setNome(peca.getNome());
        data.setDescricao(peca.getDescricao());
        data.setPrecoUnitario(peca.getPrecoUnitario());
        data.setQuantidadeEstoque(peca.getQuantidadeEstoque());
        data.setAtivo(peca.isAtivo());
        data.setCriadoEm(peca.getCriadoEm());
        data.setAtualizadoEm(peca.getAtualizadoEm());
        return data;
    }

    public static Peca toDomain(PecaData data) {
        return Peca.reconstituir(
                data.getId(),
                data.getNome(),
                data.getDescricao(),
                data.getPrecoUnitario(),
                data.getQuantidadeEstoque(),
                data.isAtivo(),
                data.getCriadoEm(),
                data.getAtualizadoEm()
        );
    }
}
