# oficina-mecanica-api

API de gestão para oficina mecânica de médio porte: ordem de serviço, orçamento com aprovação do cliente, controle de estoque e acompanhamento do atendimento. Back-end em **Clean Architecture**, com o domínio modelado por DDD.

Tech Challenge da pós-graduação em Arquitetura de Software (FIAP). A **Fase 1** entregou o domínio, a API e o ambiente local. A **Fase 2** evolui esta mesma aplicação: a arquitetura passa a ser Clean Architecture com a regra de dependência provada no build (ADR-025), e o atendimento ganha o pedido inicial do cliente na abertura, a fila de atendimento com exclusão lógica das encerradas, a notificação externa de aprovação do orçamento e o aviso por e-mail a cada mudança de status (ADR-026).

## Objetivos

A oficina atende, diagnostica, executa e entrega usando anotação manual e planilha, e é dessa forma de trabalho que nascem os cinco problemas que este sistema existe para resolver: erro na priorização dos atendimentos, falha no controle de peças e insumos, dificuldade de acompanhar o status dos serviços, perda do histórico de clientes e veículos, e ineficiência no fluxo de orçamentos e autorizações.

A primeira versão substituiu a planilha pelo registro que o próprio fluxo de trabalho produz: cada mudança de status é gravada com data e hora pela ação que a causou, o orçamento nasce dos itens lançados e vai ao cliente para aprovação, a peça é separada para a OS na aprovação do orçamento e só sai do saldo quando o Mecânico a retira, e o cliente acompanha a própria Ordem de Serviço sem depender de telefonema.

**O objetivo desta fase é evoluir essa aplicação para garantir qualidade, resiliência e escalabilidade.** A mudança de fundo é arquitetural, e ela deixa de ser promessa de documento porque o build prova: as dependências apontam para o centro e o framework não entra no anel dos casos de uso. Sobre essa base, o atendimento ganha o que o fluxo pedia e não tinha: o cliente pede serviço já na abertura, a fila mostra primeiro o que está na bancada e esconde o que já saiu, a resposta ao orçamento chega de fora por notificação, e cada mudança de status é avisada ao cliente por e-mail.

O recorte segue sendo de MVP: back-end, sem interface gráfica, com gestão de ordens de serviço, clientes e peças.

## O que o sistema faz

- **Ordem de Serviço** com máquina de estados (Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue, mais Cancelada, o sétimo status decidido no ADR-008), com mudança automática de status conforme as ações no sistema.
- **Orçamento** gerado automaticamente a partir dos itens de serviço e de peça, enviado ao cliente por e-mail, e respondido pela **notificação externa de aprovação ou de recusa**: duas rotas públicas que quem está fora do sistema chama portando o código de acompanhamento, sem exigir login do cliente.
- **CRUDs** de clientes, veículos, serviços e peças, este último com controle de estoque (reserva, baixa, entrada e consulta de peças abaixo do estoque mínimo).
- **Atualização de status por e-mail:** toda transição de status envia ao cliente um e-mail com o status novo e o endereço de acompanhamento. **A leitura adotada do requisito é esta**, notificar o cliente, e ela está fundamentada no ADR-026. O envio é atômico com a transição: se o e-mail não sai, a transição não acontece, porque falha visível é preferível a notificação perdida em silêncio. A transição que gera orçamento é notificada pelo e-mail do orçamento, que já carrega o status novo e o mesmo endereço, então cada transição produz exatamente um e-mail.
- **Tempo médio de execução** dos serviços, calculado a partir dos timestamps das transições de status.
- **Autenticação JWT** nas APIs administrativas e validação de dados sensíveis (documento e placa) como regra de domínio.

### Status da OS: o nome no enunciado da fase e o identificador da API

O enunciado da fase nomeia os status em português corrente, e a API responde identificadores. A tabela liga um ao outro, para que a palavra do enunciado leve ao campo certo; a escolha de manter os identificadores está no ADR-026.

| Nome no enunciado da fase | Identificador na API |
|---|---|
| Recebida | `RECEBIDA` |
| Diagnóstico | `EM_DIAGNOSTICO` |
| Aguardando Aprovação | `AGUARDANDO_APROVACAO` |
| Execução | `EM_EXECUCAO` |
| Finalizada | `FINALIZADA` |
| Entregue | `ENTREGUE` |
| Cancelada (sétimo status, ADR-008) | `CANCELADA` |

