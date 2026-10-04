terraform {
  required_version = ">= 1.16.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.67.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "2.38.0"
    }
    time = {
      source  = "hashicorp/time"
      version = "0.14.2"
    }
  }
}

provider "aws" {
  region = var.regiao
}

provider "kubernetes" {
  host                   = aws_eks_cluster.oficina.endpoint
  cluster_ca_certificate = base64decode(aws_eks_cluster.oficina.certificate_authority[0].data)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", aws_eks_cluster.oficina.name, "--region", var.regiao]
  }
}
