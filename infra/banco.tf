resource "kubernetes_secret" "banco" {
  metadata {
    name      = "banco"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  type = "Opaque"

  data = {
    POSTGRES_DB       = var.banco_nome
    POSTGRES_USER     = var.banco_usuario
    POSTGRES_PASSWORD = var.banco_senha
  }
}

resource "kubernetes_persistent_volume_claim" "banco" {
  metadata {
    name      = "banco"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  wait_until_bound = false

  spec {
    access_modes = ["ReadWriteOnce"]

    resources {
      requests = {
        storage = var.banco_tamanho_do_volume
      }
    }
  }
}

resource "kubernetes_deployment" "banco" {
  metadata {
    name      = "banco"
    namespace = kubernetes_namespace.oficina.metadata[0].name

    labels = {
      app = "banco"
    }
  }

  spec {
    replicas = 1

    strategy {
      type = "Recreate"
    }

    selector {
      match_labels = {
        app = "banco"
      }
    }

    template {
      metadata {
        labels = {
          app = "banco"
        }
      }

      spec {
        container {
          name  = "banco"
          image = "postgres:${var.banco_tag_da_imagem}"

          env_from {
            secret_ref {
              name = kubernetes_secret.banco.metadata[0].name
            }
          }

          env {
            name  = "TZ"
            value = "UTC"
          }

          port {
            name           = "postgres"
            container_port = 5432
          }

          volume_mount {
            name       = "dados"
            mount_path = "/var/lib/postgresql"
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.banco_usuario, "-d", var.banco_nome]
            }

            initial_delay_seconds = 5
            period_seconds        = 5
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", var.banco_usuario, "-d", var.banco_nome]
            }

            initial_delay_seconds = 30
            period_seconds        = 10
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "256Mi"
            }

            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }

        volume {
          name = "dados"

          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.banco.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "banco" {
  metadata {
    name      = "banco"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  spec {
    type = "ClusterIP"

    selector = {
      app = "banco"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = "postgres"
    }
  }
}
