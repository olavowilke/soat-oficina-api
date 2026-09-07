package br.com.oficina.cliente.infrastructure;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.entities.Documento;
import br.com.oficina.cliente.entities.TipoDocumento;

public class ClienteMapper {

    private ClienteMapper() {}

    public static ClienteData toData(Cliente cliente) {
        ClienteData data = new ClienteData();
        data.setId(cliente.getId());
        data.setNome(cliente.getNome());
        data.setEmail(cliente.getEmail());
        data.setTelefone(cliente.getTelefone());
        data.setDocumento(cliente.getDocumento().numero());
        data.setTipoDocumento(cliente.getDocumento().tipo().name());
        data.setAtivo(cliente.isAtivo());
        data.setCriadoEm(cliente.getCriadoEm());
        data.setAtualizadoEm(cliente.getAtualizadoEm());
        return data;
    }

    public static Cliente toDomain(ClienteData data) {
        TipoDocumento tipo = TipoDocumento.valueOf(data.getTipoDocumento());
        Documento documento = new Documento(data.getDocumento(), tipo);
        return Cliente.reconstituir(
                data.getId(),
                data.getNome(),
                data.getEmail(),
                data.getTelefone(),
                documento,
                data.isAtivo(),
                data.getCriadoEm(),
                data.getAtualizadoEm()
        );
    }
}
