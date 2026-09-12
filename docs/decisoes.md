# Decisões de arquitetura (ADRs)

São 26 decisões. Cada uma traz o **fundamento de negócio**, o **fundamento técnico** e **o porquê**. Onde a alternativa recusada é o próprio argumento, ela aparece em uma linha. Onde algo ficou fora do MVP, isso está declarado, e não omitido.

---

## ADR-001: Java com Spring Boot

**Decisão:** Java 25 com Spring Boot 4.1, build com Maven.

**Fundamento de negócio:** a oficina precisa do sistema no ar e funcionando. Entregar na linguagem que eu domino é o que reduz o risco de não entregar.

**Fundamento técnico:** o Spring Boot cobre com biblioteca madura tudo o que o sistema pede, sem cola: REST, JWT, Bean Validation para Documento e Placa, JPA com Flyway, springdoc para o Swagger, e JUnit com Testcontainers.

**Porquê:** domínio na linguagem, e o ecossistema resolve os requisitos sem eu precisar escrever infraestrutura.

---

## ADR-002: PostgreSQL

**Decisão:** PostgreSQL 18.

**Fundamento de negócio:** o dinheiro do orçamento e o saldo do estoque não podem divergir. Erro aqui é prejuízo direto e perda de confiança do cliente da oficina.

**Fundamento técnico:** o domínio é relacional e transacional: a OS referencia Cliente, Veículo, Serviços e Peças por identidade, e as invariantes que mais importam são entre linhas, como o saldo nunca negativo e a reserva nunca maior que o saldo. O PostgreSQL entrega isso com `CHECK` e chave estrangeira, entrega a trava pessimista de linha que a serialização dos agregados usa, e guarda o instante de cada Transição de status, que é o dado de onde sai o Tempo médio de execução.

**Porquê:** atende todos os requisitos sem adaptação, e tem o melhor ferramental de migração e teste do ecossistema Java, o que mantém o teste de integração rodando contra o banco real.

**Alternativa recusada:** banco de documentos. Resolveria bem a leitura de uma OS inteira e pioraria justamente o núcleo, que é consistência entre agregados sob concorrência.

---

## ADR-003: Monolito em camadas

**Decisão:** monolito em camadas, com os contextos delimitados como pacotes internos e a regra de dependência apontando para o domínio.

**Fundamento de negócio:** é um MVP para o cliente testar agora. Cada dia gasto em arquitetura não pedida é um dia a menos de funcionalidade entregue.

**Fundamento técnico:** camadas com domínio isolado mantêm agregados e objetos de valor puros, testáveis sem subir o Spring.

**Porquê:** é uma das duas opções que o enunciado admite, e é a base natural para a evolução das fases seguintes, que reorganiza dependências em vez de reescrever.

**Limite declarado:** a configuração de segurança é a única classe de `infrastructure` que importa de `api`. Ela monta o corpo do 401 no envelope do contrato e conhece o prefixo das rotas, que são fatos da borda HTTP, e um filtro de segurança é borda por natureza. A regra que a arquitetura protege continua íntegra e verificável: em nenhum dos cinco contextos o pacote `domain` importa `api`, `infrastructure` ou framework.

**Alternativa recusada:** Clean Architecture já nesta fase. Aumenta o custo agora sem atender nada que o enunciado peça.

**Substituído pelo ADR-025 na Fase 2**, que adota a Clean Architecture quando a fase passa a exigi-la. Este registro fica como histórico da decisão da Fase 1.

---

## ADR-004: Análise de vulnerabilidades em três superfícies

**Decisão:** SpotBugs com o plugin find-sec-bugs sobre o código compilado, OWASP Dependency-Check sobre as dependências declaradas, e OWASP ZAP contra a API em execução, com leitura pelo OWASP Top 10. As três compõem o relatório. As duas primeiras rodam pelo perfil `seguranca` do Maven, fora do build padrão; a terceira roda por fora, contra a aplicação no ar.

**Fundamento de negócio:** o sistema guarda dado de cliente e movimenta o valor do estoque, e as três superfícies por onde isso vaza são diferentes: o código escrito aqui, a cadeia de dependências herdada e a aplicação respondendo na rede. Cobrir só uma delas produz relatório que tranquiliza sem proteger.

**Fundamento técnico:** as três ferramentas medem coisas diferentes e complementares. O SpotBugs com find-sec-bugs faz **análise estática**, lendo o bytecode do próprio projeto, com regras de segurança específicas de Java e Spring; o Dependency-Check é de composição e casa as dependências com CVEs conhecidas; o ZAP é dinâmico e exercita JWT, validação de entrada e cabeçalhos na API real. As duas primeiras são plugin do Maven e rodam com um comando.

**Porquê:** juntas cobrem o que o enunciado pede sem acrescentar esteira de qualidade, que os professores classificaram como além do escopo do MVP.

**Declarado:** o resultado do SpotBugs entra no relatório **com triagem escrita**, e não como contagem bruta. A ferramenta marca padrões, não defeitos: validação no construtor, coleção exposta em DTO e rota de Spring aparecem como achado por construção e são justamente as escolhas de projeto declaradas nos outros ADRs. O que a leitura de código encontra e a ferramenta não vê entra no relatório à mão, e é assim que o segredo de assinatura versionado para o ambiente local está registrado.

---

## ADR-005: JUnit Jupiter, Mockito, Testcontainers e JaCoCo com gate

**Decisão:** JUnit Jupiter com Mockito no unitário; Spring Boot Test com Testcontainers e PostgreSQL real na integração; JaCoCo com gate que falha o build abaixo de 80%, restrito aos pacotes `domain` de cada contexto mais o `shared.domain`.

**Fundamento de negócio:** regressão de cobertura no domínio devolve saldo e status errados à operação da oficina, e um gate no build transforma "temos 80%" em fato que qualquer um verifica com um comando.

**Fundamento técnico:** o Testcontainers sobe o mesmo PostgreSQL do ambiente, então o teste exercita o dialeto e as funções de data de verdade. O gate mira `domain` porque o requisito é 80% nos domínios críticos; medir o projeto inteiro diluiria o número com controladores e configuração.

**Porquê:** é o único arranjo em que o requisito numérico não depende de disciplina humana.

