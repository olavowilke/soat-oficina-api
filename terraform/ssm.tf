# Publicados para o manifesto do Deployment e outras stacks consumirem
# via `data`, sem acoplar remote states entre os repositorios (ADR-0005).
resource "aws_ssm_parameter" "fila_respostas_url" {
  name  = "${local.ssm_base}/queues/os-saga-replies/url"
  type  = "String"
  value = aws_sqs_queue.respostas.url
}

resource "aws_ssm_parameter" "fila_respostas_arn" {
  name  = "${local.ssm_base}/queues/os-saga-replies/arn"
  type  = "String"
  value = aws_sqs_queue.respostas.arn
}
