resource "aws_sqs_queue" "respostas_dlq" {
  name                      = "${local.fila_respostas}-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "respostas" {
  name                       = local.fila_respostas
  visibility_timeout_seconds = 60
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.respostas_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "respostas_dlq" {
  queue_url = aws_sqs_queue.respostas_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.respostas.arn]
  })
}