A **listagem sem filtro** devolve a fila de atendimento nesta ordem de prioridade: `EM_EXECUCAO`, `AGUARDANDO_APROVACAO`, `EM_DIAGNOSTICO`, `RECEBIDA`, e dentro do mesmo status da mais antiga para a mais nova. As OSs `FINALIZADA`, `ENTREGUE` e `CANCELADA` ficam fora dela. **A exclusão é lógica:** o registro continua respondendo pela consulta por identificador e reaparece quando o filtro `?status=` o pede.

## Documentação DDD

Event Storming dos dois fluxos, Domain Storytelling (AS-IS e TO-BE), Context Map com os subdomínios, modelo de domínio, Linguagem Ubíqua e o C4 nos níveis de Contexto e de Contêiner.

**Link da documentação:** https://miro.com/app/board/uXjVHuneCTk=/?share_link_id=665030044859

## Como subir o ambiente completo

Pré-requisitos: Docker e Docker Compose, com as portas **5432**, **8080**, **1025** e **8025** livres. Se houver um PostgreSQL ou outra aplicação ocupando alguma delas na máquina, o `docker compose up` falha com `port is already allocated`: pare o serviço local, ou ajuste o mapeamento no `docker-compose.yml`.

```bash
docker compose up --build
```

O ambiente sobe **populado**: o Flyway cria o schema e, logo depois, o serviço `seed` carrega os dados de demonstração, então não é preciso cadastrar nada para testar.

**Confira que a carga terminou bem.** O `docker compose up` não propaga a falha de um contêiner de tarefa única, então ele pode devolver sucesso com o banco vazio. O comando acima segura o terminal enquanto o ambiente estiver de pé, então **abra outro terminal** para conferir:

```bash
docker compose ps -a    # seed deve estar Exited (0); qualquer outro código é falha
docker compose logs seed
```

O `seed` é um contêiner de tarefa única: espera o schema existir, carrega uma vez e termina, então vê-lo sair da lista de contêineres em execução é o esperado. Os dados de demonstração e o script que os carrega vivem em [`seed/`](seed/), e não em `db/migration`, pelo motivo registrado no ADR-015.

O que já vem carregado: **uma Ordem de Serviço em cada um dos sete status**, com o histórico completo de transições (insumo do tempo médio de execução), clientes PF e PJ, veículos nos dois formatos de placa, catálogo de serviços e peças com saldo e estoque mínimo.

- Usuário administrativo de demonstração: login `admin`, senha `admin123`, apenas para o ambiente local de avaliação.
- Segredo de assinatura do JWT: fixado no `application.yml` e no `docker-compose.yml`, **apenas para o ambiente local de avaliação**, para que o ambiente suba sem configuração externa. Como ele está versionado, qualquer pessoa com o repositório emite um token válido, então **este valor não serve a nenhum ambiente real**; lá ele entra pela variável `JWT_SEGREDO`.
- As Ordens de Serviço em estado **vivo** (recebida, em diagnóstico, aguardando aprovação, em execução) têm datas **relativas ao momento da carga**, de propósito: elas mantêm a ordem aguardando aprovação dentro da janela em que o lembrete é devido, em vez de o ambiente envelhecer junto com o arquivo. O disparo automático do lembrete **não faz parte do MVP** (ADR-008); o que o ambiente entrega é o estado que o torna devido. As **encerradas** mantêm data absoluta no passado, porque são histórico. As duas concluídas alimentam o tempo médio de execução; a cancelada não, porque nunca entrou em execução.
- A carga acontece na **primeira subida**. Como o banco usa volume nomeado, subir de novo não recarrega, e o serviço `seed` detecta que já há dados e não faz nada. Para recomeçar do zero, `docker compose down -v && docker compose up --build`.

- API: `http://localhost:8080/api/v1`
- Banco: PostgreSQL 18 em `localhost:5432` (base, usuário e senha `oficina`, `oficina` e `oficina_local`, apenas para o ambiente local)
- Caixa de e-mail do ambiente: `http://localhost:8025`. É onde chegam o orçamento e o aviso de cada mudança de status, sem provedor externo e sem credencial (ADR-007).

## Swagger

Com o ambiente de pé:

- Interface: http://localhost:8080/swagger-ui.html
- Especificação: http://localhost:8080/v3/api-docs
- Contrato fonte versionado: [`openapi.yaml`](openapi.yaml)

