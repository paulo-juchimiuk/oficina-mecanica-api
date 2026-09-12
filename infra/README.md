# Provisionamento da infraestrutura com Terraform

Este diretório cria do zero o ambiente onde a aplicação roda: um cluster Kubernetes local e o banco de dados dentro dele. A aplicação em si não é provisionada aqui: o Terraform entrega o cluster pronto e o banco de pé, e a publicação da aplicação acontece por manifestos de Kubernetes aplicados sobre esse ambiente.

## O que é criado

São seis recursos, um por bloco, distribuídos por arquivo:

| Recurso | Arquivo | O que é |
|---|---|---|
| `kind_cluster.oficina` | `cluster.tf` | O cluster Kubernetes, com um nó de controle e um nó de trabalho. A porta `30080` do nó de controle é publicada na porta `8080` de `127.0.0.1`, que é por onde a aplicação é acessada da máquina |
| `kubernetes_namespace.oficina` | `namespace.tf` | O namespace que isola tudo o que é da oficina dentro do cluster |
| `kubernetes_secret.banco` | `banco.tf` | Nome do banco, usuário e senha, consumidos pelo contêiner do PostgreSQL por `envFrom` |
| `kubernetes_persistent_volume_claim.banco` | `banco.tf` | O volume de 1 GiB onde os dados do banco ficam, na StorageClass `standard` do cluster |
| `kubernetes_deployment.banco` | `banco.tf` | O PostgreSQL, uma réplica, estratégia `Recreate`, com sondas de vivacidade e de prontidão por `pg_isready` |
| `kubernetes_service.banco` | `banco.tf` | O nome `banco` dentro do cluster, na porta 5432 |

As variáveis estão em `variables.tf`, todas com `description`, `type` e valor padrão, e as saídas em `outputs.tf`. O `terraform.tfvars.example` traz as que se costuma trocar, e não a lista inteira.

Os dois providers são fixados em versão exata em `providers.tf`, e o `.terraform.lock.hcl` que registra a escolha é versionado junto, com as somas de verificação de Linux, macOS Intel, macOS ARM e Windows, para que o `terraform init` funcione em qualquer uma das quatro sem regravar o arquivo. O Terraform em si tem piso de versão, não versão exata: o que muda o resultado do `apply` é o provider.

## Pré-requisitos

| Ferramenta | Versão | Por quê |
|---|---|---|
| Docker | 27 ou superior | os nós do cluster são contêineres |
| `kind` | **v0.31.0** | o provider embute a biblioteca desta versão, e o CLI é usado para conferir o cluster |
| Terraform | 1.16 ou superior | declarado em `required_version` |
| `kubectl` | compatível com a v1.35.0 do servidor | para conferir o cluster depois do apply |

Dois limites do núcleo precisam ser levantados, senão o segundo nó não sobe:

```bash
sudo tee /etc/sysctl.d/90-kind.conf <<'EOF'
fs.inotify.max_user_instances=512
fs.inotify.max_user_watches=524288
EOF
sudo sysctl --system
```

## Como aplicar

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan -out=plano.tfplan
terraform apply plano.tfplan
```

O `apply` leva cerca de um minuto e termina com as três saídas na tela. Copiar o `terraform.tfvars` é opcional, porque toda variável tem padrão: ele existe para trocar a senha do banco ou o nome do cluster sem editar o código.

Conferindo o que subiu:

```bash
kubectl get nodes
kubectl -n oficina get pvc,deployment,service
kubectl -n oficina rollout status deployment/banco
terraform output
```

## Como destruir

```bash
terraform destroy
```

O `destroy` apaga o cluster inteiro, e com ele o volume do banco. Nada sobrevive, de propósito: este ambiente é descartável.

## As saídas

| Saída | Para quê |
|---|---|
| `caminho_do_kubeconfig` | o arquivo de acesso que o provider escreve neste diretório |
| `namespace` | o namespace onde a aplicação é publicada |
| `endereco_do_banco` | o endereço que a aplicação usa para alcançar o banco dentro do cluster |

## As decisões deste diretório

**A versão do Kubernetes é ditada pelo provider.** O provider `tehcyx/kind` não chama o CLI do `kind`: ele embute a biblioteca. A imagem de nó dessa biblioteca é fixada em `cluster.tf` por digest, e é ela que define a versão do Kubernetes do cluster, hoje **v1.35.0**. Trocar a versão do Kubernetes é trocar o provider, não editar um número.

**O volume é criado sem esperar ligação.** A StorageClass padrão do cluster liga o volume apenas quando algum Pod o monta. Como o Pod que monta o volume é criado depois, o `apply` esperaria por algo que ele mesmo ainda não criou, então o recurso do volume declara `wait_until_bound = false` e quem prova que o banco subiu é o `rollout` do Deployment, que o `apply` acompanha até o fim.

**O banco é um Deployment de uma réplica, e não um StatefulSet.** O StatefulSet existe para dar identidade estável e volume próprio a cada réplica, e para ordenar a subida entre elas. Com uma réplica e estratégia `Recreate` não há identidade a distinguir, não há volume a multiplicar e não há eleição de líder: há um Pod por vez, montando sempre o mesmo volume. O que o StatefulSet resolveria não existe neste ambiente.

**Os objetos são recursos tipados, nunca manifestos genéricos.** Um recurso que aplica YAML genérico precisa alcançar a API do cluster já no `plan`, e o cluster nasce no mesmo `apply`: o primeiro `plan` de uma cópia limpa do repositório falharia. Com recurso tipado, o `plan` funciona sem cluster nenhum de pé.

**O estado fica local, no arquivo `terraform.tfstate` deste diretório, fora do controle de versão.** Estado remoto com trava é o que se usa quando várias pessoas aplicam a mesma infraestrutura, e ele vive em serviço de nuvem, que este ambiente não usa por decisão. O estado local é coerente com um cluster que roda na máquina de quem aplica.

**O `apply` mexe no `~/.kube/config`.** O provider acrescenta lá a entrada do cluster criado e a torna o contexto corrente, além de escrever o arquivo de acesso neste diretório. O `destroy` remove essa entrada e deixa o `kubectl` sem contexto corrente, sem apagar os demais contextos. Quem usa o `kubectl` com outros clusters volta para o seu com `kubectl config use-context <nome>`.

**A senha do banco é local e existe em texto no `terraform.tfvars.example`.** Ela vale só para este ambiente descartável, é a mesma do `docker-compose.yml`, e a variável é marcada como `sensitive`, de modo que nenhum `plan`, `apply` ou `output` a imprime. O `terraform.tfvars` de verdade fica fora do controle de versão.