**Alternativa recusada:** banco em memória na integração. Mente no dialeto, e teste verde ali com vermelho no banco real é o pior resultado possível.

---

## ADR-006: Fronteiras dos agregados de Cadastro e o Orçamento versionado

**Decisão:** três fronteiras. **Veículo é agregado próprio**, com identidade por ID e Placa como atributo único e alterável. **Orçamento é entidade com versões dentro da OS**, cada versão imutável depois de enviada, carregando a própria aprovação ou reprovação. **Cliente é um único agregado**, com o Documento como objeto de valor que valida CPF ou CNPJ na construção.

**Fundamento de negócio:** o balcão identifica o cliente pelo Documento e o carro pela Placa, e trata os dois como cadastros independentes. O orçamento aprovado é compromisso com o consumidor, então o que foi aprovado não pode ser sobrescrito.

**Fundamento técnico:** a Placa muda no mundo real, por transferência ou pela conversão ao padrão Mercosul, e identidade de agregado não pode mudar. Agregados pequenos com referência por identidade reduzem contenção e mantêm invariantes locais. Cada Item registra a versão do orçamento que o introduziu, e é isso que separa o escopo aprovado do reparo adicional recusado: uma versão **anterior** já reprovada não entra no escopo, e a versão corrente nunca se autoexclui, senão ela exibiria orçamento vazio justamente para o Cliente que precisa lê-lo.

**Consequência que a implementação torna visível:** como o vínculo entre item e versão é obrigatório, a **versão 1 existe como rascunho** desde a inclusão do primeiro item, com a data de envio ainda nula. **Na Fase 2 esse primeiro item pode chegar na própria abertura da OS** (ADR-026), então a versão 1 pode existir com a OS ainda em `RECEBIDA`; o que não muda é o envio ao cliente, que segue amarrado à conclusão do diagnóstico. Isso é compatível com a decisão, que declara a versão imutável **depois de enviada**, e não depois de criada.

**Porquê:** a fronteira cai onde a linguagem do negócio muda. O Veículo significa algo fora do contexto do Cliente, então não pertence a ele; e o Orçamento reprova no critério de objeto de valor, porque exatamente o que ele não pode ser é substituído.

**Alternativa recusada:** dois agregados de Cliente, um para pessoa física e outro para jurídica. Dobraria CRUD, repositório e endpoints sem uma única regra de negócio que difira no MVP.

---

## ADR-007: Interação do cliente por Código de acompanhamento

**Decisão:** o JWT protege apenas a superfície administrativa. O Cliente consulta e responde o orçamento por um **Código de acompanhamento**, token opaco de 128 bits de aleatoriedade criptográfica, comparado por igualdade exata, com 404 uniforme para código inexistente. O orçamento chega por e-mail, e o ambiente local sobe uma caixa de e-mail própria, sem provedor externo nem credencial.

**Fundamento de negócio:** o cliente precisa ser avisado e aprovar sem criar conta. Menos atrito significa aprovação mais rápida e carro menos tempo parado no pátio, que é a dor que o sistema existe para curar.

**Fundamento técnico:** separar a superfície pública da administrativa reduz a área de ataque de cada uma. O padrão é o de capability URL: token não adivinhável, validado no servidor a cada chamada, com escopo de uma única OS. O poder de aprovar expira pela máquina de estados, porque só existe com a OS em Aguardando aprovação.

**Porquê:** cumpre o enunciado com o mínimo de peças móveis, e a disciplina de Desenvolvimento Seguro ensina exatamente esse padrão de token validado a cada uso.

**Declarado:** o poder de **leitura** não expira no MVP, e o portador do código é tratado como o Cliente. É a premissa de qualquer link de rastreio. A caixa de e-mail é demonstração do ambiente, não feature.

---

## ADR-008: Máquina de estados da OS e a métrica de tempo

**Decisão:** o status **Cancelada** entra como sétimo, desfecho terminal. A reprovação é transição automática, decidida por um guard de uma pergunta: **existe versão aprovada nesta OS?** Se não existe, a OS vai a Cancelada; se existe, volta a Em execução e o Mecânico conclui o escopo já aprovado. O **Tempo médio de execução** é a média por OS do tempo em Em execução, **somando os segmentos** quando o reparo adicional faz a OS reentrar nesse estado.

**Fundamento de negócio:** o gestor promete prazo com base no tempo que a oficina controla, não no tempo que o cliente leva para responder. E a OS reprovada precisa sair da fila, senão a fila de aprovação nunca esvazia.

**Fundamento técnico:** a Transição de status grava data e hora de todas as transições, então qualquer recorte futuro é uma agregação, sem mudança de modelo. O guard é formulado por "existe versão aprovada" e não por "é a primeira versão" porque é a lei que protege o que foi **aprovado**, não a ordem de emissão.

**Porquê:** o enunciado aponta o recorte ao dizer tempo médio de **execução**, e a permissão para um sétimo status é condicionada a justificá-lo pelo fluxo, que é o caso.

**Semântica declarada do filtro por Serviço:** ele devolve a **média das OSs que INCLUEM aquele serviço**, e não o tempo do serviço isolado. Medir o serviço isolado exigiria o Mecânico apontar início e fim por item, comandos que o enunciado não pede.

**Na fila de atendimento, Cancelada é tratada como as outras encerradas** (ADR-026): fica fora da listagem sem filtro e continua respondendo pela consulta por identificador e pelo filtro de status. A OS reprovada sai da fila, que é o efeito que esta decisão queria.

**Renegociação fica fora do MVP.** Reprovação é desfecho: se o cliente quiser renegociar, isso acontece antes de formalizar a reprovação, e o retorno dele depois gera OS nova. A estrutura de versões já suporta a renegociação no dia em que ela for requisito.

**Alternativa recusada:** medir de Recebida a Entregue. Misturaria a espera do cliente com a produtividade da oficina, e a média ficaria refém do cliente lento.

**Declarado:** a validade de dez dias do Orçamento é **informativa**, e a resposta do Cliente continua aceita depois de vencida. O CDC fixa prazo para o orçamento vincular o fornecedor, não para extinguir a manifestação do consumidor, e recusar a aprovação tardia deixaria a OS num estado sem saída pela API.