As duas primeiras servem a especificação **gerada a partir do código**, e ela difere do contrato versionado em conteúdo, não em quantidade de rotas: a especificação gerada traz o título e a área de negócio de cada operação, mas **nenhuma delas tem descrição longa**; **não publica as respostas de erro nem o schema `Erro`**; não carrega as restrições de valor monetário (mínimo, teto e moeda única); **não traz o `info.description`**, que é onde moram as convenções de autorização e de erro de protocolo; expõe uma propriedade de validação de campo cruzado que o contrato não tem; e sai em OpenAPI 3.1.0 contra 3.0.3 do arquivo versionado. **O `openapi.yaml` é a fonte de verdade do contrato completo**, e é ele que deve ser lido para conhecer o contrato; o Swagger UI serve para experimentar as chamadas.

A documentação, o login, as três rotas de acompanhamento do cliente e as duas sondas de saúde (`/actuator/health/liveness` e `/actuator/health/readiness`) são públicos: as de acompanhamento pelo ADR-007, e as sondas porque o Kubernetes as consulta sem credencial. As sondas leem só o estado de disponibilidade da própria aplicação, sem consultar banco nem e-mail, então uma chamada anônima não gera trabalho fora dela. A raiz `/actuator/health` exige JWT, e nenhum outro endpoint do actuator é exposto. Todas as demais rotas **sob `/api/v1`** exigem JWT.

Para experimentar a superfície do cliente sem autenticar, use o código de acompanhamento da Ordem de Serviço que a carga deixa aguardando aprovação: `ACMP-e5a312adaec084e9ea783e0ff3f142a6`.

## Autenticação

