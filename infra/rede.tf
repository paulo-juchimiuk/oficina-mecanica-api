locals {
  quantidade_de_zonas      = 2
  zonas_sem_suporte_do_eks = ["use1-az3"]
}

data "aws_availability_zones" "disponiveis" {
  state            = "available"
  exclude_zone_ids = local.zonas_sem_suporte_do_eks
}

resource "aws_vpc" "oficina" {
  cidr_block           = var.cidr_da_vpc
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = var.nome_do_cluster
  }
}

resource "aws_subnet" "publica" {
  count = local.quantidade_de_zonas

  vpc_id                  = aws_vpc.oficina.id
  cidr_block              = cidrsubnet(var.cidr_da_vpc, 8, count.index)
  availability_zone       = data.aws_availability_zones.disponiveis.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.nome_do_cluster}-publica-${data.aws_availability_zones.disponiveis.names[count.index]}"
  }
}

resource "aws_internet_gateway" "oficina" {
  vpc_id = aws_vpc.oficina.id

  tags = {
    Name = var.nome_do_cluster
  }
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.oficina.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.oficina.id
  }

  tags = {
    Name = "${var.nome_do_cluster}-publica"
  }
}

resource "aws_route_table_association" "publica" {
  count = local.quantidade_de_zonas

  subnet_id      = aws_subnet.publica[count.index].id
  route_table_id = aws_route_table.publica.id
}