**Fora do MVP e declarado:** o **disparo** do Lembrete de aprovação. Ele está modelado como política no Event Storming, com o critério de três dias sem resposta dentro da validade, e não existe como código nem agendador. O que está entregue é a validade de dez dias, exibida na consulta.

---

## ADR-009: Autenticação como subdomínio Genérico

**Decisão:** a Autenticação é contexto delimitado classificado como subdomínio **Genérico**, com investimento mínimo: um agregado Usuário, credenciais para o JWT administrativo e a validação do token como filtro de borda. Um único perfil, e a senha usa BCrypt direto.

**Fundamento de negócio:** autenticação não diferencia a oficina. Não é onde mora a vantagem competitiva, é infraestrutura de acesso.

**Fundamento técnico:** o Spring Security resolve o JWT na borda e o domínio permanece limpo. Os papéis do negócio (Atendente, Mecânico, Estoquista) existem como atores no modelo, e **não** como perfis de acesso, porque o enunciado pede autenticação e não autorização por papel.

**Porquê:** a disciplina dá "funções de autenticação" como exemplo de subdomínio Genérico e prescreve comprar solução de mercado para genéricos. Spring Security e JWT são essa solução.

**Alternativa recusada:** contexto pleno com padrão formal de integração. Sem tradução de modelo entre contextos do mesmo processo e do mesmo dono, seria cerimônia sem conteúdo, e cada hora ali é hora tirada do núcleo.

---

## ADR-010: Chassi fora do MVP

**Decisão:** o Veículo não tem chassi.

**Fundamento de negócio:** nenhum fluxo do enunciado usa chassi. O atendimento identifica o carro pela Placa, no balcão.

**Fundamento técnico:** incluí-lo exigiria um objeto de valor com validação de VIN, 17 caracteres sem I, O e Q e dígito verificador, sem nenhum consumidor no domínio. Código sem consumidor só pode envelhecer.

**Porquê:** teto de escopo. O que o enunciado não pede, não entra.

---

## ADR-011: Nome do projeto e identidade da entrega

**Decisão:** o projeto e o repositório se chamam `oficina-mecanica-api`.

**Fundamento de negócio:** o repositório é público e serve também como portfólio. Um nome que anuncia "trabalho de faculdade" desperdiça um projeto bom no primeiro filtro de quem lê.

**Fundamento técnico:** o nome amarra repositório, imagem Docker, artefato Maven e o link da entrega. Nomear o **sistema**, e não a fase, faz o repositório sobreviver às fases seguintes, que evoluem esta mesma aplicação sem renomear com histórico publicado.

**Porquê:** equilibra os dois públicos, quem lê o domínio entende de imediato e quem lê o repositório como portfólio não descarta.

---

## ADR-012: Idioma do código decidido pelo significado

**Decisão:** **nomeia algo que existe ou acontece no negócio da oficina? Português. Nomeia como o software implementa, transporta, persiste ou organiza algo? Inglês.** Na prática: `AprovarOrcamento` fica em português mesmo morando em `application`, e `Repository` fica em inglês mesmo servindo ao domínio. Pacotes de contexto em português, pacotes de camada em inglês. Identificadores sem acento; o acento vive em texto e em dados.

**Fundamento de negócio:** o modelo foi descoberto em português, que é a língua em que o negócio da oficina é falado. Manter o mesmo termo do glossário até o código elimina a camada de tradução entre o que foi modelado e o que foi implementado, que é exatamente o que a Linguagem Ubíqua existe para eliminar.

**Fundamento técnico:** a fronteira semântica é objetiva, ao contrário de uma fronteira por camada ou por pasta. `Controller`, `Repository` e `domain` têm significado consolidado na engenharia e não pertencem ao vocabulário da oficina; traduzi-los custaria legibilidade sem ganhar precisão.

**Porquê:** o critério é para quem este código fala. Traduzir o domínio criaria uma segunda representação a manter, e parte das traduções não é mecânica: "Orçamento" é `Estimate` ou `Quote` conforme o quanto vincula a oficina, o que faria da tradução uma decisão de modelagem disfarçada.

**Alternativa recusada:** inglês em tudo, com o glossário traduzindo. O ganho de legibilidade internacional é menor do que parece, porque um repositório com código em inglês e Event Storming, Context Map e Storytelling em português continua inacessível a um leitor estrangeiro.

---

## ADR-013: Fluxo de estoque do MVP

**Decisão:** três regras. Com **estoque insuficiente**, a reserva separa o que existe e abre pendência do que faltou, na mesma operação, e a OS **segue Em execução**, sem status "Aguardando peças". A **baixa acontece na retirada** pelo Mecânico, não na finalização da OS. A **retirada e a devolução identificam a reserva** e não a peça, e movem a reserva inteira; a devolução é manual e aceita até a OS Entregue.

**Fundamento de negócio:** o saldo de estoque é dinheiro, e é dele que sai a decisão de compra. A oficina perde venda quando promete peça que não tem. O que já está na prateleira fica separado para aquela OS, e a pendência pede exatamente o que falta. E sobra de material aparece na conferência física, que muitas vezes acontece depois de o carro sair.

**Fundamento técnico:** a invariante do agregado Peça é o saldo nunca negativo, e ela só se sustenta se cada movimento for comando explícito na raiz, dentro de transação. A Reserva tem ciclo de vida próprio (ativa, consumida, devolvida), e é a situação dela que decide o efeito da devolução: por isso a operação identifica a reserva, e não a peça, já que a mesma peça pode ter duas reservas na mesma OS em situações diferentes.

**Porquê:** resolve o que o enunciado pede com o menor artefato que resolve, e registra por escrito o que ficou de fora.

**Alternativa recusada:** baixa na finalização da OS. Deixaria o saldo mentindo durante toda a execução, que é exatamente a dor que o sistema existe para curar.

**Fora do MVP e declarado:** o **Alerta de reposição**, que está modelado como política automática nos diagramas e não existe no código; o que está entregue é a consulta de peças abaixo do estoque mínimo, que é sob demanda e não é o alerta. E a **resolução da pendência**, que nasce pelos dois caminhos e alimenta a decisão de compra, sem nada que a feche automaticamente.

---

## ADR-014: Remoção lógica nos cadastros