`POST /api/v1/auth/login` é público e devolve o token; as demais rotas administrativas exigem o cabeçalho `Authorization: Bearer <token>` e respondem `401` com corpo JSON `{"codigo": "NAO_AUTORIZADO", "mensagem": "..."}` quando o token está ausente, é inválido ou expirou.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","senha":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/clientes
```

O token é HS256, com validade padrão de 60 minutos, assinado com o segredo de `oficina.jwt.segredo`. O segredo precisa ter no mínimo 32 bytes: abaixo disso a aplicação **não sobe**, e falha com a contagem de bytes na mensagem, em vez de quebrar no primeiro login.

## Executar localmente sem Docker

Requer **Java 25** e Maven. A versão exata do JDK está fixada em [`.sdkmanrc`](.sdkmanrc); com SDKMAN, basta `sdk env` na raiz do projeto.

```bash
docker compose up banco email -d    # sobe o PostgreSQL e a caixa de e-mail
TZ=UTC mvn spring-boot:run          # cria o schema pelo Flyway
```

**O `TZ=UTC` não é enfeite.** As colunas de data e hora são `TIMESTAMP` sem fuso, e o driver JDBC impõe o fuso da JVM à sessão do banco, inclusive ao `NOW()` avaliado no servidor. Os dados de demonstração são carregados por um contêiner em UTC; a aplicação rodando fora do Docker herdaria o fuso da máquina, e as duas escritas ficariam em relógios diferentes. Em fuso a oeste de Brasília isso chega a gravar uma transição **antes** da transição inicial da própria Ordem de Serviço, o que corrompe o tempo médio de execução (ADR-008). Pelo `docker compose up`, todos os contêineres estão em UTC e o problema não existe.

Este caminho sobe o banco **vazio**, porque o serviço `seed` não entra nele. O `mvn spring-boot:run` também segura o terminal, então, com a aplicação de pé e o schema criado, carregue os dados de demonstração **em outro terminal**:

```bash
docker compose run --rm --no-deps seed
```

## Provisionamento com Terraform

O ambiente de execução em Kubernetes é provisionado por código, no diretório [`infra/`](infra/): um cluster local com `kind`, de um nó de controle e um nó de trabalho, e o banco de dados dentro dele, com segredo, volume persistente, Deployment e Service. **O que cada recurso é, os pré-requisitos e o porquê de cada decisão estão em [`infra/README.md`](infra/README.md)** (ADR-024). O ambiente provisionado é local e descartável, e não descreve um ambiente de produção em nuvem.

**A porta 8080 é de um ambiente por vez.** O cluster publica a porta `30080` do nó de controle em `127.0.0.1:8080`, que é a mesma porta que o `docker compose` usa para a aplicação. Com o ambiente de Compose de pé, o `terraform apply` falha na criação do nó com `exit status 125`, que é o Docker recusando publicar uma porta ocupada. **Derrube um antes de subir o outro:** `docker compose down`.

**Os dois limites de `inotify` do núcleo também precisam estar levantados antes do primeiro `apply`**, senão o segundo nó do cluster não sobe. Os valores e o comando estão nos pré-requisitos de [`infra/README.md`](infra/README.md).

```bash
terraform -chdir=infra init
terraform -chdir=infra plan -out=plano.tfplan
terraform -chdir=infra apply plano.tfplan
```

O `apply` leva cerca de um minuto, acompanha o banco até ele estar pronto, e termina imprimindo o caminho do kubeconfig, o namespace e o endereço interno do banco. Conferindo:

```bash
kubectl get nodes                                     # dois nós Ready
kubectl -n oficina rollout status deployment/banco    # banco de pé
terraform -chdir=infra output
```

Todos os comandos deste README rodam da raiz do repositório; por isso o Terraform é chamado com `-chdir=infra`. Para desfazer, `terraform -chdir=infra destroy`, que apaga o cluster e, com ele, os dados do banco. Se o `apply` for interrompido ou falhar antes de o banco ficar pronto, o cluster pode ficar de pé fora do estado do Terraform, e aí quem o remove é `kind delete cluster --name oficina`.

**O `apply` troca o contexto corrente do `kubectl`.** Ele acrescenta ao `~/.kube/config` a entrada do cluster criado e a torna corrente; o `destroy` a remove e deixa o `kubectl` sem contexto corrente, sem apagar os demais. Quem usa o `kubectl` com outros clusters volta para o seu com `kubectl config use-context <nome>`.

## Deploy em Kubernetes

Com o cluster provisionado (seção anterior), a aplicação é publicada pelos manifestos de [`k8s/`](k8s/), aplicados com `kubectl`. O `terraform apply` já deixa o `kubectl` apontando para o cluster criado.

| Arquivo | Objeto | O que é |
|---|---|---|
| `oficina-app-deployment.yaml` | Deployment `oficina-app` | a API, com pedido e limite de CPU e memória, as sondas de inicialização, de prontidão e de vivacidade, e uma pausa de 10 segundos antes de desligar cada réplica, para nenhuma chamada cair numa réplica que está saindo |
| `oficina-app-service.yaml` | Service `oficina-app` | `NodePort` na porta `30080`, que o cluster publica em `127.0.0.1:8080` |
| `oficina-app-configmap.yaml` | ConfigMap `oficina-app` | endereço do banco, servidor de e-mail, fuso horário e log de acesso |
| `oficina-app-secret.yaml` | Secret `oficina-app` | o segredo de assinatura do JWT, com o mesmo valor local de avaliação do `docker-compose.yml`. Usuário e senha do banco não estão aqui: a aplicação os lê do Secret `banco`, criado pelo Terraform |
| `oficina-app-hpa.yaml` | HorizontalPodAutoscaler `oficina-app` | de 1 a 4 réplicas, pela CPU, com alvo de 70% |
| `email-deployment.yaml` e `email-service.yaml` | Deployment e Service `email` | a caixa de e-mail do ambiente, com a mesma imagem do compose |
| `seed-job.yaml` | Job `seed` | a carga dos dados de demonstração, pelo mesmo `seed/carrega.sh` do compose |
| `metrics-server.yaml` | `metrics-server`, em `kube-system` | o coletor de CPU e memória que o HPA consulta |

Os manifestos seguem os valores padrão do Terraform: cluster `oficina`, namespace `oficina` e banco `oficina`. Quem trocar esses valores no `terraform.tfvars` troca também em `k8s/` e nos comandos abaixo.

**A imagem.** O Deployment usa a imagem `ghcr.io/paulo-juchimiuk/oficina-mecanica-api:main`. Para publicar o código desta cópia, construa a imagem com esse nome e carregue-a nos nós do cluster; como a política de pull é `IfNotPresent`, o cluster usa a imagem carregada em vez de buscá-la no registro:

```bash
docker build -t ghcr.io/paulo-juchimiuk/oficina-mecanica-api:main .
kind load docker-image --name oficina \
  ghcr.io/paulo-juchimiuk/oficina-mecanica-api:main
