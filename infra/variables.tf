variable "regiao" {
  description = "Regiao da AWS onde a rede e o cluster sao criados"
  type        = string
  default     = "us-east-1"
}

variable "nome_do_cluster" {
  description = "Nome do cluster EKS, usado tambem como prefixo da rede e das roles"
  type        = string
  default     = "oficina"
}

variable "versao_do_kubernetes" {
  description = "Versao do Kubernetes do cluster, dentro do suporte padrao da AWS"
  type        = string
  default     = "1.35"
}

variable "tipo_do_no" {
  description = "Tipo de instancia EC2 dos nos do cluster"
  type        = string
  default     = "t3.medium"
}

variable "quantidade_de_nos" {
  description = "Numero fixo de nos do cluster, que nao escala sozinho"
  type        = number
  default     = 2
}

variable "cidr_da_vpc" {
  description = "Faixa de enderecos da VPC, dividida em uma sub-rede publica por zona"
  type        = string
  default     = "10.0.0.0/16"
}

variable "arn_do_administrador" {
  description = "ARN do usuario IAM que administra o cluster pelo Terraform e pelo kubectl"
  type        = string
}

variable "arn_da_pipeline" {
  description = "ARN da role que a pipeline assume por OIDC para provisionar e publicar"
  type        = string
}

variable "namespace" {
  description = "Namespace que isola os objetos da oficina dentro do cluster"
  type        = string
  default     = "oficina"
}

variable "banco_nome" {
  description = "Nome do banco de dados criado na inicializacao do PostgreSQL"
  type        = string
  default     = "oficina"
}

variable "banco_usuario" {
  description = "Usuario proprietario do banco de dados"
  type        = string
  default     = "oficina"
}

variable "banco_senha" {
  description = "Senha do usuario do banco, com valor padrao de avaliacao"
  type        = string
  default     = "oficina_local"
  sensitive   = true
}

variable "banco_tag_da_imagem" {
  description = "Tag da imagem oficial do PostgreSQL"
  type        = string
  default     = "18-alpine"
}

variable "banco_tamanho_do_volume" {
  description = "Tamanho do volume persistente reservado para os dados do banco"
  type        = string
  default     = "1Gi"
}
