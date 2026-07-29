# oficina-mecanica-api

API de gestão para oficina mecânica de médio porte: ordem de serviço, orçamento com aprovação do cliente, controle de estoque e acompanhamento do atendimento. Back-end monolítico, modelado com DDD.

Tech Challenge da Fase 1 da pós-graduação em Arquitetura de Software (FIAP).

> **Estado atual: esqueleto.** A estrutura, o build, a infraestrutura e o contrato da API já estão definidos. A implementação das regras de negócio depende das decisões de modelagem ainda em aberto (ADRs 006 a 010). Esta nota sai quando o MVP estiver completo.

## O que o sistema faz

- **Ordem de Serviço** com máquina de estados (Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue), com mudança automática de status conforme as ações no sistema.
- **Orçamento** gerado automaticamente a partir dos itens de serviço e de peça, enviado ao cliente, com aprovação ou reprovação via API pelo código de acompanhamento, sem exigir login do cliente.
- **CRUDs** de clientes, veículos, serviços e peças, este último com controle de estoque (reserva, baixa, entrada e alerta de reposição por estoque mínimo).
- **Tempo médio de execução** dos serviços, calculado a partir dos timestamps das transições de status.
- **Autenticação JWT** nas APIs administrativas e validação de dados sensíveis (documento e placa) como regra de domínio.

## Como subir o ambiente completo

Pré-requisitos: Docker e Docker Compose.

```bash
docker compose up --build
```

O ambiente é projetado para subir **populado**: o Flyway cria o schema e aplica os dados de demonstração antes de a API atender, para que não seja preciso cadastrar nada para testar.

> **Ainda não vale para o esqueleto.** A migration de schema depende do modelo de domínio e será adicionada junto com as entidades. Até lá, `docker compose up` sobe o banco e a aplicação, mas sem schema nem dados. O arquivo de dados de demonstração já existe, inerte, em `src/main/resources/db/seed/`.

- API: `http://localhost:8080`
- Banco: PostgreSQL 16 em `localhost:5432` (base, usuário e senha `oficina`, `oficina` e `oficina_local`, apenas para o ambiente local)

## Swagger

Com o ambiente de pé:

- Interface: http://localhost:8080/swagger-ui.html
- Especificação: http://localhost:8080/v3/api-docs
- Contrato fonte versionado: [`openapi.yaml`](openapi.yaml)

## Executar localmente sem Docker

Requer **Java 21** e Maven. A versão exata do JDK está fixada em [`.sdkmanrc`](.sdkmanrc); com SDKMAN, basta `sdk env` na raiz do projeto.

```bash
docker compose up banco -d    # sobe apenas o PostgreSQL
mvn spring-boot:run
```

## Testes

```bash
mvn verify
```

Roda os testes unitários (Surefire, `*Test`) e os de integração (Failsafe, `*IT`, com Testcontainers, portanto o Docker precisa estar ativo).

### Cobertura

O relatório do JaCoCo fica em `target/site/jacoco/index.html` após o `mvn verify`.

O build tem **gate de cobertura de 80% nos domínios críticos**, medido sobre os pacotes `dominio` de cada contexto delimitado, e não sobre o projeto inteiro. Abaixo disso, o `mvn verify` falha.

## Análise de vulnerabilidades

Varredura das dependências e do código:

```bash
mvn -Pseguranca verify
```

O relatório sai em `dependency-check-report.html`. A varredura dinâmica da API em execução é feita com OWASP ZAP, e o resultado das duas compõe o relatório de vulnerabilidades da entrega.

## Estrutura do projeto

Monolito em camadas. Cada **contexto delimitado** do Context Map é um pacote de primeiro nível, e dentro de cada contexto ficam as **quatro camadas do DDD**:

```
br.com.oficinamecanica
├── ordemservico      Core.     Agregado: Ordem de Serviço
├── cadastro          Suporte.  Agregados: Cliente, Veículo
├── catalogo          Suporte.  Agregado: Serviço
├── estoque           Suporte.  Agregado: Peça
├── autenticacao      Genérico. Agregado: Usuário
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
| Agregados, entidades e value objects | português | `OrdemServico`, `Orcamento`, `SaldoEstoque`, `CodigoAcompanhamento` |
| Comportamentos do domínio | português | `aprovarOrcamento()`, `reservarPecas()`, `iniciarExecucao()` |
| Casos de uso | português | `AbrirOrdemServico`, `CalcularTempoMedioExecucao` |
| Eventos de domínio | português, verbo no passado | `OrcamentoAprovado`, `PecasReservadas` |
| Exceções de domínio | conceito em português, sufixo técnico em inglês | `EstoqueInsuficienteException` |
| Padrões e mecanismos técnicos | inglês | `Controller`, `Repository`, `Mapper`, `Configuration` |
| Métodos herdados de framework | inglês | `save()`, `findById()` |
| DTOs e campos JSON | conceito em português, função técnica em inglês | `CriarOrdemServicoRequest`, `codigoAcompanhamento` |
| Tabelas e colunas de domínio | português, `snake_case` | `ordem_servico`, `codigo_acompanhamento` |
| Metadados de infraestrutura no banco | inglês | `created_at`, `updated_at`, `version` |
| Nomes de teste de comportamento | português | `deveImpedirExecucaoSemOrcamentoAprovado()` |

**Identificadores não usam acento** (`Orcamento`, e não `Orçamento`). O acento é preservado em texto, comentários, `@DisplayName` e dados. Java aceita Unicode em identificadores, mas ASCII reduz atrito de busca, teclado e ferramental.

A correspondência entre cada termo do negócio e seu identificador está no glossário de Linguagem Ubíqua, que é a fonte de verdade: **nenhum conceito do glossário aparece no projeto sob um segundo nome.**

## Decisões de arquitetura

Todas documentadas com fundamento de negócio, fundamento técnico e o porquê.

| ADR | Decisão | Status |
|---|---|---|
| 001 | Java 21 e Spring Boot 3, build com Maven | decidido |
| 002 | PostgreSQL 16 | decidido |
| 003 | Monolito em camadas, contextos delimitados como pacotes | decidido |
| 004 | OWASP ZAP na API em execução, mais OWASP Dependency-Check sobre o código | decidido |
| 005 | JUnit 5, Mockito, Testcontainers e JaCoCo com gate de 80% | proposto |
| 006 a 010 | Modelagem: agregados, orçamento versionado, máquina de estados, autenticação, chassi | em aberto |
| 012 | Convenção de idioma: domínio em português, engenharia em inglês, por regra semântica | decidido |

## Documentação DDD

Event Storming, Domain Storytelling (AS-IS e TO-BE), Context Map com os subdomínios, modelo de domínio e Linguagem Ubíqua.

**Link da documentação:** a preencher quando as pranchas forem publicadas.