```

**A publicação:**

```bash
kubectl -n oficina delete job seed --ignore-not-found
kubectl -n oficina create configmap seed --from-file=seed/ \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/
kubectl -n oficina rollout status deployment/oficina-app --timeout=300s
kubectl -n oficina wait --for=condition=complete job/seed --timeout=300s
```

O Job é apagado antes do `apply` porque o modelo de Pod de um Job não pode mudar depois de criado; como a carga é idempotente (ADR-015), rodá-la de novo não altera nada, e ela só carrega com o banco vazio. O ConfigMap `seed` é gerado de `seed/` na hora, e não versionado em `k8s/`, para que o script e os dados tenham um dono só. A carga espera o schema, que o Flyway cria no boot da aplicação, então ela termina logo depois de a aplicação ficar pronta, o que nesta máquina leva cerca de 20 segundos.

**Conferindo:**

```bash
kubectl -n oficina get deployment,service,configmap,secret,hpa,job
kubectl top pods -n oficina
curl -s localhost:8080/actuator/health/readiness
```

O `kubectl top` passa a responder até um minuto depois do primeiro `apply`, que é o tempo de o `metrics-server` colher as primeiras amostras.

Com o cluster de pé, a API, o Swagger e a autenticação respondem nos mesmos endereços do ambiente de Compose, em `localhost:8080`. A caixa de e-mail fica dentro do cluster; para abri-la, encaminhe a porta dela, num terminal que fica preso enquanto o encaminhamento durar, e abra `http://localhost:8025`:

```bash
kubectl -n oficina port-forward service/email 8025:8025
```

**Escalabilidade automática.** O HPA mede a CPU das réplicas contra o pedido de cada uma, `250m`, e mantém entre 1 e 4 réplicas, com alvo de 70%. Para simular aumento de carga, um Pod dentro do cluster chama o login em laço; o login confere a senha com BCrypt, que é caro em CPU de propósito, então cada chamada pesa de verdade:

```bash
kubectl -n oficina run gerador-carga --image=busybox:1.36 \
  --restart=Never -- /bin/sh -c 'while true; do
  wget -q -O- --header="Content-Type: application/json" \
  --post-data="{\"login\":\"admin\",\"senha\":\"admin123\"}" \
  http://oficina-app:8080/api/v1/auth/login >/dev/null; done'
kubectl -n oficina get hpa oficina-app -w
```

Nesta máquina, a CPU média passou do alvo em cerca de 30 segundos, e o HPA chegou a 4 réplicas em menos de um minuto, direto ou passando por 3, conforme o valor da primeira leitura acima do alvo. Para ver a volta, apague o gerador:

```bash
kubectl -n oficina delete pod gerador-carga
```

A CPU cai abaixo do alvo em pouco mais de um minuto, e o HPA espera a janela de estabilização de 5 minutos antes de reduzir as réplicas, para não oscilar com uma queda momentânea: nesta máquina, a volta a 1 réplica veio cerca de 6 minutos depois de o gerador ser apagado.

A escala é só pela CPU, e a conta que exclui a memória está no ADR-027: a memória de uma réplica quase não muda entre parada e sob carga, então ela não acompanha a demanda e, somada à CPU, impediria a volta a 1 réplica.

## Pipeline de CI/CD

A pipeline é do GitHub Actions, em [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml). Ela dispara em todo `push` e `pull_request` na `main`, e pelo botão **Run workflow** da aba Actions.

### O fluxo de deploy

| Ordem | Job | Onde roda | O que faz | Quando |
|---|---|---|---|---|
| 1 | Build e testes | executor hospedado do GitHub | build da aplicação; testes unitários e de integração, com Testcontainers; `terraform init`, `fmt -check` e `validate` de `infra/` | sempre |
| 2 | Build da imagem Docker | executor hospedado do GitHub | constrói a imagem e a publica no GitHub Container Registry com duas tags, o SHA do commit e `main` | na `main`, fora de pull request |
| 3 | Deploy no cluster Kubernetes | executor auto-hospedado, com o rótulo `oficina-local` | os cinco passos abaixo | na `main`, fora de pull request |

Os passos do deploy, com o nome que aparece no log:

1. **Conferência do cluster:** gera um kubeconfig próprio a partir do `kind` e confere os nós e o banco provisionado. O job não usa o contexto corrente do `kubectl` da máquina, então não publica em outro cluster que a máquina conheça.
2. **Carga da imagem no cluster:** baixa do registro a imagem do commit e a carrega nos nós, com a tag do commit e com `main`.
3. **Aplicação dos manifestos:** troca a tag `main` pela do commit numa cópia dos manifestos e confere a troca antes de qualquer comando no cluster; depois apaga o Job da carga anterior, gera o ConfigMap `seed`, aplica a cópia e espera o rollout. Cada commit publicado vira uma revisão do Deployment com a imagem dele, que `kubectl -n oficina rollout history deployment/oficina-app` lista; executar de novo o mesmo commit não cria revisão, porque o Deployment não muda.
4. **Deploy do banco de dados:** confere o banco que o Terraform provisionou, mostra no log o que o Flyway fez no boot da aplicação, a migration aplicada ou o aviso de que o schema já está em dia, e espera a carga de demonstração. Vem depois da aplicação dos manifestos porque o schema nasce no boot da aplicação e a carga espera por ele.
5. **Conferência da aplicação:** chama a sonda de prontidão pela porta 8080.