**Decisão:** os quatro cadastros têm coluna `ativo`, e o DELETE marca como inativo. O registro inativo some das listagens e o detalhe responde 404, sem expor a coluna. O DELETE é **recusado com 409** enquanto houver Ordem de Serviço em andamento referenciando o registro, mais Reserva ativa no caso da Peça. **Não há reativação no MVP.**

**Fundamento de negócio:** a oficina precisa tirar do balcão um cliente que não atende mais e uma peça que saiu de linha, sem perder o histórico do serviço já prestado. Ordem de Serviço encerrada é documento de uma relação de consumo, e não pode virar registro órfão porque alguém limpou um cadastro.

**Fundamento técnico:** os quatro são referenciados por chave estrangeira a partir de tabelas de histórico. A remoção lógica preserva a integridade referencial ao custo de uma coluna e de um predicado nas leituras, e o guard do 409 impede inativar registro em uso corrente.

**Porquê:** é a única alternativa que entrega o D do CRUD de forma demonstrável **e** preserva o histórico. Remoção física em cascata destruiria Ordens de Serviço encerradas; com `RESTRICT`, o cadastro ficaria indelével na prática e o requisito não seria demonstrável.

**Esta decisão vale para os quatro cadastros e não para a Ordem de Serviço.** A exclusão lógica que a Fase 2 pede na listagem de OS é por **status**, sem coluna nova, e está no ADR-026: a OS não tem coluna `ativo` e nada aqui muda por causa dela.

**Declarado:** um Documento ou uma Placa de registro inativado ficam **reservados**, porque a chave única não distingue ativo de inativo e não existe reativação. Inativar um Cliente não inativa os Veículos dele. E a **remoção, a alteração e o uso pela Ordem de Serviço disputam a mesma linha**, o que impede tanto ressuscitar um cadastro removido quanto inativar um cadastro enquanto uma OS passa a usá-lo. A serialização entre remoção e alteração é a mesma nos quatro cadastros, e **nos quatro ela tem prova por mutação**: removida a leitura travada do caso de uso de alteração, um teste de concorrência quebra. Em Cliente, Veículo e Serviço a prova vem da disputa entre remoção e alteração; na Peça, da disputa entre a alteração do cadastro e a Entrada de estoque. **O que a trava impede é a perda de escrita do ADR-019:** sem ela, a alteração grava o agregado inteiro por cima do que a operação concorrente acabou de confirmar.

---

## ADR-015: Dados de demonstração fora do fluxo de migrations

**Decisão:** o arquivo de dados de demonstração vive em `seed/`, fora do classpath da aplicação, e é aplicado por um serviço próprio do `docker-compose.yml`, que espera o schema aparecer, carrega uma vez e sai. A carga é **idempotente** e a espera é **limitada**.

**Fundamento de negócio:** quem opera a oficina precisa de um comando e um ambiente pronto para experimentar o sistema antes de confiar nele. E a estrutura do repositório passa a contar a verdade sobre o que é estrutura e o que é dado de exemplo, leitura que se faz em segundos, antes de qualquer documento ser aberto.

**Fundamento técnico:** migration descreve a evolução da **estrutura**, é versionada e imutável depois de aplicada; seed é dado de **um** ambiente e muda sempre que o cenário muda. Tratar o seed como migration põe dado fictício no histórico de schema e sujeita o arquivo a uma imutabilidade que ele não pode honrar.

**Porquê:** é a única forma de o ambiente subir populado **sem** mentir sobre a natureza do arquivo.

**Alternativa recusada:** carga pelo mecanismo nativo de inicialização da imagem do banco. Ela roda antes de a aplicação subir, e portanto antes de o Flyway criar as tabelas, então quebraria por tabela inexistente.

---

## ADR-016: Política de versão, sempre em versão suportada

**Decisão:** onde existe LTS, usar o LTS corrente, o que vale para o Java. Onde não existe, usar uma versão dentro da janela de suporte, o que vale para o Spring Boot, que não designa nenhuma versão como LTS. Onde o suporte é longo, ficar na corrente quando a troca for barata, o que vale para o PostgreSQL. **A política é reavaliada a cada fase, porque as janelas de suporte mudam sozinhas.**

**Fundamento de negócio:** o sistema guarda dado de cliente da oficina, e CVE conhecida sem correção é risco direto sobre esse dado. Entregar CVEs críticas conhecidas, sem correção disponível, num projeto novo e sem restrição de legado, não tem defesa, porque não existe custo de migração a alegar.

**Fundamento técnico:** o eixo que decide não é "novo contra antigo", é **"com correção contra sem correção"**. Framework fora de suporte acumula CVE por construção, e nenhuma delas será corrigida. O caminho conservador chega na mesma conclusão, porque quem quer estabilidade escolhe a versão suportada mais madura.

**Porquê:** o ônus da prova inverte. Usar a versão corrente é o comportamento padrão de quem inicia um projeto; usar uma versão sem manutenção é que exigiria justificativa.

**Reavaliação da Fase 2:** a política mandou reavaliar, e a reavaliação **mantém as três versões**. O Java 25 é o LTS corrente, o Spring Boot 4.1 está dentro da janela de suporte e o PostgreSQL 18 é a major corrente, que é exatamente o que a decisão pede em cada um dos três eixos. **Nenhuma versão sobe nesta fase**, e o gatilho para reabrir isto é CVE crítica com correção disponível, medida pelo perfil de análise de dependências, não impressão.

**Fora desta política:** ferramenta de build, que não é superfície de ataque do produto entregue.

---

## ADR-017: Dinheiro replicado por contexto, sem kernel compartilhado

**Decisão:** cada contexto que precisa de dinheiro declara o próprio `Dinheiro` no seu pacote de domínio, com a validação no construtor. O pacote `shared` fica restrito ao que **não** é domínio de negócio, e nenhum conceito do glossário mora nele.

**Fundamento de negócio:** preço de peça e valor de mão de obra são preços de coisas diferentes, formados por gente diferente. O preço da peça vem do fornecedor e muda com a nota de compra; o valor da mão de obra é tabela da oficina. Amarrar os dois ao mesmo tipo faz uma decisão comercial de um lado atravessar o outro.

