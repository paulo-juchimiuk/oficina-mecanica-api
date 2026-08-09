# oficina-mecanica-api

API de gestão para oficina mecânica de médio porte: ordem de serviço, orçamento com aprovação do cliente, controle de estoque e acompanhamento do atendimento. Back-end monolítico, modelado com DDD.

Tech Challenge da Fase 1 da pós-graduação em Arquitetura de Software (FIAP).

> **Estado atual: em construção.** Estrutura, build, infraestrutura, contrato da API e **schema com dados de demonstração** estão prontos. Dos cinco contextos delimitados, **Autenticação, Cadastro (clientes e veículos) e Catálogo de Serviços estão implementados e respondendo**; Ordem de Serviço e Estoque ainda não. As rotas dos contextos não implementados constam do `openapi.yaml` e, com token válido, respondem `404`; sem token respondem `401`. As rotas de acompanhamento do cliente, que o contrato declara públicas, também respondem `401` hoje: elas só passam a ser liberadas quando o contexto Ordem de Serviço existir. Esta nota sai quando o MVP estiver completo.

## O que o sistema faz

- **Ordem de Serviço** com máquina de estados (Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue, mais Cancelada, o sétimo status decidido no ADR-008), com mudança automática de status conforme as ações no sistema.
- **Orçamento** gerado automaticamente a partir dos itens de serviço e de peça, enviado ao cliente, com aprovação ou reprovação via API pelo código de acompanhamento, sem exigir login do cliente.
- **CRUDs** de clientes, veículos, serviços e peças, este último com controle de estoque (reserva, baixa, entrada e consulta de peças abaixo do estoque mínimo).
- **Tempo médio de execução** dos serviços, calculado a partir dos timestamps das transições de status.
- **Autenticação JWT** nas APIs administrativas e validação de dados sensíveis (documento e placa) como regra de domínio.

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
- As Ordens de Serviço em estado **vivo** (recebida, em diagnóstico, aguardando aprovação, em execução) têm datas **relativas ao momento da carga**, de propósito: elas mantêm a ordem aguardando aprovação dentro da janela em que o lembrete é devido, em vez de o ambiente envelhecer junto com o arquivo. O disparo automático do lembrete **não faz parte do MVP** (ADR-008); o que o ambiente entrega é o estado que o torna devido. As **encerradas** mantêm data absoluta no passado, porque são histórico. As duas concluídas alimentam o tempo médio de execução; a cancelada não, porque nunca entrou em execução.
- A carga acontece na **primeira subida**. Como o banco usa volume nomeado, subir de novo não recarrega, e o serviço `seed` detecta que já há dados e não faz nada. Para recomeçar do zero, `docker compose down -v && docker compose up --build`.

- API: `http://localhost:8080/api/v1`
- Banco: PostgreSQL 18 em `localhost:5432` (base, usuário e senha `oficina`, `oficina` e `oficina_local`, apenas para o ambiente local)
- Caixa de e-mail do ambiente: `http://localhost:8025`. É onde o orçamento chega quando o envio for implementado, sem provedor externo e sem credencial (ADR-007).

## Swagger

Com o ambiente de pé:

- Interface: http://localhost:8080/swagger-ui.html
- Especificação: http://localhost:8080/v3/api-docs
- Contrato fonte versionado: [`openapi.yaml`](openapi.yaml)

As duas primeiras servem a especificação **gerada a partir do código já implementado**, então enquanto a implementação avança elas mostram menos que o contrato. A diferença não é só de quantidade de rotas: a especificação gerada **não publica as respostas de erro nem o schema `Erro`**, não carrega as restrições de valor monetário (mínimo, teto e moeda única), **não traz o `info.description`**, que é onde moram as convenções de autorização e de erro de protocolo, e sai em OpenAPI 3.1.0 contra 3.0.3 do arquivo versionado. **O `openapi.yaml` é a fonte de verdade do contrato completo**, e é ele que deve ser lido para conhecer o contrato; o Swagger UI serve para experimentar as chamadas.

A documentação é pública. Todo o resto exige JWT.

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

## Análise de vulnerabilidades

Varredura das dependências declaradas no projeto, casando cada uma com as CVEs conhecidas:

```bash
mvn -Pseguranca verify
```

O relatório sai em `target/dependency-check-report.html`. **A primeira execução leva algo entre 30 e 60 minutos**, porque baixa a base de vulnerabilidades do NVD inteira (mais de 370 mil registros) para um cache local; as execuções seguintes são incrementais e rápidas. O download é limitado a 5 requisições por 30 segundos sem chave de API do NVD. **Salve o HTML fora de `target/` antes de qualquer `mvn clean`**, porque o diretório é descartável e não vai para o repositório.

A varredura da **API em execução** com OWASP ZAP **ainda não está montada neste repositório**: ela entra quando os endpoints existirem, porque é varredura dinâmica e precisa da API respondendo. O relatório de vulnerabilidades da entrega compõe o resultado das duas ferramentas. Elas cobrem superfícies diferentes, a cadeia de dependências e o comportamento em runtime, e nenhuma das duas faz análise estática do código escrito aqui (ADR-004).

