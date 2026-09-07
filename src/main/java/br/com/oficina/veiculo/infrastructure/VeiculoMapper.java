package br.com.oficina.veiculo.infrastructure;

import br.com.oficina.veiculo.entities.Placa;
import br.com.oficina.veiculo.entities.Veiculo;

public class VeiculoMapper {

    private VeiculoMapper() {}

    public static VeiculoData toData(Veiculo veiculo) {
        VeiculoData data = new VeiculoData();
        data.setId(veiculo.getId());
        data.setClienteId(veiculo.getClienteId());
        data.setPlaca(veiculo.getPlaca().valor());
        data.setMarca(veiculo.getMarca());
        data.setModelo(veiculo.getModelo());
        data.setAno(veiculo.getAno());
        data.setCor(veiculo.getCor());
        data.setCriadoEm(veiculo.getCriadoEm());
        data.setAtualizadoEm(veiculo.getAtualizadoEm());
        return data;
    }

    public static Veiculo toDomain(VeiculoData data) {
        Placa placa = new Placa(data.getPlaca());
        return Veiculo.reconstituir(
                data.getId(),
                data.getClienteId(),
                placa,
                data.getMarca(),
                data.getModelo(),
                data.getAno(),
                data.getCor(),
                data.getCriadoEm(),
                data.getAtualizadoEm()
        );
    }
}
