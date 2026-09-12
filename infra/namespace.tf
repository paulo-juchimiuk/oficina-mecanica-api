resource "kubernetes_namespace" "oficina" {
  metadata {
    name = var.namespace
  }
}
