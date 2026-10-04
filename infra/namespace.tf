resource "kubernetes_namespace" "oficina" {
  metadata {
    name = var.namespace
  }

  depends_on = [time_sleep.propagacao_do_acesso]
}
