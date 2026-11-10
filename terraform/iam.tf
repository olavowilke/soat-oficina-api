data "aws_ssm_parameter" "cluster_name" {
  name = "${local.ssm_base}/cluster/name"
}

# Pod Identity: o servico pods.eks.amazonaws.com assume a role em nome do pod.
data "aws_iam_policy_document" "confianca" {
  statement {
    actions = ["sts:AssumeRole", "sts:TagSession"]

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "oficina_api" {
  name               = "${local.prefixo}-oficina-api"
  assume_role_policy = data.aws_iam_policy_document.confianca.json
}

data "aws_iam_policy_document" "sqs" {
  statement {
    sid = "ConsumirRespostas"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
    ]
    resources = [aws_sqs_queue.respostas.arn]
  }

  statement {
    sid = "LerTamanhoDaDlq"
    actions = [
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
    ]
    resources = [aws_sqs_queue.respostas_dlq.arn]
  }

  statement {
    sid = "EnviarComandos"
    actions = [
      "sqs:SendMessage",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
    ]
    resources = local.filas_comando_arns
  }
}

resource "aws_iam_role_policy" "sqs" {
  name   = "sqs"
  role   = aws_iam_role.oficina_api.id
  policy = data.aws_iam_policy_document.sqs.json
}

resource "aws_eks_pod_identity_association" "oficina_api" {
  cluster_name    = data.aws_ssm_parameter.cluster_name.value
  namespace       = var.namespace
  service_account = var.service_account
  role_arn        = aws_iam_role.oficina_api.arn
}
