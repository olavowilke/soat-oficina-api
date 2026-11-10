package br.com.oficina.ordemservico.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OrdemServico {

    private UUID id;
    private UUID clienteId;
    private UUID veiculoId;
    private StatusOS status;
    private String observacoes;
    private List<ItemServico> itensServico;
    private List<ItemPeca> itensPeca;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime inicioExecucao;
    private LocalDateTime fimExecucao;

    private OrdemServico() {
        this.itensServico = new ArrayList<>();
        this.itensPeca = new ArrayList<>();
    }

    public static OrdemServico nova(UUID clienteId, UUID veiculoId, String observacoes) {
        if (clienteId == null) throw new IllegalArgumentException("ClienteId não pode ser nulo");
        if (veiculoId == null) throw new IllegalArgumentException("VeiculoId não pode ser nulo");
        OrdemServico os = new OrdemServico();
        os.id = UUID.randomUUID();
        os.clienteId = clienteId;
        os.veiculoId = veiculoId;
        os.status = StatusOS.RECEBIDA;
        os.observacoes = observacoes;
        os.criadoEm = LocalDateTime.now();
        os.atualizadoEm = LocalDateTime.now();
        return os;
    }

    public static OrdemServico reconstituir(UUID id, UUID clienteId, UUID veiculoId, StatusOS status,
                                            String observacoes, List<ItemServico> itensServico,
                                            List<ItemPeca> itensPeca, LocalDateTime criadoEm,
                                            LocalDateTime atualizadoEm, LocalDateTime inicioExecucao,
                                            LocalDateTime fimExecucao) {
        OrdemServico os = new OrdemServico();
        os.id = id;
        os.clienteId = clienteId;
        os.veiculoId = veiculoId;
        os.status = status;
        os.observacoes = observacoes;
        os.itensServico = new ArrayList<>(itensServico != null ? itensServico : List.of());
        os.itensPeca = new ArrayList<>(itensPeca != null ? itensPeca : List.of());
        os.criadoEm = criadoEm;
        os.atualizadoEm = atualizadoEm;
        os.inicioExecucao = inicioExecucao;
        os.fimExecucao = fimExecucao;
        return os;
    }

    public void adicionarServico(ItemServico item) {
        validarEdicaoPermitida();
        itensServico.add(item);
        atualizadoEm = LocalDateTime.now();
    }

    public void removerServico(UUID itemServicoId) {
        validarEdicaoPermitida();
        boolean removido = itensServico.removeIf(i -> i.getId().equals(itemServicoId));
        if (!removido) throw new IllegalArgumentException("Item de serviço não encontrado: " + itemServicoId);
        atualizadoEm = LocalDateTime.now();
    }

    public void adicionarPeca(ItemPeca item) {
        validarEdicaoPermitida();
        itensPeca.add(item);
        atualizadoEm = LocalDateTime.now();
    }

    public void removerPeca(UUID itemPecaId) {
        validarEdicaoPermitida();
        boolean removido = itensPeca.removeIf(i -> i.getId().equals(itemPecaId));
        if (!removido) throw new IllegalArgumentException("Item de peça não encontrado: " + itemPecaId);
        atualizadoEm = LocalDateTime.now();
    }

    public void avancarStatus(StatusOS novoStatus) {
        validarTransicao(novoStatus);
        this.status = novoStatus;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void enviarParaOrcamento() {
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new IllegalStateException("A OS precisa de pelo menos um item para gerar orçamento");
        }
        avancarStatus(StatusOS.AGUARDANDO_ORCAMENTO);
    }

    public void registrarOrcamentoGerado() {
        avancarStatus(StatusOS.AGUARDANDO_APROVACAO);
    }

    public void voltarParaDiagnostico() {
        avancarStatus(StatusOS.EM_DIAGNOSTICO);
    }

    public void registrarAprovacao() {
        avancarStatus(StatusOS.AGUARDANDO_PAGAMENTO);
    }

    public void registrarPagamentoConfirmado() {
        avancarStatus(StatusOS.AGUARDANDO_EXECUCAO);
    }

    public void iniciarExecucao(LocalDateTime inicio) {
        validarTransicao(StatusOS.EM_EXECUCAO);
        this.inicioExecucao = inicio;
        avancarStatus(StatusOS.EM_EXECUCAO);
    }

    public void finalizarExecucao(LocalDateTime fim) {
        validarTransicao(StatusOS.FINALIZADA);
        this.fimExecucao = fim;
        avancarStatus(StatusOS.FINALIZADA);
    }

    public void cancelar() {
        avancarStatus(StatusOS.CANCELADA);
    }

    private void validarTransicao(StatusOS novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new IllegalStateException(
                    "Transição inválida: " + status + " → " + novoStatus);
        }
    }

    public BigDecimal calcularOrcamentoTotal() {
        BigDecimal totalServicos = itensServico.stream()
                .map(ItemServico::getValorCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPecas = itensPeca.stream()
                .map(ItemPeca::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalServicos.add(totalPecas);
    }

    private void validarEdicaoPermitida() {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Não é possível editar uma OS com status " + status);
        }
    }

    public UUID getId() { return id; }
    public UUID getClienteId() { return clienteId; }
    public UUID getVeiculoId() { return veiculoId; }
    public StatusOS getStatus() { return status; }
    public String getObservacoes() { return observacoes; }
    public List<ItemServico> getItensServico() { return Collections.unmodifiableList(itensServico); }
    public List<ItemPeca> getItensPeca() { return Collections.unmodifiableList(itensPeca); }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public LocalDateTime getInicioExecucao() { return inicioExecucao; }
    public LocalDateTime getFimExecucao() { return fimExecucao; }
}