**Fundamento técnico:** contexto delimitado como pacote só entrega isolamento se o pacote for autocontido. Um objeto de valor comum transforma pacotes independentes num grafo com raiz compartilhada, que é justamente o acoplamento que o Context Map existe para tornar visível.

**Porquê:** o custo da cópia é um `record` de poucas linhas, pago uma vez. O custo do kernel compartilhado é permanente e cresce com o número de contextos, porque toda mudança nele passa a exigir concordância de todos.

**Consequência que é instrumento de leitura:** divergência entre as **regras de validação** das cópias é a informação de que a regra monetária de um contexto deixou de ser a do outro. Aritmética de apoio pode divergir sem significar nada.

---

## ADR-018: Identidade de Serviço e Peça sem chave natural

**Decisão:** Serviço e Peça têm identidade por ID e **nenhuma chave natural**. Nome é atributo descritivo, livre e alterável, e cadastrar dois registros com o mesmo nome não é erro.

**Fundamento de negócio:** o balcão reconhece o cliente pelo documento e o carro pela placa porque são identidades que o mundo já atribuiu. Peça e serviço não têm isso. Duas peças chamadas "Filtro de óleo", de fabricantes diferentes e preços diferentes, é caso corriqueiro de oficina.

**Fundamento técnico:** identidade de agregado precisa ser imutável, e nome de catálogo é o atributo que mais muda, por fornecedor, por marketing e por correção de digitação.

**Porquê:** unicidade é invariante de negócio, não higiene de cadastro, e nenhuma fonte do trabalho pede que dois serviços não possam ter o mesmo nome.

**Alternativa recusada:** chave única no nome. Combinada com a remoção lógica sem reativação do ADR-014, ela reservaria o nome **para sempre**, e corrigir um cadastro errado apagando e recriando ficaria impossível.

---

## ADR-019: Concorrência no agregado Peça

**Decisão:** a linha da Peça é o **ponto único de serialização** do agregado, com trava pessimista adquirida antes de qualquer decisão, e a leitura travada é a autoritativa. **Quando uma requisição toca mais de uma Peça, a aquisição é ordenada pelo identificador da Peça.**

**Fundamento de negócio:** o saldo de estoque é dinheiro. Um movimento perdido não aparece como erro: aparece como peça que sumiu da prateleira ou apareceu do nada, e a oficina só descobre no inventário.

**Fundamento técnico:** o isolamento efetivo aqui é READ COMMITTED, e **nele transação não impede perda de escrita**: duas transações que leem o mesmo saldo e gravam valores derivados dessa leitura sobrescrevem uma à outra sem erro nenhum. E `CHECK` protege invariante, não protege valor, porque segue satisfeito por números coerentes entre si e errados em relação ao mundo. Travar a raiz cobre o agregado inteiro, porque as entidades internas só mudam através dela. A ordenação total sobre o recurso travado é o que elimina o ciclo de espera.

**Porquê:** é a única alternativa que mantém a regra dentro do domínio **e** o valor correto. Um `UPDATE` condicional atômico funcionaria, e mudaria a regra de lugar: a invariante passaria a viver na cláusula SQL, e o domínio deixaria de ser onde a regra mora.

**Consequência aceita:** operações sobre a mesma Peça passam a ser seriais. O volume é de unidades de linhas, e o custo de errar o saldo é maior que o de esperar.

---

## ADR-020: Concorrência no agregado Ordem de Serviço

**Decisão:** a linha da Ordem de Serviço é o ponto único de serialização do agregado, e as oito operações que mudam status, itens ou versões do orçamento adquirem a trava antes de decidir. **Quem perde a corrida cai na máquina de estados**, recebendo o 409 que ela já produzia em uso sequencial. As consultas **não** travam. Mas **leitura que decide trava**: os guards que o Estoque faz sobre o status da OS são pré-condição de escrita, não exibição.

**Fundamento de negócio:** o que se perde sem isso não aparece como erro. Aparece como um orçamento que o cliente aprovou por um valor e a oficina executou por outro, como um comprovante de cancelamento de um serviço que está sendo feito, e como um histórico de status que a própria máquina declara impossível.

**Fundamento técnico:** valem as mesmas duas premissas do ADR-019. Medido sem trava: quatro aprovações simultâneas do mesmo código respondiam 200 nas quatro e reservavam peça em quádruplo; quatro inclusões de item deixavam o total do orçamento menor que a soma dos próprios itens. **A ordem de aquisição é sempre Ordem de Serviço antes de Peça**, e **Cliente antes de Veículo** nos caminhos que tocam os dois, o que preserva a ordenação total que elimina o impasse. A segunda metade tem custo medido: sem ela, alterar o proprietário de um veículo enquanto nasce uma Ordem de Serviço para o mesmo par produz impasse no banco e resposta 500, porque gravar a chave estrangeira trava a linha do Cliente depois da do Veículo.

**Porquê:** é a mesma escolha do ADR-019 pelo mesmo motivo. Aplicar uma decisão existente a um segundo agregado é mais barato de defender do que inventar um segundo padrão de concorrência no mesmo projeto.

**Declarado:** a trava está coberta por teste de concorrência com prova por mutação nas operações da OS. O guard de leitura que decide é exceção: o estado final de uma corrida legítima é indistinguível do estado final da corrida sem trava, então o teste afirma o que é observável e **não** serve de prova por mutação.

---

## ADR-021: A camada como unidade de encapsulamento

**Decisão:** a camada dentro do contexto delimitado é a unidade de organização e de encapsulamento. **Nenhuma das quatro camadas tem subpacote**, e a distinção entre tipos de arquivo é feita pelo nome da classe. A camada de infraestrutura mantém suas classes package-private.

**Fundamento de negócio:** o encapsulamento por camada é o que garante a regra de que **somente a lógica do agregado altera o próprio estado**. Como a classe de persistência não é visível fora da sua camada, nenhuma outra camada consegue importar a entidade JPA, e por isso a tabela só é alcançada pelo adaptador.

**Fundamento técnico:** em Java, uma declaração sem modificador de acesso é acessível apenas dentro do pacote que a contém, e **subpacote é outro pacote**. Os tipos package-private da infraestrutura sobrevivem exatamente porque cada camada é um pacote único. Subdividir os tornaria públicos.

**Porquê:** entre uma garantia que o compilador verifica e uma arrumação que só o olho verifica, a decisão fica com o compilador.

