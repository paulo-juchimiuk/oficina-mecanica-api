locals {
  politicas_dos_nos = toset([
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPullOnly",
  ])
}

resource "aws_iam_role" "cluster" {
  name = "${var.nome_do_cluster}-cluster"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "eks.amazonaws.com" }
        Action    = ["sts:AssumeRole", "sts:TagSession"]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "cluster" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "nos" {
  name = "${var.nome_do_cluster}-nos"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "nos" {
  for_each = local.politicas_dos_nos

  role       = aws_iam_role.nos.name
  policy_arn = each.value
}

resource "aws_iam_role" "ebs_csi" {
  name = "${var.nome_do_cluster}-ebs-csi"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "pods.eks.amazonaws.com" }
        Action    = ["sts:AssumeRole", "sts:TagSession"]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ebs_csi" {
  role       = aws_iam_role.ebs_csi.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEBSCSIDriverPolicyV2"
}