**Quem provisiona o banco.** O Terraform provisiona o banco, e a pipeline não roda `terraform apply`: o estado do Terraform é local, e um `apply` disparado pela pipeline teria um estado diferente do `apply` feito no terminal, e tentaria criar um cluster que já existe. Na pipeline, o deploy do banco de dados é o passo que confirma o banco, o schema e a carga (ADR-027).

### O executor auto-hospedado

O deploy precisa alcançar o cluster, que roda na máquina de quem o provisionou, e o executor hospedado do GitHub não alcança essa máquina. Por isso o terceiro job roda num executor registrado nela.

**Registrar, uma vez:** em **Settings, Actions, Runners, New self-hosted runner**, escolha Linux x64 e siga os comandos que a própria página mostra, numa pasta fora do clone, por exemplo `~/actions-runner`. Quando o `config.sh` pedir rótulos adicionais, informe `oficina-local`. Não instale como serviço.

**Ligar, a cada janela de trabalho,** num terminal que fica preso enquanto o executor estiver ligado:

```bash
cd ~/actions-runner && ./run.sh
```

A máquina precisa ter o Docker, o `kind` e o `kubectl` no `PATH`, e o cluster precisa estar de pé (seção "Provisionamento com Terraform").

**Com o executor desligado, o deploy espera na fila**, e os dois primeiros jobs rodam normalmente. O job enfileirado começa sozinho quando o executor é ligado, e falha se passar 24 horas na fila.

**Repositório público com executor auto-hospedado.** A disciplina de DevOps alerta que executor auto-hospedado não é recomendado para repositório público, porque o código de um pull request pode rodar na infraestrutura dele. Aqui isso é contido por cinco medidas, detalhadas no ADR-027: o repositório exige aprovação para rodar workflow de colaborador externo; o deploy só roda em `push` na `main` e pelo botão; o executor tem rótulo próprio e fica ligado só nas janelas de trabalho, nunca como serviço; o workflow pede a permissão mínima; e nenhum segredo fica gravado no executor.

### A imagem no registro

A imagem fica em `ghcr.io/paulo-juchimiuk/oficina-mecanica-api`, publicada com o token que o próprio job recebe, sem conta nem segredo a mais. Quando o pacote está público, quem aplica os manifestos sem construir a imagem recebe a do último push na `main`: basta pular o `docker build` e o `kind load` da seção "Deploy em Kubernetes".

## Testes

```bash
mvn verify
```

Roda os testes unitários (Surefire, `*Test`) e os de integração (Failsafe, `*IT`, com Testcontainers, portanto o Docker precisa estar ativo).

### Cobertura

O relatório do JaCoCo fica em `target/site/jacoco/index.html` após o `mvn verify`.

O build tem **gate de cobertura de 80% nos domínios críticos**, medido sobre os pacotes `domain` de cada contexto delimitado mais o `shared.domain`, que guarda a hierarquia de erros do domínio, e não sobre o projeto inteiro. Abaixo disso, o `mvn verify` falha.

A cobertura é medida sobre **tudo o que o `mvn verify` executa**, unitários e integração juntos: o agente do JaCoCo é preparado uma vez e as duas suítes escrevem no mesmo `target/jacoco.exec`. Rodar só uma das duas dá um número diferente, então o número que vale é o do `mvn verify` completo.

**Quais fluxos são testados, onde o gate morde e como se mede** está em [`docs/estrategia-testes.md`](docs/estrategia-testes.md).

## Análise de vulnerabilidades

Três superfícies, três ferramentas. As duas que rodam pelo Maven ficam no perfil `seguranca`, fora do build padrão; a varredura dinâmica roda por fora, contra o ambiente de pé (ADR-004).

**Análise estática do código**, com SpotBugs mais o plugin find-sec-bugs, que traz as regras de segurança de Java e Spring:

```bash
mvn -Pseguranca -DskipTests -Ddependency-check.skip=true \
  compile com.github.spotbugs:spotbugs-maven-plugin:spotbugs
```

O resultado sai em `target/spotbugsXml.xml`. Para ler no navegador:

```bash
mvn -Pseguranca com.github.spotbugs:spotbugs-maven-plugin:gui
```

**Varredura das dependências declaradas**, casando cada uma com as CVEs conhecidas:

