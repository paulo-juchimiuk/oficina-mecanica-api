# Estratégia de testes e domínios críticos

Este documento define O QUE testar, ONDE exigir os 80% e COMO medir, para que a entrega e quem a corrige meçam a mesma coisa. Ferramental no ADR-005.

## 1. Principais fluxos a testar (nomeados a partir dos Event Storming)

| # | Fluxo | Origem | Tipo de teste |
|---|---|---|---|
| F1 | Criação da OS com identificação do Cliente pelo Documento e o Veículo referenciado por identidade | Event Storming da Ordem de Serviço, fase Recepção | integração |
| F2 | Orçamento automático: Concluir diagnóstico gera e envia o Orçamento e leva a OS a Aguardando aprovação | Event Storming da Ordem de Serviço, passo 9 | unitário (agregado + SD) e integração |
| F3 | Aprovação do orçamento: transição automática para Em execução e ativação da Reserva de peças | Event Storming da Ordem de Serviço e Event Storming do Estoque | integração |
| F4a | Reprovação **sem versão aprovada**: OS vai a Cancelada, desfecho terminal, e não há reserva de peças a estornar (por construção, ver ADR-013) | Event Storming da Ordem de Serviço, passo 11 (guard) | integração |
| F4b | Reprovação **com versão aprovada** (reparo adicional recusado): OS volta a Em execução, o escopo aprovado permanece e o adicional recusado não entra | Event Storming da Ordem de Serviço, passo 11 (guard) | integração |
| F5 | Reparo adicional: nova Versão do orçamento imutável e retorno a Aguardando aprovação | Event Storming da Ordem de Serviço, passo 15 | unitário |
| F6 | Reserva e Baixa de estoque com invariante de Saldo em estoque nunca negativo e Estoque insuficiente identificado | Event Storming do Estoque | unitário (invariante) e integração (concorrência) |
| F7 | Consulta de Peças abaixo do Estoque mínimo (filtro `abaixoDoMinimo`), que é sob demanda e **não é** o Alerta de reposição, fora do MVP (ADR-013) | openapi.yaml, `GET /pecas` | integração |
| F8 | Consulta de acompanhamento pelo Código de acompanhamento (sem JWT) e recusa de código inválido | Event Storming da Ordem de Serviço, fase Aprovação | integração |
| F9a | Tempo médio por OS calculado das Transições de status, de Em execução a Finalizada | ADR-008 | integração (dados do seed) |
| F9b | **Reentrada: soma de segmentos.** OS que passou por reparo adicional (Em execução, volta a Aguardando aprovação, volta a Em execução) tem os segmentos somados, e a espera do Cliente pela aprovação NÃO entra na conta | ADR-008 | integração (dados do seed) |
| F9c | Filtro por Serviço devolve a média das OSs que **incluem** o Serviço, e o teste afirma exatamente isso, não o tempo do serviço isolado | ADR-008 | integração (dados do seed) |
| F10 | Autenticação JWT: acesso administrativo com token válido, recusa sem token e com token expirado | requisito de segurança | integração |
| F11 | Validação de dados sensíveis: CPF, CNPJ e Placa válidos e inválidos (dígito verificador, formatos) | modelo de domínio | unitário |
| F12 | CRUDs (clientes, veículos, serviços, peças) com regras de domínio por trás (documento único, placa válida, saldo) | enunciado | integração |

## 2. Domínios críticos (onde os 80% são exigidos) e justificativa

**Critério:** é crítico o pacote onde vive invariante cuja violação causa prejuízo direto (dinheiro errado, estoque errado, estado inválido) ou quebra requisito de segurança do enunciado.

| Pacote (domínio) | Por que é crítico |
|---|---|
| Agregado Ordem de Serviço (máquina de estados, versões de Orçamento, itens com snapshot) | estado inválido ou total errado do orçamento é prejuízo financeiro e legal (CDC art. 40) |
| Agregado Peça (Saldo em estoque, Reserva de peças, Baixa de estoque, Estoque mínimo) | saldo negativo ou reserva fantasma para a operação da oficina |
| VOs de validação: Documento (CPF/CNPJ) e Placa | são a "validação de dados sensíveis" exigida pelo enunciado |
| Agregados de Cadastro (Cliente e Veículo) e o VO Contato | guardam as invariantes que sustentam o A1, o A12 e o A13: Documento válido e único, Placa válida, e-mail obrigatório porque é o destino do envio do Orçamento, e a Remoção lógica do ADR-014 |
| Agregado Serviço e o VO Dinheiro | o Valor de mão de obra vira snapshot no Item de serviço e entra no total do Orçamento, então valor negativo, com mais de duas casas decimais ou acima do que a coluna persiste é dinheiro errado, que é o primeiro exemplo do critério acima |
| Agregado Usuário e a hierarquia de erros compartilhada | o Usuário é a credencial do JWT administrativo; a hierarquia de erros é o que traduz invariante violada em resposta da API, então um erro ali some com o 400, o 404 ou o 409 que o contrato promete |
| Serviços de domínio: Orçamento automático e Reserva de peças | são as automações prometidas ("alteração automática dos status" e reações a eventos). O Lembrete de aprovação está fora, porque o disparo dele ficou fora do MVP (ADR-008) e não haverá código a cobrir. **Ressalva de medição:** as duas são disparadas de dentro dos agregados e dos casos de uso, mas as PORTAS delas (`EnvioDeOrcamento` e `ReservaDePecas`) vivem em `application`, que o gate não mede. O gate morde os pacotes `domain`; a cobertura destas duas é garantida por teste explícito, e não pelo número |

**Fora da meta de 80%** (testados, mas sem gate): controllers/DTOs (borda fina sem regra), configuração de infraestrutura, mapeamentos JPA triviais. Justificativa: diluir a métrica na borda esconderia lacunas no domínio; o gate mede onde o risco mora.

## 3. O que é unitário e o que é integração NESTE projeto

- **Unitário:** exercita agregados, VOs e serviços de domínio puros, sem Spring, sem banco, sem rede, e também os casos de uso da camada de aplicação com as portas dubladas. Ex.: transição inválida da máquina de estados lança erro; CPF com dígito errado é rejeitado na construção do VO; reserva acima do saldo falha.
- **Integração:** sobe o contexto Spring e fala com PostgreSQL real via Testcontainers, atravessando controller, aplicação, domínio e repositório, ou entrando direto pelo caso de uso quando o alvo é concorrência. Ex.: POST de criação da OS persiste e retorna o Código de acompanhamento; aprovação dispara a reserva, que sobe a quantidade reservada sem mover o Saldo em estoque; endpoint de tempo médio agrega os timestamps do seed.

## 4. Como a cobertura é medida e verificada

- **Ferramenta:** JaCoCo, plugin Maven, executado em `mvn verify`.
- **Threshold configurado:** regra `check` com mínimo de 80% de linhas, medido a partir da primeira fatia que tenha código de domínio e seus testes, aplicada por pacote aos domínios críticos da seção 2 (grupo de `includes` apontando para os pacotes de domínio). Build FALHA abaixo do mínimo, que é o único arranjo em que o requisito numérico não depende de disciplina humana.
- **Relatório:** `mvn verify` gera `target/site/jacoco/index.html`; comandos e caminho documentados no `README.md`.
