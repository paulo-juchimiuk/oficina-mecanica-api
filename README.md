# oficina-mecanica-api

API de gestão para oficina mecânica de médio porte: ordem de serviço, orçamento com aprovação do cliente, controle de estoque e acompanhamento do atendimento. Back-end monolítico, modelado com DDD.

Tech Challenge da Fase 1 da pós-graduação em Arquitetura de Software (FIAP).

## Objetivos

A oficina atende, diagnostica, executa e entrega usando anotação manual e planilha, e é dessa forma de trabalho que nascem os cinco problemas que este sistema existe para resolver: erro na priorização dos atendimentos, falha no controle de peças e insumos, dificuldade de acompanhar o status dos serviços, perda do histórico de clientes e veículos, e ineficiência no fluxo de orçamentos e autorizações.

O objetivo desta primeira versão é substituir a planilha pelo registro que o próprio fluxo de trabalho produz: cada mudança de status é gravada com data e hora pela ação que a causou, o orçamento nasce dos itens lançados e vai ao cliente para aprovação, a peça é separada para a OS na aprovação do orçamento e só sai do saldo quando o Mecânico a retira, e o cliente acompanha a própria Ordem de Serviço sem depender de telefonema.

O recorte é de MVP: back-end, sem interface gráfica, com gestão de ordens de serviço, clientes e peças.

## O que o sistema faz

- **Ordem de Serviço** com máquina de estados (Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue, mais Cancelada, o sétimo status decidido no ADR-008), com mudança automática de status conforme as ações no sistema.
- **Orçamento** gerado automaticamente a partir dos itens de serviço e de peça, enviado ao cliente, com aprovação ou reprovação via API pelo código de acompanhamento, sem exigir login do cliente.
- **CRUDs** de clientes, veículos, serviços e peças, este último com controle de estoque (reserva, baixa, entrada e consulta de peças abaixo do estoque mínimo).
- **Tempo médio de execução** dos serviços, calculado a partir dos timestamps das transições de status.
- **Autenticação JWT** nas APIs administrativas e validação de dados sensíveis (documento e placa) como regra de domínio.

## Documentação DDD

Event Storming, Domain Storytelling (AS-IS e TO-BE), Context Map com os subdomínios, modelo de domínio e Linguagem Ubíqua.

**Link da documentação:** a preencher quando as pranchas forem publicadas.

## Como subir o ambiente completo

Pré-requisitos: Docker e Docker Compose, com as portas **5432**, **8080**, **1025** e **8025** livres. Se houver um PostgreSQL ou outra aplicação ocupando alguma delas na máquina, o `docker compose up` falha com `port is already allocated`: pare o serviço local, ou ajuste o mapeamento no `docker-compose.yml`.

```bash
docker compose up --build
```

O ambiente sobe **populado**: o Flyway cria o schema e, logo depois, o serviço `seed` carrega os dados de demonstração, então não é preciso cadastrar nada para testar.

**Confira que a carga terminou bem.** O `docker compose up` não propaga a falha de um contêiner de tarefa única, então ele pode devolver sucesso com o banco vazio:

```bash
docker compose ps -a    # seed deve estar Exited (0); qualquer outro código é falha
docker compose logs seed
```

O `seed` é um contêiner de tarefa única: espera o schema existir, carrega uma vez e termina, então vê-lo sair da lista de contêineres em execução é o esperado. Os dados de demonstração vivem em [`seed/`](seed/) e não em `db/migration`, pelo motivo registrado no ADR-015.

O que já vem carregado: **uma Ordem de Serviço em cada um dos sete status**, com o histórico completo de transições (insumo do tempo médio de execução), clientes PF e PJ, veículos nos dois formatos de placa, catálogo de serviços e peças com saldo e estoque mínimo.

- Usuário administrativo de demonstração: login `admin`, senha `admin123`, apenas para o ambiente local de avaliação.
- Segredo de assinatura do JWT: fixado no `application.yml` e no `docker-compose.yml`, **apenas para o ambiente local de avaliação**, para que o ambiente suba sem configuração externa. Como ele está versionado, qualquer pessoa com o repositório emite um token válido, então **este valor não serve a nenhum ambiente real**; lá ele entra pela variável `JWT_SEGREDO`.
- As Ordens de Serviço em estado **vivo** (recebida, em diagnóstico, aguardando aprovação, em execução) têm datas **relativas ao momento da carga**, de propósito: elas mantêm a ordem aguardando aprovação dentro da janela em que o lembrete é devido, em vez de o ambiente envelhecer junto com o arquivo. O disparo automático do lembrete **não faz parte do MVP** (ADR-008); o que o ambiente entrega é o estado que o torna devido. As **encerradas** mantêm data absoluta no passado, porque são histórico. As duas concluídas alimentam o tempo médio de execução; a cancelada não, porque nunca entrou em execução.
- A carga acontece na **primeira subida**. Como o banco usa volume nomeado, subir de novo não recarrega, e o serviço `seed` detecta que já há dados e não faz nada. Para recomeçar do zero, `docker compose down -v && docker compose up --build`.