```bash
mvn -Pseguranca verify
```

O relatório sai em `target/dependency-check-report.html`. **A primeira execução leva algo entre 30 e 60 minutos**, porque baixa a base de vulnerabilidades do NVD inteira (mais de 370 mil registros) para um cache local; as execuções seguintes são incrementais e rápidas. O download é limitado a 5 requisições por 30 segundos sem chave de API do NVD. **Salve o HTML fora de `target/` antes de qualquer `mvn clean`**, porque o diretório é descartável e não vai para o repositório.

A varredura da **API em execução** com OWASP ZAP roda **fora do build**, contra o ambiente de pé: é varredura dinâmica, feita sobre a API respondendo, e o relatório dela compõe a entrega de segurança. O relatório de vulnerabilidades da entrega compõe o resultado das três ferramentas, que cobrem superfícies diferentes: o código escrito aqui, a cadeia de dependências e o comportamento em runtime (ADR-004).

## Por que PostgreSQL

O domínio é relacional e transacional por natureza: Ordem de Serviço, Orçamento, Item, Peça e Reserva se ligam por chave estrangeira, e as invariantes que mais importam são de consistência entre linhas, como o Saldo em estoque nunca negativo e a reserva nunca maior que o saldo. O PostgreSQL entrega isso com `CHECK` e chave estrangeira no próprio banco, e entrega o **bloqueio pessimista de linha** (`SELECT ... FOR UPDATE`) que a serialização dos agregados usa, sem depender de coordenação na aplicação.

Somam-se a isso o `TIMESTAMP` de cada Transição de status, que é o dado de onde sai a métrica de tempo médio de execução, e o fato de o driver JDBC e o Testcontainers serem maduros no ecossistema Spring, o que mantém os testes de integração rodando contra o banco real e não contra um substituto em memória que se comporta de outro jeito.

Um banco de documentos foi descartado pelo mesmo motivo: ele resolveria bem a leitura de uma OS inteira, e pioraria exatamente o que aqui é o núcleo, que é consistência entre agregados sob concorrência.

O raciocínio completo, com a alternativa recusada e a política de versão, está no **ADR-002** e no **ADR-016**.

## Estrutura do projeto

**Clean Architecture** (ADR-025), com os **contextos delimitados** do Context Map como pacotes de primeiro nível e os **quatro anéis** dentro de cada contexto. Os quatro pacotes são os quatro anéis, e o que define qual é qual é a direção das dependências: nada de um anel interno menciona o nome de algo declarado num anel externo. O contexto vem do DDD, o anel vem da arquitetura, e cada pacote nasce junto da fatia do seu contexto.

```
br.com.oficinamecanica
├── ordemservico      Core.     Agregado: Ordem de Serviço      (implementado)
├── cadastro          Suporte.  Agregados: Cliente, Veículo     (implementado)
├── catalogo          Suporte.  Agregado: Serviço               (implementado)
├── estoque           Suporte.  Agregado: Peça                  (implementado)
├── autenticacao      Genérico. Agregado: Usuário               (implementado)
└── shared            não é contexto, apenas o que não tem dono
```

Dentro de cada contexto:

| Pacote | Anel da Clean Architecture | Responsabilidade |
|---|---|---|
| `api` | Adaptadores de interface: Controllers e Presenters | controllers REST, DTOs de entrada e as respostas que traduzem o agregado para a borda |
| `application` | Casos de uso | orquestra os casos de uso e resolve as pré-condições que atravessam agregados, como o vínculo entre Veículo e Cliente na criação da OS; a regra de dentro de um agregado mora no `domain`. **Não tem uma linha de import do framework** |
| `domain` | Entidades | agregados, value objects, entidades internas, portas de leitura e de saída |
| `infrastructure` | Adaptadores de interface: Gateways, mais a fronteira com Frameworks e drivers | persistência, adaptadores das portas, e as `@Configuration` que registram os casos de uso como bean e declaram a transação |

**A regra de dependência aponta para o centro, e isso tem prova no build.** Um teste de arquitetura afirma que `domain` e `application` não dependem de framework, de borda nem de infraestrutura, e que a borda não depende da infraestrutura; ele foi conferido por controle negativo, com um import proibido inserido de propósito para ver o teste ficar vermelho. Os casos de uso são registrados como bean por métodos explícitos numa `@Configuration` por contexto, na infraestrutura, e a transação é declarada por classe em `shared/infrastructure`: um teste de integração lista os casos de uso que existem no código e exige um bean transacional para cada um, com o atributo esperado. Por isso o `domain` e o `application` são testáveis sem subir o Spring.

