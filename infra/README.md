# Provisionamento da infraestrutura com Terraform

Este diretório cria do zero, na AWS, o ambiente onde a aplicação roda: a rede, um cluster Kubernetes gerenciado (Amazon EKS) com dois nós, os complementos de que o cluster precisa para guardar dados e medir consumo, e o banco de dados dentro dele. A aplicação em si não é provisionada aqui: o Terraform entrega o cluster pronto e o banco de pé, e a publicação da aplicação acontece por manifestos de Kubernetes aplicados sobre esse ambiente (ADR-028).

## O que é criado

São 32 recursos, em 26 blocos, um assunto por arquivo:

| Arquivo | Recursos | O que é |
|---|---|---|
| `rede.tf` | VPC, 2 sub-redes, internet gateway, route table, 2 associações | A rede do cluster: VPC `10.0.0.0/16` com resolução de nomes, uma sub-rede pública em cada uma de duas zonas da região, e a rota de saída para a internet |
| `iam.tf` | 3 roles, 5 anexações de política | As identidades que o cluster, os nós e o driver de volumes assumem na AWS, cada uma com políticas gerenciadas pela própria AWS |
| `eks.tf` | o cluster | O EKS `oficina`, Kubernetes 1.35 em suporte padrão, com acesso concedido por access entry |
| `nos.tf` | o node group | Dois nós `t3.medium`, em número fixo, com Amazon Linux 2023 |
| `acesso.tf` | 2 access entries, 2 associações, 1 espera | Quem administra o cluster: o usuário do administrador e a role da pipeline, os dois com `AmazonEKSClusterAdminPolicy`; e a espera para essa permissão valer dentro do cluster |
| `complementos.tf` | 3 complementos, 1 espera | O agente de Pod Identity, o driver de volumes EBS e o `metrics-server`; e a espera para o driver apagar o volume do banco na destruição |
| `armazenamento.tf` | StorageClass `gp3` | Volumes EBS do tipo `gp3`, criados pelo driver quando um Pod os monta |
| `namespace.tf` | namespace `oficina` | O namespace que isola tudo o que é da oficina dentro do cluster |
| `banco.tf` | Secret, PersistentVolumeClaim, Deployment e Service `banco` | O PostgreSQL, uma réplica, estratégia `Recreate`, com volume de 1 GiB, sondas por `pg_isready` e o nome `banco` na porta 5432 |

As variáveis estão em `variables.tf`, todas com `description` e `type`, e com valor padrão as que têm um valor razoável para qualquer conta; os dois ARNs não têm, porque são da conta de quem aplica. As saídas estão em `outputs.tf`. O `terraform.tfvars.example` traz os ARNs e as que se costuma trocar.

Os três providers (`hashicorp/aws`, `hashicorp/kubernetes` e `hashicorp/time`) são fixados em versão exata em `providers.tf`, e o `.terraform.lock.hcl` versionado traz as somas de verificação de Linux, nas arquiteturas Intel e ARM, de macOS Intel, de macOS ARM e de Windows, para que o `terraform init` funcione em qualquer uma delas sem regravar o arquivo. O Terraform em si tem piso de versão, não versão exata: o que muda o resultado do `apply` é o provider.

## Pré-requisitos

| Ferramenta | Versão | Por quê |
|---|---|---|
| Conta AWS | com permissão de administrador para quem aplica | o Terraform cria rede, IAM, EKS e instâncias |
| `aws` CLI | 2.x, testado com 2.37.9 | é a credencial do Terraform e do `kubectl`, que pede o token do cluster com `aws eks get-token` |
| Terraform | 1.16 ou superior | declarado em `required_version` |
| `kubectl` | um minor de distância do servidor 1.35, testado com 1.35.9 | para conferir o cluster depois do `apply` |

**Três coisas existem antes do primeiro `apply`**, criadas uma vez e fora do Terraform, porque o próprio Terraform depende delas:

1. **O bucket do estado**, com versionamento ligado. O nome é único no mundo, então leva o ID da conta:

   ```bash
   aws s3api create-bucket --bucket <nome-do-bucket>
   aws s3api put-bucket-versioning --bucket <nome-do-bucket> \
     --versioning-configuration Status=Enabled
   ```

2. **A role que a pipeline assume**, cujo ARN entra no `arn_da_pipeline`. A AWS recusa uma access entry para um principal que não existe. Como criá-la está na seção "Provisionamento com Terraform" do [README principal](../README.md).
3. **Cota de pelo menos 4 vCPUs sob demanda** na região (os dois `t3.medium`). Conta nova começa com 5, então basta conferir:

   ```bash
   aws service-quotas get-service-quota --service-code ec2 \
     --quota-code L-1216C47A --query Quota.Value
   ```

**Custo:** cerca de US$ 0,20 por hora com o cluster de pé (plano de controle US$ 0,10, os dois nós cerca de US$ 0,08, os IPs públicos e os discos completam). O ambiente é feito para subir e descer na mesma sessão de uso.

## Como aplicar

```bash
cd infra
cp backend.hcl.example backend.hcl
cp terraform.tfvars.example terraform.tfvars
terraform init -backend-config=backend.hcl
terraform fmt -check && terraform validate
terraform plan -out=plano.tfplan
terraform apply plano.tfplan
```

No `backend.hcl` vai o nome do bucket do estado; no `terraform.tfvars`, os dois ARNs. O `plan` mostra 32 recursos a criar. **O `apply` levou 12m40s na medição**, quase todo esperando a AWS: o cluster leva cerca de 9 minutos e o node group cerca de 2. Ele termina com o banco pronto e as saídas na tela.

O mesmo `apply`, sobre o mesmo estado, roda pela pipeline, pelo botão do workflow "Infraestrutura" (README principal, seção da pipeline). Os dois caminhos usam a trava do estado, então nunca rodam ao mesmo tempo.

## Como conferir

```bash
aws eks update-kubeconfig --region us-east-1 --name oficina
kubectl get nodes
kubectl -n kube-system get pods
kubectl -n oficina get pvc
kubectl -n oficina rollout status deployment/banco
terraform output
```

Dois nós `Ready`, os Pods de `kube-system` todos `Running` (entre eles `ebs-csi-controller`, `eks-pod-identity-agent` e `metrics-server`), o volume do banco `Bound` na classe `gp3`, e o banco de pé. O `update-kubeconfig` acrescenta o cluster ao `~/.kube/config` e o torna o contexto corrente; quem usa o `kubectl` com outros clusters volta para o seu com `kubectl config use-context <nome>`.

## Como destruir

```bash
terraform plan -destroy -out=destruicao.tfplan
terraform apply destruicao.tfplan
```

A destruição apaga tudo, inclusive o volume do banco. Nada sobrevive, de propósito: o ambiente é descartável. **Ela levou 11m38s na medição**, na ordem inversa da criação: primeiro o banco e o volume dele, depois os complementos e os nós, e só então a rota de saída, o cluster e a rede. A mesma destruição roda pelo botão do workflow "Infraestrutura".

**Conferência de que nada ficou cobrando**, depois de toda destruição. Cada linha devolve uma lista vazia; a VPC padrão da conta, o internet gateway dela e os security groups `default` são da conta e não aparecem nessas consultas:

```bash
aws eks list-clusters --query 'clusters'
aws ec2 describe-instances --filters Name=instance-state-name,Values=pending,running,stopping,stopped --query 'Reservations[].Instances[].InstanceId'
aws ec2 describe-volumes --query 'Volumes[].[VolumeId,State]'
aws ec2 describe-addresses --query 'Addresses[].PublicIp'
aws ec2 describe-nat-gateways --filter Name=state,Values=pending,available,deleting --query 'NatGateways[].NatGatewayId'
aws ec2 describe-network-interfaces --query 'NetworkInterfaces[].[NetworkInterfaceId,Status]'
aws elb describe-load-balancers --query 'LoadBalancerDescriptions[].LoadBalancerName'
aws elbv2 describe-load-balancers --query 'LoadBalancers[].LoadBalancerName'
aws ec2 describe-vpcs --query 'Vpcs[?IsDefault==`false`].VpcId'
aws logs describe-log-groups --log-group-name-prefix /aws/eks --query 'logGroups[].logGroupName'
aws iam list-roles --query "Roles[?starts_with(RoleName, 'oficina-')].RoleName"
```