- API: `http://localhost:8080/api/v1`
- Banco: PostgreSQL 18 em `localhost:5432` (base, usuário e senha `oficina`, `oficina` e `oficina_local`, apenas para o ambiente local)
- Caixa de e-mail do ambiente: `http://localhost:8025`. É onde o orçamento chega, sem provedor externo e sem credencial (ADR-007).

## Swagger

Com o ambiente de pé:

- Interface: http://localhost:8080/swagger-ui.html
- Especificação: http://localhost:8080/v3/api-docs
- Contrato fonte versionado: [`openapi.yaml`](openapi.yaml)

As duas primeiras servem a especificação **gerada a partir do código**, e ela difere do contrato versionado em conteúdo, não em quantidade de rotas: a especificação gerada traz o título e a área de negócio de cada operação, mas **nenhuma delas tem descrição longa**; **não publica as respostas de erro nem o schema `Erro`**; não carrega as restrições de valor monetário (mínimo, teto e moeda única); **não traz o `info.description`**, que é onde moram as convenções de autorização e de erro de protocolo; expõe uma propriedade de validação de campo cruzado que o contrato não tem; e sai em OpenAPI 3.1.0 contra 3.0.3 do arquivo versionado. **O `openapi.yaml` é a fonte de verdade do contrato completo**, e é ele que deve ser lido para conhecer o contrato; o Swagger UI serve para experimentar as chamadas.

A documentação, o login e as três rotas de acompanhamento do cliente são públicos (ADR-007). Todas as demais rotas **sob `/api/v1`** exigem JWT. Fora desse prefixo responde ainda o `/error`, que é o caminho de erro do próprio framework e não faz parte do contrato.

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

Este caminho sobe o banco **vazio**, porque o serviço `seed` não entra nele. Com a aplicação de pé e o schema criado, carregue os dados de demonstração com:

```bash
docker compose run --rm --no-deps seed
```

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

Monolito em camadas, como o enunciado permite para um MVP, com os **contextos delimitados** do Context Map como pacotes de primeiro nível e as **quatro camadas do DDD** dentro de cada contexto. As duas exigências se encontram aqui: a camada vem do requisito técnico, o contexto vem do DDD. Cada pacote nasce junto da fatia do seu contexto.

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

| Pacote | Camada do DDD | Responsabilidade |
|---|---|---|
| `api` | interface do usuário | controllers REST e DTOs |
| `application` | aplicação | orquestra casos de uso e resolve as pré-condições que atravessam agregados, como o vínculo entre Veículo e Cliente na criação da OS; a regra de dentro de um agregado mora no `domain` |
| `domain` | domínio | agregados, value objects, entidades internas, portas de leitura |
| `infrastructure` | infraestrutura | persistência e adaptadores |

**A regra de dependência aponta para o domínio.** O pacote `domain` não importa `infrastructure` nem `api`, e por isso é testável sem subir o Spring.

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
| Transições de status | português, nome do estado | `AGUARDANDO_APROVACAO`, `EM_EXECUCAO` |
| Exceções de domínio | conceito em português, sufixo técnico em inglês | `PecaComReservaAtivaException` |
| Padrões e mecanismos técnicos | inglês | `Controller`, `Repository`, `Mapper`, `Configuration` |
| Métodos herdados de framework | inglês | `save()`, `findById()` |
| DTOs e campos JSON | conceito em português, função técnica em inglês | `CriarOrdemServicoRequest`, `codigoAcompanhamento` |
| Tabelas e colunas de domínio | português, `snake_case` | `ordem_servico`, `codigo_acompanhamento` |
| Metadados de infraestrutura no banco | inglês | `created_at` |
| Nomes de teste de comportamento | português | `deveRecusarReparoAdicionalForaDeEmExecucao()` |

**Identificadores não usam acento** (`Orcamento`, e não `Orçamento`). O acento é preservado em texto, comentários, `@DisplayName` e dados. Java aceita Unicode em identificadores, mas ASCII reduz atrito de busca, teclado e ferramental.

A correspondência entre cada termo do negócio e seu identificador está no glossário de Linguagem Ubíqua, que é a fonte de verdade: **cada conceito do glossário tem um nome por camada, na convenção de cada uma, e nunca dois nomes concorrentes na mesma camada. Um conceito nunca aparece no projeto sob um segundo nome.** A regra vale para os conceitos do glossário; sufixo técnico (`Request`, `Response`, `UseCase`), vocabulário de framework e palavra de forma de projeção (`Resumo`, `Detalhe`) não são conceitos do negócio e não entram nele.

## Decisões de arquitetura

São **21**, cada uma com fundamento de negócio, fundamento técnico e o porquê, em [`docs/decisoes.md`](docs/decisoes.md). Onde a alternativa recusada é o próprio argumento, ela aparece em uma linha.

**Os códigos `ADR-0xx` citados neste README, no contrato da API e nos testes referem-se a esse documento.**