**Dentro das camadas não há subpacotes, e isso é decisão (ADR-021).** A unidade de encapsulamento é a camada dentro do contexto: as classes de persistência de `infrastructure` são package-private, então é o compilador que impede qualquer outra camada de importar uma entidade JPA. Como em Java o acesso de pacote não atravessa subpacote, subdividir a camada tornaria essas classes públicas e trocaria uma garantia verificada pelo compilador por uma regra que nada verifica.

### Convenção de idioma do código

Este projeto adota uma regra **semântica**, e não geográfica: o idioma de um identificador é decidido pelo que ele **significa**, nunca pela camada ou pela pasta em que ele vive.

> **Nomeia algo que existe ou acontece no negócio da oficina? Português.**
> **Nomeia como o software implementa, transporta, persiste ou organiza algo? Inglês.**

O fundamento é a **Linguagem Ubíqua**. Todo o modelo deste trabalho, o glossário, o Event Storming, o Domain Storytelling, o Context Map e o modelo de domínio, foi construído em português, porque é a língua em que o negócio da oficina é falado e em que os conceitos foram descobertos. Manter esses mesmos termos no código preserva a continuidade que a Linguagem Ubíqua existe para garantir: **um conceito, um nome, do glossário ao banco de dados**, sem nenhuma camada de tradução entre a documentação e a implementação.

Pelo mesmo princípio, o vocabulário de engenharia permanece em inglês. `Repository`, `Controller`, `Configuration`, `domain`, `application` e `infrastructure` não são termos da oficina: são termos da construção de software, com significado consolidado e internacional. Traduzi-los não aproximaria o código do negócio, apenas afastaria o código da profissão.

A regra aplicada artefato por artefato:

| Artefato | Idioma | Exemplos |
|---|---|---|
| Pacotes de camada | inglês | `domain`, `application`, `infrastructure`, `api` |
| Pacotes de contexto delimitado | português | `ordemservico`, `estoque`, `catalogo` |
| Agregados, entidades e value objects | português | `OrdemServico`, `Orcamento`, `ReservaPeca`, `CodigoAcompanhamento` |
| Comportamentos do domínio | português | `aprovarOrcamento()`, `registrarReparoAdicional()`, `concluirDiagnostico()` |
| Casos de uso | português | `CriarOrdemServicoUseCase`, `ConsultarTempoMedioExecucaoUseCase` |
| Status da OS | português, nome do estado | `AGUARDANDO_APROVACAO`, `EM_EXECUCAO` |
| Exceções de domínio | conceito em português, sufixo técnico em inglês | `PecaComReservaAtivaException` |
| Padrões e mecanismos técnicos | inglês | `Controller`, `Repository`, `UseCase`, `Configuration` |
| Métodos herdados de framework | inglês | `save()`, `findById()` |
| DTOs e campos JSON | conceito em português, função técnica em inglês | `CriarOrdemServicoRequest`, `codigoAcompanhamento` |
| Tabelas e colunas de domínio | português, `snake_case` | `ordem_servico`, `codigo_acompanhamento` |
| Metadados de infraestrutura no banco | inglês | `created_at` |
| Nomes de teste de comportamento | português | `deveRecusarReparoAdicionalForaDeEmExecucao()` |

**Identificadores não usam acento** (`Orcamento`, e não `Orçamento`). O acento é preservado em texto, comentários, `@DisplayName` e dados. Java aceita Unicode em identificadores, mas ASCII reduz atrito de busca, teclado e ferramental.

A correspondência entre cada termo do negócio e seu identificador está no glossário de Linguagem Ubíqua, que é a fonte de verdade: **cada conceito do glossário tem um nome por camada, na convenção de cada uma, e nunca dois nomes concorrentes na mesma camada. Um conceito nunca aparece no projeto sob um segundo nome.** A regra vale para os conceitos do glossário; sufixo técnico (`Request`, `Response`, `UseCase`), vocabulário de framework e palavra de forma de projeção (`Resumo`, `Detalhe`) não são conceitos do negócio e não entram nele.

## Decisões de arquitetura

São **27**, cada uma com fundamento de negócio, fundamento técnico e o porquê, em [`docs/decisoes.md`](docs/decisoes.md). Onde a alternativa recusada é o próprio argumento, ela aparece em uma linha.

**Os códigos `ADR-0xx` citados neste README, no contrato da API e nos testes referem-se a esse documento.**
