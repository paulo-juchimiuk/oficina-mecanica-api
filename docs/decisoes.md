# Decisões de arquitetura (ADRs)

São 21 decisões. Cada uma traz o **fundamento de negócio**, o **fundamento técnico** e **o porquê**. Onde a alternativa recusada é o próprio argumento, ela aparece em uma linha. Onde algo ficou fora do MVP, isso está declarado, e não omitido.

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

**Alternativa recusada:** Clean Architecture já nesta fase. Aumenta o custo agora sem atender nada que o enunciado peça.

---

## ADR-004: Análise de vulnerabilidades com OWASP ZAP e Dependency-Check

**Decisão:** OWASP ZAP contra a API em execução, com leitura pelo OWASP Top 10, mais OWASP Dependency-Check sobre as dependências declaradas. As duas compõem o relatório.

**Fundamento de negócio:** o relatório de vulnerabilidades é entregável, e cobrir as duas superfícies que o sistema tem, a aplicação no ar e a cadeia de dependências, é o que torna o relatório honesto.

**Fundamento técnico:** as ferramentas medem coisas diferentes e complementares. O ZAP é dinâmico e exercita JWT, validação de entrada e cabeçalhos na API real; o Dependency-Check é de composição e casa as dependências com CVEs conhecidas. O Dependency-Check é plugin do Maven e custa uma linha de configuração.

**Porquê:** juntas cobrem o que o enunciado pede sem acrescentar esteira de qualidade, que os professores classificaram como além do escopo do MVP.

**Declarado em vez de maquiado:** nenhuma das duas faz análise estática do código escrito aqui. O que a leitura de código encontra entra no relatório à mão, e é assim que o segredo de assinatura versionado para o ambiente local está registrado.

---

## ADR-005: JUnit 5, Mockito, Testcontainers e JaCoCo com gate

**Decisão:** JUnit 5 com Mockito no unitário; Spring Boot Test com Testcontainers e PostgreSQL real na integração; JaCoCo com gate que falha o build abaixo de 80%, restrito aos pacotes `domain` de cada contexto mais o `shared.domain`.

**Fundamento de negócio:** cobertura é item avaliado, e um gate no build transforma "temos 80%" em fato verificável com um comando na frente de quem corrige.

**Fundamento técnico:** o Testcontainers sobe o mesmo PostgreSQL do ambiente, então o teste exercita o dialeto e as funções de data de verdade. O gate mira `domain` porque o requisito é 80% nos domínios críticos; medir o projeto inteiro diluiria o número com controladores e configuração.

**Porquê:** é o único arranjo em que o requisito numérico não depende de disciplina humana.

**Alternativa recusada:** banco em memória na integração. Mente no dialeto, e teste verde ali com vermelho no banco real é o pior resultado possível.

---

## ADR-006: Fronteiras dos agregados de Cadastro e o Orçamento versionado

**Decisão:** três fronteiras. **Veículo é agregado próprio**, com identidade por ID e Placa como atributo único e alterável. **Orçamento é entidade com versões dentro da OS**, cada versão imutável depois de enviada, carregando a própria aprovação ou reprovação. **Cliente é um único agregado**, com o Documento como objeto de valor que valida CPF ou CNPJ na construção.

**Fundamento de negócio:** o balcão identifica o cliente pelo Documento e o carro pela Placa, e trata os dois como cadastros independentes. O orçamento aprovado é compromisso com o consumidor, então o que foi aprovado não pode ser sobrescrito.

**Fundamento técnico:** a Placa muda no mundo real, por transferência ou pela conversão ao padrão Mercosul, e identidade de agregado não pode mudar. Agregados pequenos com referência por identidade reduzem contenção e mantêm invariantes locais. Cada Item registra a versão do orçamento que o introduziu, e é isso que separa o escopo aprovado do reparo adicional recusado: uma versão **anterior** já reprovada não entra no escopo, e a versão corrente nunca se autoexclui, senão ela exibiria orçamento vazio justamente para o Cliente que precisa lê-lo.

**Consequência que a implementação torna visível:** como o vínculo entre item e versão é obrigatório, a **versão 1 existe como rascunho** desde a inclusão do primeiro item, com a data de envio ainda nula. Isso é compatível com a decisão, que declara a versão imutável **depois de enviada**, e não depois de criada.

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

**Renegociação fica fora do MVP.** Reprovação é desfecho: se o cliente quiser renegociar, isso acontece antes de formalizar a reprovação, e o retorno dele depois gera OS nova. A estrutura de versões já suporta a renegociação no dia em que ela for requisito.

**Alternativa recusada:** medir de Recebida a Entregue. Misturaria a espera do cliente com a produtividade da oficina, e a média ficaria refém do cliente lento.

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

**Porquê:** equilibra os dois públicos, quem corrige entende de imediato e quem recruta não descarta.

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