**Consequência aceita:** o maior pacote do projeto fica com 32 arquivos, e navegar nele exige conhecer o sufixo dos nomes, não abrir pastas.

---

## ADR-022: Transições disparadas por endpoint de comando, não por escrita de status

**Decisão:** cada transição da Ordem de Serviço tem um endpoint próprio que nomeia o **evento do negócio** (`POST /{id}/diagnostico/inicio`, `/diagnostico/conclusao`, `/execucao/conclusao`, `/entrega`, `/reparos-adicionais`, mais aprovação e reprovação na superfície pública). **Nenhuma rota recebe o status como dado de entrada de escrita.** Onde o status aparece na entrada é só como filtro de leitura da fila, em `GET /ordens-servico?status=`.

**Fundamento de negócio:** o Atendente não escolhe um status, ele registra um fato que aconteceu na oficina. "Concluí o diagnóstico" é o que ele faz; "Aguardando aprovação" é a consequência.

**Fundamento técnico:** o enunciado exige que o status mude **automaticamente** conforme o andamento. Um `PATCH /{id}/status` transferiria a máquina de estados para o cliente da API, e a validação da transição viraria conferência de um valor que veio de fora. Com endpoint de comando, `transicionarPara` é privado ao agregado e a máquina de estados é a única autoridade: o método de transição não tem chamador fora da raiz.

**Porquê:** é a diferença entre uma API que expõe o modelo e uma API que expõe o negócio. A segunda é o que a linguagem ubíqua pede, e é ela que torna o requisito de automatismo verificável em vez de prometido.

**Alternativa recusada:** um único `PATCH /ordens-servico/{id}/status` recebendo o status de destino. É menos código e é exatamente o que o enunciado não quer, porque deixa o cliente escrever o estado.

---

## ADR-023: O envio do Orçamento é atômico com a transição que o gera

**Decisão:** o envio do Orçamento ao Cliente acontece **dentro da mesma transação** que grava a transição de status, na conclusão do diagnóstico e no registro de reparo adicional. Falha no envio derruba a transação inteira, e a Ordem de Serviço permanece no status anterior.

**Fundamento de negócio:** o Código de acompanhamento só chega ao Cliente pelo e-mail. Uma Ordem de Serviço que entrasse em Aguardando aprovação sem o e-mail ter saído ficaria esperando a resposta de alguém que nunca foi avisado, e não existe rota para reenviar. O estado "aguardando quem não sabe" é pior que o erro visível.

**Fundamento técnico:** medido com o serviço de e-mail derrubado: a operação responde 500 e o banco volta ao estado anterior, sem transição gravada e sem orçamento com data de envio. Repetida com o serviço no ar, a mesma chamada responde 200. O comportamento é conservador por construção, e não deixa estado parcial.

**Porquê:** entre falhar visivelmente e avançar em silêncio para um estado sem saída, a escolha é falhar.

**Alternativa recusada:** gravar a transição e notificar depois, fora da transação. É o padrão correto quando existe reenvio, fila ou retentativa; sem nenhum dos três, ele troca um erro que o operador vê por uma Ordem de Serviço travada que ninguém percebe. Mensageria e agendador estão fora do escopo do MVP.

---

## ADR-024: Cluster Kubernetes local, provisionado por Terraform

**Decisão:** o cluster Kubernetes desta fase é **local**, criado com `kind` e provisionado pelo Terraform. Nenhum recurso é criado em provedor de nuvem, e o banco de dados provisionado pelo Terraform também é local.

**Fundamento de negócio:** o valor que esta fase precisa demonstrar é o ciclo completo, provisionar, publicar, escalar e observar a aplicação escalando. Esse ciclo é o mesmo com ou sem nuvem, e a nuvem acrescenta ao caminho crítico três coisas que não são o objeto da avaliação: identidade e acesso, rede virtual e faturamento. O enunciado da fase autoriza as duas formas de maneira explícita, ao pedir *"provisionamento do cluster Kubernetes (local ou cloud)"*, então a forma local satisfaz o requisito pela letra.

**Fundamento técnico:** `kind` executa um cluster Kubernetes conforme dentro de contêineres Docker, e é mantido pelo próprio SIG de testes do Kubernetes, o que o torna a implementação local mais próxima de um cluster real. O `metrics-server` roda nele, e sem métrica de CPU e memória o Horizontal Pod Autoscaler não escala, o que tornaria a demonstração de escalabilidade impossível. O Terraform gerencia o cluster pelo provider do `kind` e os objetos pelos providers `kubernetes` e `helm`, de modo que o mesmo `apply` que existe contra a nuvem existe aqui.

**Porquê:** a diferença decisiva não é custo, é **quem controla o ambiente no momento da gravação**. Um cluster local sobe do zero em minutos, sempre igual, sem depender de quota, de região ou de conta. Um cluster gerenciado esquecido ligado durante os dois meses da fase custaria mais que a fase inteira, e o risco de esquecer não é hipotético em uma rotina com agenda imprevisível.

**Consequência aceita:** o executor hospedado do provedor de CI não alcança um cluster que roda na máquina do desenvolvedor, então a etapa de publicação do pipeline precisa de um executor com acesso ao cluster. E o desenho da arquitetura entregue descreve um ambiente local, não um ambiente de produção em nuvem, o que precisa estar dito no README para que documento e ambiente não se contradigam.

---

## ADR-025: Clean Architecture, com o framework fora dos casos de uso

**Decisão:** a aplicação adota **Clean Architecture** nos quatro anéis que os quatro pacotes de cada contexto já formam, e o framework sai do anel dos casos de uso: o pacote `application` não importa nada do Spring. O registro dos casos de uso como bean e a declaração de transação passam para a infraestrutura, e a regra de dependência passa a ser provada pelo build. **Este ADR substitui o ADR-003**, que declarava monolito em camadas e recusava a Clean Architecture para o MVP da Fase 1.

**Fundamento de negócio:** esta mesma aplicação vai ser evoluída nas fases seguintes do curso, e a tabela comparativa da disciplina de Arquitetura de Software indica a Clean Architecture justamente para *"projetos com expectativa de longo prazo, mudanças tecnológicas e alta exigência de qualidade"*, listando *"independência de frameworks"* entre os benefícios. O custo que a mesma tabela cobra, *"verbosidade"* e *"maior esforço inicial de estruturação"*, é pago uma vez e aproveitado por três fases.

