data "aws_caller_identity" "current" {}

locals {
  prefixo  = "${var.project}-${var.environment}"
  ssm_base = "/${var.project}/${var.environment}"

  fila_respostas = "${local.prefixo}-os-saga-replies"

  # Filas de comando dos outros servicos. Nomes fixos (contrato da Fase 4):
  # a oficina-api so envia para elas, nunca as cria.
  arn_base_sqs = "arn:aws:sqs:${var.aws_region}:${data.aws_caller_identity.current.account_id}"
  filas_comando_arns = [
    "${local.arn_base_sqs}:${local.prefixo}-billing-commands",
    "${local.arn_base_sqs}:${local.prefixo}-execution-commands",
  ]
}
