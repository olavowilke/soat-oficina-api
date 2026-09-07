package br.com.oficina.ordemservico.presenters;

public record TempoMedioExecucaoResponse(Double tempoMedioMinutos, String mensagem) {

    public static TempoMedioExecucaoResponse semDados() {
        return new TempoMedioExecucaoResponse(null, "Nenhuma OS finalizada com tempo registrado");
    }

    public static TempoMedioExecucaoResponse comValor(double minutos) {
        return new TempoMedioExecucaoResponse(minutos, null);
    }
}
