resource "aws_eks_node_group" "oficina" {
  cluster_name    = aws_eks_cluster.oficina.name
  node_group_name = "${var.nome_do_cluster}-nos"
  node_role_arn   = aws_iam_role.nos.arn
  subnet_ids      = aws_subnet.publica[*].id
  instance_types  = [var.tipo_do_no]
  ami_type        = "AL2023_x86_64_STANDARD"
  disk_size       = 20

  scaling_config {
    desired_size = var.quantidade_de_nos
    min_size     = var.quantidade_de_nos
    max_size     = var.quantidade_de_nos
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.nos,
    aws_route_table_association.publica,
  ]
}