**Fundamento técnico:** o artigo de Robert C. Martin publicado em 13 de agosto de 2012, que a disciplina indica como fonte, nomeia quatro círculos, Entities, Use Cases, Interface Adapters e Frameworks and Drivers, e enuncia a regra de dependência assim: *"Source code dependencies can only point inwards. Nothing in an inner circle can know anything at all about something in an outer circle."*, e adiante, de forma literal: *"The name of something declared in an outer circle must not be mentioned by the code in an inner circle."* A apostila diz o mesmo em uma linha, *"as dependências de código devem sempre apontar para o centro"*, e atribui a inversão de dependência ao fato de que *"frameworks e ferramentas dependem do domínio, e não o contrário"*.

Os quatro pacotes de cada contexto já eram esses quatro círculos, e a direção das dependências já era obedecida: nenhuma das classes de `domain` importa framework, borda ou infraestrutura. O que faltava estava em um lugar só: as anotações `@Service` e `@Transactional` nos casos de uso. Anotação é o nome de algo declarado no círculo externo escrito dentro do círculo interno, que é exatamente o que a segunda frase do artigo proíbe.

**O grau, declarado:** os 41 casos de uso não têm anotação nenhuma e não importam Spring. Cada contexto ganha na infraestrutura uma `@Configuration` com um método `@Bean` por caso de uso, que é o círculo onde o artigo põe a cola com o framework. A transação é declarada em `shared/infrastructure`, por classe e não por nome de método, com a máquina padrão do Spring: uma fonte de atributos de transação própria mais o advisor. Todo caso de uso é transacional, os 14 de consulta em `readOnly`, listados nominalmente, e `AutenticarUsuarioUseCase` fora de transação, que é o que as anotações anteriores diziam. A dependência do `PasswordEncoder` do Spring Security sai do caso de uso por uma porta, `VerificadorDeSenha`, implementada na infraestrutura com BCrypt, mantendo o ADR-009.

**O que prova, e é execução e não promessa:** um teste de arquitetura afirma que `domain` e `application` não dependem de framework, de borda nem de infraestrutura, e que a borda não depende da infraestrutura. Um teste de integração lista os casos de uso que existem no código, exige um bean para cada um, e afirma o atributo de transação de cada bean: quem criar um caso de uso e esquecer de registrá-lo ou de declarar a transação derruba o build.

**Os nomes do artigo neste código:**

| Nome no artigo | Onde vive aqui |
|---|---|
| Entities | pacote `domain` de cada contexto: agregados, objetos de valor e as exceções de invariante |
| Use Cases | pacote `application`: as classes `*UseCase`, um método `executar` cada |
| Interface Adapters, Controllers | pacote `api`: os `*Controller` |
| Interface Adapters, Presenters | pacote `api`: os `*Response`, com o `static de(...)` que traduz o agregado para a borda |
| Interface Adapters, Gateways | as portas de saída declaradas em `domain` e `application`, e os adaptadores JPA que as implementam em `infrastructure` |
| Frameworks and Drivers | as classes `*Config` da infraestrutura, mais Tomcat, JPA e Flyway |

**Porquê:** é a forma de ser Clean de fato que custa menos sobre 212 arquivos de produção e preserva três decisões inteiras: o ADR-021, porque nenhum pacote é renomeado nem ganha subpacote; o ADR-005, porque o gate de cobertura mira `*.domain` pelo nome; e o ADR-012, porque nenhuma classe é renomeada.

**Alternativa recusada:** Arquitetura Hexagonal. Ela cabe nos mesmos quatro pacotes e custaria praticamente o mesmo, porque as portas de saída já existem e `api` e `infrastructure` já são adaptadores. Recusada porque a tabela da disciplina indica a Hexagonal para *"sistemas que precisam se comunicar com múltiplas interfaces"*, e este sistema tem uma borda, REST, e uma persistência, PostgreSQL, e porque a Clean nomeia os anéis que os quatro pacotes já são.

**Limites declarados, dois:** o artigo desenha uma porta de entrada por caso de uso, a Use Case Input Port, e aqui não existe interface separada para isso, porque o único chamador de um caso de uso é a borda REST e a interface não teria segundo implementador nem segundo consumidor. E a configuração de segurança segue sendo a única classe de `infrastructure` que importa de `api`, limite que o ADR-003 já declarava: ela monta o corpo do 401 no envelope do contrato e conhece o prefixo das rotas, que são fatos da borda HTTP.

---

## ADR-026: As APIs da Fase 2

**Decisão:** as cinco mudanças de API que a fase pede entram sem rota nova além do que o enunciado descreve, e cada uma tem a leitura adotada declarada aqui: **(1)** os identificadores de status continuam os de hoje, com a tabela de nomes no README e no contrato; **(2)** a abertura da OS aceita serviços e peças opcionais; **(3)** as duas rotas públicas de resposta ao orçamento são a entrada da notificação externa; **(4)** a listagem sem filtro devolve a fila de atendimento ordenada por status, sem as encerradas; **(5)** toda transição de status envia e-mail ao cliente, e o envio é atômico com a transição.

**Fundamento de negócio:** as cinco mudanças descrevem o mesmo atendimento visto de fora: o cliente pede serviço no balcão, acompanha por um código, responde ao orçamento sem login, recebe aviso a cada passo, e o atendente trabalha por uma fila que mostra primeiro o que está na bancada.

### 1. Identificadores de status, e a tabela de nomes

**Decisão: os identificadores ficam.** O enunciado grafa os nomes em português corrente, e usa grafias diferentes no mesmo documento, "Execução" na consulta de status e "Em Execução" na ordenação da listagem. A API responde `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE` e `CANCELADA`, e o README e as descrições do contrato trazem a tabela que liga o nome do enunciado ao identificador.

**Fundamento técnico:** renomear custaria migration alterando três `CHECK` da primeira versão do schema, a carga de demonstração, o contrato, mais de cento e cinquenta linhas de literais em código e teste, e a Linguagem Ubíqua entregue na fase anterior passaria a divergir do código. O prefixo não é literal nem para quem escreveu o enunciado, e `DIAGNOSTICO` também não seria "Diagnóstico".