A última lista pode trazer a role da pipeline, que é permanente e não é criada aqui; qualquer outra `oficina-*` é resto.

**Quando a destruição não resolve.** Três saídas, da mais comum para a mais rara:

- **Sobrou um volume EBS** (`available` na terceira consulta, com a etiqueta `ebs.csi.aws.com/cluster`): é o volume do banco que o driver não chegou a apagar. `aws ec2 delete-volume --volume-id <id>`.
- **A rede não sai** (`DependencyViolation` na VPC ou numa sub-rede): sobrou interface de rede ou security group. A sexta consulta, filtrada por `Name=vpc-id,Values=<vpc>`, mostra o que prende; `delete-network-interface` funciona na interface `available`, e a `in-use` pertence a uma instância que ainda existe.
- **O Terraform não alcança o cluster** (cluster apagado por fora, ou access entry removida): o estado guarda objetos do Kubernetes que só se apagam pela API do cluster. A saída é tirá-los do estado e destruir o resto:

  ```bash
  terraform state list | grep '^kubernetes_'
  terraform state rm <cada endereço listado>
  terraform plan -destroy -out=destruicao.tfplan
  terraform apply destruicao.tfplan
  ```

## As saídas

| Saída | Para quê |
|---|---|
| `nome_do_cluster` | o nome do cluster EKS criado |
| `regiao` | a região onde ele foi criado |
| `comando_do_kubeconfig` | a linha que aponta o `kubectl` para o cluster |
| `namespace` | o namespace onde a aplicação é publicada |
| `endereco_do_banco` | o endereço que a aplicação usa para alcançar o banco, válido de dentro do namespace |

## As decisões deste diretório

**A rede é pública e sem NAT.** Os nós ficam em sub-redes públicas e saem para a internet pelo internet gateway, para baixar as imagens e falar com a API do cluster. Sub-rede privada exigiria um NAT gateway, que cobra por hora e por volume trafegado e é mais um recurso a sobrar depois da destruição. Nenhuma porta dos nós é aberta para fora: a aplicação é alcançada por `kubectl port-forward`, que passa pela API do cluster.

**Duas zonas, e nunca a `use1-az3`.** O EKS exige sub-redes em pelo menos duas zonas, e não aceita a zona de ID `use1-az3` de `us-east-1`. O nome de zona que aponta para esse ID muda de conta para conta, então as zonas são escolhidas pela lista da região já sem ele.

**Dois nós, em número fixo.** Dois `t3.medium` somam 4 vCPUs, dentro da cota inicial de 5 de uma conta nova, e comportam a aplicação em até 4 réplicas, o banco e a caixa de e-mail. O número de réplicas da aplicação escala sozinho, pelo HPA; o número de nós, não.

**Quem cria o cluster não vira administrador sozinho.** O cluster usa o modo de acesso `API`, sem a permissão automática de quem o criou, e declara os dois administradores por access entry: o usuário de quem aplica pelo terminal e a role da pipeline. Assim os dois caminhos chegam ao cluster pelo mesmo mecanismo, e um não depende de ter sido o outro a criá-lo.

**O primeiro objeto do cluster espera 60 segundos pela permissão.** A associação da política de administrador é aceita pela AWS antes de valer dentro do cluster: na medição, o namespace pedido no mesmo segundo foi recusado com `forbidden`, e a StorageClass pedida minutos depois passou. A espera fica entre as associações e o primeiro objeto.

