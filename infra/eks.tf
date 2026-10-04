resource "aws_eks_cluster" "oficina" {
  name     = var.nome_do_cluster
  version  = var.versao_do_kubernetes
  role_arn = aws_iam_role.cluster.arn

  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = false
  }

  upgrade_policy {
    support_type = "STANDARD"
  }

  vpc_config {
    subnet_ids = aws_subnet.publica[*].id
  }

  depends_on = [aws_iam_role_policy_attachment.cluster]
}
