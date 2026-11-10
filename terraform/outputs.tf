output "fila_respostas_url" {
  description = "URL da fila de respostas da saga"
  value       = aws_sqs_queue.respostas.url
}

output "fila_respostas_arn" {
  description = "ARN da fila de respostas da saga"
  value       = aws_sqs_queue.respostas.arn
}

output "fila_respostas_dlq_arn" {
  description = "ARN da DLQ da fila de respostas"
  value       = aws_sqs_queue.respostas_dlq.arn
}

output "role_arn" {
  description = "ARN da role assumida pelo pod da oficina-api"
  value       = aws_iam_role.oficina_api.arn
}
