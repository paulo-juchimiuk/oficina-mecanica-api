output "nome_do_cluster" {
  description = "Nome do cluster EKS criado"
  value       = aws_eks_cluster.oficina.name
}

output "regiao" {
  description = "Regiao onde o cluster foi criado"
  value       = var.regiao
}

output "comando_do_kubeconfig" {
  description = "Comando que aponta o kubectl para o cluster criado"
  value       = "aws eks update-kubeconfig --region ${var.regiao} --name ${aws_eks_cluster.oficina.name}"
}

output "namespace" {
  description = "Namespace onde a aplicacao e o banco sao publicados"
  value       = kubernetes_namespace.oficina.metadata[0].name
}

output "endereco_do_banco" {
  description = "Endereco do banco para a aplicacao publicada no mesmo namespace"
  value       = "${kubernetes_service.banco.metadata[0].name}:5432"
}
