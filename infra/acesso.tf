locals {
  administradores_do_cluster = {
    administrador = var.arn_do_administrador
    pipeline      = var.arn_da_pipeline
  }
}

resource "aws_eks_access_entry" "administrador" {
  for_each = local.administradores_do_cluster

  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = each.value
}

resource "aws_eks_access_policy_association" "administrador" {
  for_each = local.administradores_do_cluster

  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = aws_eks_access_entry.administrador[each.key].principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}

resource "time_sleep" "propagacao_do_acesso" {
  create_duration = "60s"

  depends_on = [aws_eks_access_policy_association.administrador]
}
