variable "nome_do_cluster" {
  description = "Nome do cluster kind, usado tambem no contexto do kubeconfig"
  type        = string
  default     = "oficina"
}

variable "namespace" {
  description = "Namespace que isola os objetos da oficina dentro do cluster"
  type        = string
  default     = "oficina"
}

variable "porta_do_no" {
  description = "Porta NodePort que a aplicacao publica dentro do cluster"
  type        = number
  default     = 30080
}

variable "porta_do_hospedeiro" {
  description = "Porta da maquina local mapeada para a porta NodePort da aplicacao"
  type        = number
  default     = 8080
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
  description = "Senha do usuario do banco, valida apenas neste ambiente local"
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