**Porquê:** o item pede que a consulta informe a situação atual, e informa; o nome do enunciado chega ao leitor pela tabela, que é onde ele procura.

### 2. Abertura da OS com serviços e peças

**Decisão:** a abertura aceita duas listas **opcionais**, de serviços e de peças, e o caso de uso inclui os itens como o pedido inicial do cliente. Sem itens, a OS nasce sem orçamento, como antes. O guard do agregado passou a aceitar item em `RECEBIDA` e em `EM_DIAGNOSTICO`.

**Fundamento de negócio:** o cliente chega dizendo o que quer, e o atendente não tem por que digitar isso duas vezes. O que ele pede na abertura é pedido, não diagnóstico: o orçamento nasce como rascunho e só vai ao cliente quando o diagnóstico é concluído.

**Fundamento técnico:** a versão 1 do orçamento continua nascendo na inclusão do primeiro item, que é o que o ADR-006 já declarava; o que mudou é que esse primeiro item pode chegar mais cedo. Nada mais no ciclo muda, porque o envio ao cliente segue amarrado à conclusão do diagnóstico.

**Alternativa recusada:** manter item só no diagnóstico e pedir ao atendente uma segunda chamada. Deixaria a letra do requisito descoberta.

### 3. Notificação externa de aprovação e de recusa

**Decisão:** as duas rotas públicas que já existem, de aprovação e de reprovação pelo código de acompanhamento, **são** a entrada da notificação externa. Nada novo se cria; o contrato e o README passam a chamá-las pelas palavras do enunciado.

**Fundamento técnico:** quem chama essas rotas está fora do sistema e porta o código, que é a credencial do cliente para responder (ADR-007 e ADR-022). O requisito pede um endpoint para receber a notificação de aprovação ou recusa, e é exatamente o que elas fazem.

**Alternativa recusada:** um endpoint único com a decisão no corpo da requisição. Traria de volta a escrita de decisão como dado, contra o ADR-022, que é a razão de as transições serem disparadas por comando e não por escrita de status.

### 4. A fila de atendimento, com exclusão lógica por status

**Decisão:** a listagem sem filtro devolve a fila ordenada por `EM_EXECUCAO`, `AGUARDANDO_APROVACAO`, `EM_DIAGNOSTICO`, `RECEBIDA` e, dentro do mesmo status, das mais antigas para as mais novas; as OSs `FINALIZADA`, `ENTREGUE` e `CANCELADA` ficam **fora** dela. Com o filtro de status, a listagem devolve as OSs daquele status, **inclusive as encerradas**.

**A exclusão é lógica, e é isso que a torna verificável:** o registro não é apagado nem marcado, continua respondendo pela consulta por identificador e reaparece quando o filtro o pede. **Sem coluna nova:** o critério é o próprio status, então o ADR-014 continua valendo apenas para os quatro cadastros e a OS segue sem coluna `ativo`.

**Fundamento técnico:** a prioridade mora no domínio, no próprio `StatusOrdemServico`, e o caso de uso da listagem apenas ordena por ela; a exclusão é declarada como o complemento dos status encerrados, então um status novo entra na fila por construção, em vez de ficar fora em silêncio.

**Porquê:** a fila existe para o atendente saber o que está na bancada agora. OS entregue não é trabalho, é histórico, e histórico se consulta, não se enfileira.

### 5. E-mail a cada transição de status

**Decisão:** **toda transição de status envia um e-mail ao cliente**, com o status novo e o endereço de acompanhamento, pela mesma porta de notificação que envia o orçamento. **O envio é atômico com a transição:** se o e-mail não sai, a transição não acontece. **Cada transição produz exatamente um e-mail:** quando a transição gera orçamento, o e-mail do orçamento é a notificação dela, porque já carrega o status novo e o mesmo endereço. **A abertura da OS também notifica**, porque é a transição que entrega o código de acompanhamento ao cliente, sem o qual a notificação externa de aprovação não tem como chegar.

**Fundamento de negócio:** o cliente da oficina liga para saber do carro. O aviso a cada passo é o que substitui o telefonema, e é por isso que ele carrega o endereço de acompanhamento em vez de só o nome do status.

**Fundamento técnico, e ele é próprio deste ADR:** a caixa de e-mail faz parte do ambiente que a entrega sobe, então o envio não depende de provedor externo nem de credencial. Falha visível é preferível a notificação perdida em silêncio, e sem fila nem retentativa a única forma de não perder é derrubar a operação. Um só caminho de código e um só de teste: quem cria um caso de uso que transiciona e esquece de notificar não tem como o teste passar, porque a contagem de e-mails é afirmada contra a contagem de transições gravadas.

**O custo assumido, declarado:** a caixa de e-mail do ambiente passa a ser dependência de toda transição, e não só das duas do orçamento. É o mesmo risco que o ADR-023 já aceitava, agora mais amplo, e ele é local: a caixa sobe no mesmo `docker compose` da aplicação.

**Por que não herdar o fundamento do ADR-023:** aquele ADR justifica a atomicidade dizendo que o cliente precisa receber o orçamento para poder agir, e esse argumento não transfere para uma transição informativa, como a entrega. O fundamento desta decisão é outro, e está escrito acima.

**A notificação lê o contato do cliente da OS mesmo com o cadastro inativado**, e a razão está no ADR-014: Ordem de Serviço encerrada é documento de uma relação de consumo e não pode virar registro órfão porque alguém limpou um cadastro. O guard da inativação recusa apagar cadastro com OS **em andamento**, mas uma OS já finalizada pode ter o cliente inativado depois, e a entrega dela precisa continuar possível. Abrir OS nova para cliente inativo segue recusado, porque essa é a superfície do cadastro, e não a de uma OS que já existe.

**O status vai no identificador da API, não em nome de exibição.** Um segundo vocabulário para o mesmo fato precisaria de dono, e a tabela do README já liga o identificador ao nome do enunciado.

**Alternativa recusada:** atomicidade só onde o e-mail habilita ação do cliente e melhor esforço nas demais. Cria duas políticas para o mesmo fato, e a segunda é a que perde aviso sem ninguém ver.
