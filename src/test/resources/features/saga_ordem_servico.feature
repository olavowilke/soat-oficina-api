# language: pt
Funcionalidade: Saga da ordem de serviço

  Contexto:
    Dado uma peça "Filtro de óleo" com 10 unidades em estoque
    E uma OS em diagnóstico com o serviço "Troca de óleo" e 2 unidades de "Filtro de óleo"

  Cenário: Fluxo completo até a finalização
    Quando o mecânico envia o orçamento
    Então o Billing recebe o comando "GerarOrcamento" com 2 itens
    Quando o Billing responde "OrcamentoGerado"
    Então a OS fica em "AGUARDANDO_APROVACAO"
    Quando o Billing responde "OrcamentoAprovado"
    Então a OS fica em "AGUARDANDO_PAGAMENTO"
    Quando o Billing responde "PagamentoConfirmado"
    Então a Execução recebe o comando "IniciarExecucao"
    Quando a Execução responde "ExecucaoAceita"
    Então a OS fica em "EM_EXECUCAO"
    Quando a Execução responde "ExecucaoFinalizada"
    Então a OS fica em "FINALIZADA"
    E a saga fica em "CONCLUIDA"
    E o estoque de "Filtro de óleo" é 8

  Cenário: Cliente recusa o orçamento
    Quando o mecânico envia o orçamento
    E o Billing responde "OrcamentoGerado"
    E o Billing responde "OrcamentoRecusado"
    Então a OS fica em "CANCELADA"
    E a saga fica em "COMPENSADA"
    E o estoque de "Filtro de óleo" é 10

  Cenário: Execução rejeita e o pagamento é estornado
    Quando o mecânico envia o orçamento
    E o Billing responde "OrcamentoGerado"
    E o Billing responde "OrcamentoAprovado"
    E o Billing responde "PagamentoConfirmado"
    E a Execução responde "ExecucaoRejeitada"
    Então o Billing recebe o comando "EstornarPagamento"
    Quando o Billing responde "PagamentoEstornado"
    Então a OS fica em "CANCELADA"
    E a saga fica em "COMPENSADA"
    E o estoque de "Filtro de óleo" é 10

  Cenário: Pagamento expira
    Quando o mecânico envia o orçamento
    E o Billing responde "OrcamentoGerado"
    E o Billing responde "OrcamentoAprovado"
    E o Billing responde "PagamentoFalhou" com motivo "EXPIRADO"
    Então a OS fica em "CANCELADA"
    E o estoque de "Filtro de óleo" é 10