**A rota de saída vive mais que os nós.** Os nós falam com a API do cluster pelo endereço público dela, então dependem do internet gateway. Sem essa dependência declarada, a destruição apagaria a rota antes dos nós, e os nós ficariam sem a API no meio da destruição, com o Pod do banco preso e o volume sem quem o apague. O node group depende das associações da route table, e por isso a rota só sai depois dele.

**O driver de volumes espera 90 segundos antes de sair.** Depois que o volume do banco é liberado, o driver ainda precisa soltá-lo do nó e apagá-lo, o que levou 19 segundos na medição. Sem a espera, o complemento do driver seria removido logo em seguida e o volume ficaria na conta, cobrando. A espera só existe na destruição; na criação, ela não atrasa nada.

**O volume do banco vem do driver EBS, com identidade por Pod Identity.** O driver precisa de permissão na AWS para criar e apagar volumes, e a recebe por uma role própria, ligada só à conta de serviço dele, e não pela role dos nós, que todo Pod herdaria. O `metrics-server` entra pelo mesmo mecanismo de complemento, na versão que a AWS validou para a versão do cluster, e não precisa de permissão nenhuma.

**O volume é criado sem esperar ligação.** A StorageClass `gp3` liga o volume apenas quando algum Pod o monta, para criá-lo na mesma zona do nó que roda esse Pod. Como o Pod que monta o volume é criado depois, o `apply` esperaria por algo que ele mesmo ainda não criou, então o recurso do volume declara `wait_until_bound = false`, e quem prova que o banco subiu é o `rollout` do Deployment, que o `apply` acompanha até o fim.

**O banco é um Deployment de uma réplica, e não um StatefulSet.** O StatefulSet existe para dar identidade estável e volume próprio a cada réplica, e para ordenar a subida e a descida entre elas. Com uma réplica e estratégia `Recreate` não há identidade a distinguir, não há volume a multiplicar, não há eleição de líder e não há desligamento simultâneo: há um Pod por vez, montando sempre o mesmo volume, e ele recebe o sinal de parada sozinho. O que o StatefulSet resolveria não existe neste ambiente.

**Os objetos são recursos tipados, nunca manifestos genéricos.** Um recurso que aplica YAML genérico precisa alcançar a API do cluster já no `plan`, e o cluster nasce no mesmo `apply`: o primeiro `plan` de uma cópia limpa do repositório falharia. Com recurso tipado, o `plan` funciona sem cluster nenhum de pé. O provider do Kubernetes pede o token do cluster a cada uso, com `aws eks get-token`, porque um token fixo expira antes de um `apply` terminar.

**O estado fica num bucket S3, com trava nativa.** O terminal e a pipeline aplicam a mesma infraestrutura, então o estado precisa estar num lugar que os dois alcancem, e a trava impede que dois `apply` rodem ao mesmo tempo. A trava é o arquivo que o próprio backend S3 grava ao lado do estado (`use_lockfile`), sem tabela de banco de dados à parte. O nome do bucket não está no código: ele vem do `backend.hcl`, que fica fora do controle de versão.

**A saída do endereço do banco é o nome curto, e não o nome completo do Service.** Dentro do namespace, `banco` resolve no primeiro sufixo de busca da lista do Pod. O nome completo, `banco.oficina.svc.cluster.local`, tem quatro pontos e fica abaixo do `ndots:5` que o Kubernetes configura, então o resolvedor percorre a lista de busca antes de tentá-lo como nome absoluto, e cada resolução paga essa volta. O nome curto não tem esse caminho.

**A senha do banco é de avaliação e existe em texto no `terraform.tfvars.example`.** Ela vale só para este ambiente descartável, é a mesma do `docker-compose.yml`, e a variável é marcada como `sensitive`, de modo que nenhum `plan`, `apply` ou `output` a imprime. O `terraform.tfvars` de verdade fica fora do controle de versão, junto com o `backend.hcl`.