## Estrutura do projeto

Monolito em camadas. Cada **contexto delimitado** do Context Map é um pacote de primeiro nível, e dentro de cada contexto ficam as **quatro camadas do DDD**. Cada pacote nasce junto da fatia do seu contexto.

```
br.com.oficinamecanica
├── ordemservico      Core.     Agregado: Ordem de Serviço      (a implementar)
├── cadastro          Suporte.  Agregados: Cliente, Veículo     (implementado)
├── catalogo          Suporte.  Agregado: Serviço               (implementado)
├── estoque           Suporte.  Agregado: Peça                  (a implementar)
├── autenticacao      Genérico. Agregado: Usuário               (implementado)
└── shared            não é contexto, apenas o que não tem dono
```

Dentro de cada contexto:

| Pacote | Camada do DDD | Responsabilidade |
|---|---|---|
| `api` | interface do usuário | controllers REST e DTOs |
| `application` | aplicação | orquestra casos de uso, sem regra de negócio |
| `domain` | domínio | agregados, value objects, serviços de domínio, eventos |
| `infrastructure` | infraestrutura | persistência e adaptadores |

**A regra de dependência aponta para o domínio.** O pacote `domain` não importa `infrastructure` nem `api`, e por isso é testável sem subir o Spring.

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
| Agregados, entidades e value objects | português | `OrdemServico`, `Orcamento`, `SaldoEmEstoque`, `CodigoAcompanhamento` |
| Comportamentos do domínio | português | `aprovarOrcamento()`, `reservarPecas()`, `concluirDiagnostico()` |
| Casos de uso | português | `CriarOrdemServico`, `CalcularTempoMedioExecucao` |
| Eventos de domínio | português, verbo no passado | `OrcamentoAprovado`, `PecasReservadas` |
| Exceções de domínio | conceito em português, sufixo técnico em inglês | `EstoqueInsuficienteException` |
| Padrões e mecanismos técnicos | inglês | `Controller`, `Repository`, `Mapper`, `Configuration` |
| Métodos herdados de framework | inglês | `save()`, `findById()` |
| DTOs e campos JSON | conceito em português, função técnica em inglês | `CriarOrdemServicoRequest`, `codigoAcompanhamento` |
| Tabelas e colunas de domínio | português, `snake_case` | `ordem_servico`, `codigo_acompanhamento` |
| Metadados de infraestrutura no banco | inglês | `created_at` |
| Nomes de teste de comportamento | português | `deveImpedirExecucaoSemOrcamentoAprovado()` |

**Identificadores não usam acento** (`Orcamento`, e não `Orçamento`). O acento é preservado em texto, comentários, `@DisplayName` e dados. Java aceita Unicode em identificadores, mas ASCII reduz atrito de busca, teclado e ferramental.

A correspondência entre cada termo do negócio e seu identificador está no glossário de Linguagem Ubíqua, que é a fonte de verdade: **nenhum conceito do glossário aparece no projeto sob um segundo nome.**

## Decisões de arquitetura

Todas documentadas com fundamento de negócio, fundamento técnico e o porquê. A tabela abaixo é o índice.

**Os códigos `ADR-0xx` citados no código, no contrato e nos arquivos de infraestrutura referem-se a esta tabela.** O texto completo de cada decisão vive no documento de decisões arquiteturais, que é entregue junto da documentação do projeto e ainda não está publicado aqui; o link entra nesta página quando a documentação for publicada.

| ADR | Decisão | Status |
|---|---|---|
| 001 | Java 25 LTS e Spring Boot 4, build com Maven | decidido |
| 002 | PostgreSQL 18 | decidido |
| 003 | Monolito em camadas, contextos delimitados como pacotes | decidido |
| 004 | OWASP ZAP na API em execução, mais OWASP Dependency-Check nas dependências | decidido |
| 005 | JUnit 5, Mockito, Testcontainers e JaCoCo com gate de 80% | decidido |
| 006 | Fronteiras dos agregados de Cadastro e o Orçamento versionado | decidido |
| 007 | Interação do cliente com o sistema, por Código de acompanhamento | decidido |
| 008 | Máquina de estados da OS e a métrica de tempo | decidido |
| 009 | Autenticação como subdomínio Genérico | decidido |
| 010 | Chassi fora do MVP | decidido |
| 011 | Identidade da entrega, nome do projeto | decidido |
| 012 | Convenção de idioma: domínio em português, engenharia em inglês, por regra semântica | decidido |
| 013 | Fluxo de estoque: falta vira pendência, baixa na retirada, devolução manual | decidido |
| 014 | Remoção lógica nos cadastros, para o histórico de OS não virar registro órfão | decidido |
| 015 | Dados de demonstração fora do fluxo de migrations, carregados por serviço próprio | decidido |
| 016 | Política de versão: sempre numa versão que ainda recebe correção | decidido |

## Documentação DDD

Event Storming, Domain Storytelling (AS-IS e TO-BE), Context Map com os subdomínios, modelo de domínio e Linguagem Ubíqua.

**Link da documentação:** a preencher quando as pranchas forem publicadas.
