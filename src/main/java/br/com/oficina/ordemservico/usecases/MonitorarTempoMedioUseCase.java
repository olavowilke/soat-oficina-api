package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.OptionalDouble;

@Service
public class MonitorarTempoMedioUseCase {

    private final OrdemServicoGateway ordemServicoGateway;

    public MonitorarTempoMedioUseCase(OrdemServicoGateway ordemServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
    }

    @Transactional(readOnly = true)
    public OptionalDouble execute() {
        return ordemServicoGateway.tempoMedioExecucaoMinutos();
    }
}