**Declarado:** um Documento ou uma Placa de registro inativado ficam **reservados**, porque a chave única não distingue ativo de inativo e não existe reativação. Inativar um Cliente não inativa os Veículos dele. E a **remoção, a alteração e o uso pela Ordem de Serviço disputam a mesma linha**, o que impede tanto ressuscitar um cadastro removido quanto inativar um cadastro enquanto uma OS passa a usá-lo. A serialização entre remoção e alteração tem teste de concorrência com prova por mutação em Cliente, Veículo e Serviço; na Peça as duas operações já leem pela mesma consulta travada.

---

## ADR-015: Dados de demonstração fora do fluxo de migrations

**Decisão:** o arquivo de dados de demonstração vive em `seed/`, fora do classpath da aplicação, e é aplicado por um serviço próprio do `docker-compose.yml`, que espera o schema aparecer, carrega uma vez e sai. A carga é **idempotente** e a espera é **limitada**.

**Fundamento de negócio:** quem avalia precisa de um comando e um ambiente pronto. O que muda é que a estrutura do repositório passa a contar a verdade sobre o que é estrutura e o que é dado de exemplo, e essa leitura é feita em segundos, antes de qualquer documento ser aberto.

**Fundamento técnico:** migration descreve a evolução da **estrutura**, é versionada e imutável depois de aplicada; seed é dado de **um** ambiente e muda sempre que o cenário muda. Tratar o seed como migration põe dado fictício no histórico de schema e sujeita o arquivo a uma imutabilidade que ele não pode honrar.

**Porquê:** é a única forma de o ambiente subir populado **sem** mentir sobre a natureza do arquivo.

**Alternativa recusada:** carga pelo mecanismo nativo de inicialização da imagem do banco. Ela roda antes de a aplicação subir, e portanto antes de o Flyway criar as tabelas, então quebraria por tabela inexistente.

---

## ADR-016: Política de versão, sempre em versão suportada

**Decisão:** onde existe LTS, usar o LTS corrente, o que vale para o Java. Onde não existe, usar uma versão dentro da janela de suporte, o que vale para o Spring Boot, que não designa nenhuma versão como LTS. Onde o suporte é longo, ficar na corrente quando a troca for barata, o que vale para o PostgreSQL. **Reconferir antes da entrega de cada fase.**

**Fundamento de negócio:** o relatório de vulnerabilidades é avaliado por quem ensina Desenvolvimento Seguro. Entregar CVEs críticas conhecidas, sem correção disponível, num projeto novo e sem restrição de legado, não tem defesa, porque não existe custo de migração a alegar.

**Fundamento técnico:** o eixo que decide não é "novo contra antigo", é **"com correção contra sem correção"**. Framework fora de suporte acumula CVE por construção, e nenhuma delas será corrigida. O caminho conservador chega na mesma conclusão, porque quem quer estabilidade escolhe a versão suportada mais madura.

**Porquê:** o ônus da prova inverte. Usar a versão corrente é o comportamento padrão de quem inicia um projeto; usar uma versão sem manutenção é que exigiria justificativa.

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

**Fundamento técnico:** valem as mesmas duas premissas do ADR-019. Medido sem trava: quatro aprovações simultâneas do mesmo código respondiam 200 nas quatro e reservavam peça em quádruplo; quatro inclusões de item deixavam o total do orçamento menor que a soma dos próprios itens. **A ordem de aquisição é sempre Ordem de Serviço antes de Peça**, o que preserva a ordenação total que elimina o impasse.

**Porquê:** é a mesma escolha do ADR-019 pelo mesmo motivo. Aplicar uma decisão existente a um segundo agregado é mais barato de defender do que inventar um segundo padrão de concorrência no mesmo projeto.

**Declarado:** a trava está coberta por teste de concorrência com prova por mutação nas operações da OS. O guard de leitura que decide é exceção: o estado final de uma corrida legítima é indistinguível do estado final da corrida sem trava, então o teste afirma o que é observável e **não** serve de prova por mutação.

---

## ADR-021: A camada como unidade de encapsulamento

**Decisão:** a camada dentro do contexto delimitado é a unidade de organização e de encapsulamento. **Nenhuma das quatro camadas tem subpacote**, e a distinção entre tipos de arquivo é feita pelo nome da classe. A camada de infraestrutura mantém suas classes package-private.

**Fundamento de negócio:** o encapsulamento por camada é o que garante a regra de que **somente a lógica do agregado altera o próprio estado**. Como a classe de persistência não é visível fora da sua camada, nenhum outro ponto do sistema consegue escrever na tabela contornando a invariante. Saldo negativo e transição impossível deixam de depender da disciplina de quem escreve o código.

**Fundamento técnico:** em Java, uma declaração sem modificador de acesso é acessível apenas dentro do pacote que a contém, e **subpacote é outro pacote**. Os tipos package-private da infraestrutura sobrevivem exatamente porque cada camada é um pacote único. Subdividir os tornaria públicos.

**Porquê:** entre uma garantia que o compilador verifica e uma arrumação que só o olho verifica, a decisão fica com o compilador.

**Consequência aceita:** o maior pacote do projeto fica com 32 arquivos, e navegar nele exige conhecer o sufixo dos nomes, não abrir pastas.
