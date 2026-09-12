output "caminho_do_kubeconfig" {
  description = "Arquivo de acesso ao cluster criado, para uso do kubectl"
  value       = kind_cluster.oficina.kubeconfig_path
}

output "namespace" {
  description = "Namespace onde a aplicacao e o banco sao publicados"
  value       = kubernetes_namespace.oficina.metadata[0].name
}

output "endereco_do_banco" {
  description = "Endereco do banco para a aplicacao publicada no mesmo namespace"
  value       = "${kubernetes_service.banco.metadata[0].name}:5432"
}
