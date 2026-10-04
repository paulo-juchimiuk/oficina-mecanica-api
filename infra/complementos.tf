resource "aws_eks_addon" "pod_identity" {
  cluster_name                = aws_eks_cluster.oficina.name
  addon_name                  = "eks-pod-identity-agent"
  resolve_conflicts_on_create = "OVERWRITE"

  depends_on = [aws_eks_node_group.oficina]
}

resource "aws_eks_addon" "ebs_csi" {
  cluster_name                = aws_eks_cluster.oficina.name
  addon_name                  = "aws-ebs-csi-driver"
  resolve_conflicts_on_create = "OVERWRITE"

  pod_identity_association {
    role_arn        = aws_iam_role.ebs_csi.arn
    service_account = "ebs-csi-controller-sa"
  }

  depends_on = [
    aws_eks_node_group.oficina,
    aws_eks_addon.pod_identity,
    aws_iam_role_policy_attachment.ebs_csi,
  ]
}

resource "time_sleep" "liberacao_do_volume" {
  destroy_duration = "90s"

  depends_on = [aws_eks_addon.ebs_csi]
}

resource "aws_eks_addon" "metrics_server" {
  cluster_name                = aws_eks_cluster.oficina.name
  addon_name                  = "metrics-server"
  resolve_conflicts_on_create = "OVERWRITE"

  depends_on = [aws_eks_node_group.oficina]
}
