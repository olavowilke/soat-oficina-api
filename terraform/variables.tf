variable "project" {
  description = "Prefixo de nomes dos recursos"
  type        = string
  default     = "oficina"
}

variable "environment" {
  description = "Ambiente logico"
  type        = string

  validation {
    condition     = contains(["homolog"], var.environment)
    error_message = "environment deve ser homolog."
  }
}

variable "aws_region" {
  description = "Regiao AWS"
  type        = string
  default     = "us-east-1"
}

variable "namespace" {
  description = "Namespace Kubernetes da aplicacao"
  type        = string
  default     = "oficina"
}

variable "service_account" {
  description = "ServiceAccount Kubernetes usada pelo pod da oficina-api"
  type        = string
  default     = "oficina-api"
}
