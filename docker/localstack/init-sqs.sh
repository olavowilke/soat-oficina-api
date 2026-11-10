#!/bin/bash
# Cria as filas SQS do ambiente local, cada uma com DLQ (redrive após 3 recebimentos).
# O LocalStack roda este script quando fica pronto (/etc/localstack/init/ready.d).
set -euo pipefail

for fila in oficina-homolog-os-saga-replies oficina-homolog-billing-commands oficina-homolog-execution-commands; do
  awslocal sqs create-queue --queue-name "${fila}-dlq" > /dev/null
  dlq_url=$(awslocal sqs get-queue-url --queue-name "${fila}-dlq" --query QueueUrl --output text)
  dlq_arn=$(awslocal sqs get-queue-attributes --queue-url "${dlq_url}" \
    --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
  awslocal sqs create-queue --queue-name "${fila}" --attributes \
    "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${dlq_arn}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" > /dev/null
  echo "fila ${fila} criada com DLQ"
done
