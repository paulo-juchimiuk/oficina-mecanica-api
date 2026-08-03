-- Schema inicial. Nomes de tabela e coluna em portugues (ADR-012: o que existe
-- no negocio e nomeado em portugues); metadados de infraestrutura em ingles.

CREATE TABLE usuario (
    id          UUID PRIMARY KEY,
    login       VARCHAR(60)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(60)  NOT NULL,
    perfil      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE cliente (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    documento   VARCHAR(14)  NOT NULL UNIQUE,
    email       VARCHAR(120) NOT NULL,
    telefone    VARCHAR(20),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT documento_apenas_digitos CHECK (documento ~ '^[0-9]{11}$' OR documento ~ '^[0-9]{14}$'),
    CONSTRAINT email_nao_vazio CHECK (length(trim(email)) > 0)
);

CREATE TABLE veiculo (
    id          UUID PRIMARY KEY,
    placa       VARCHAR(7)   NOT NULL UNIQUE,
    marca       VARCHAR(60)  NOT NULL,
    modelo      VARCHAR(60)  NOT NULL,
    ano         INTEGER      NOT NULL CHECK (ano BETWEEN 1950 AND 2100),
    cliente_id  UUID         NOT NULL REFERENCES cliente (id),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT placa_antiga_ou_mercosul CHECK (placa ~ '^[A-Z]{3}[0-9]{4}$' OR placa ~ '^[A-Z]{3}[0-9][A-Z][0-9]{2}$')
);
CREATE INDEX idx_veiculo_cliente ON veiculo (cliente_id);

CREATE TABLE servico (
    id                UUID PRIMARY KEY,
    nome              VARCHAR(120)   NOT NULL,
    descricao         VARCHAR(500),
    valor_mao_de_obra NUMERIC(12, 2) NOT NULL CHECK (valor_mao_de_obra >= 0),
    moeda             VARCHAR(3)     NOT NULL DEFAULT 'BRL' CHECK (moeda = 'BRL'),
    ativo             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE peca (
    id                    UUID PRIMARY KEY,
    nome                  VARCHAR(120)   NOT NULL,
    unidade_medida        VARCHAR(20)    NOT NULL DEFAULT 'unidade',
    preco                 NUMERIC(12, 2) NOT NULL CHECK (preco >= 0),
    moeda                 VARCHAR(3)     NOT NULL DEFAULT 'BRL' CHECK (moeda = 'BRL'),
    saldo_em_estoque      INTEGER        NOT NULL DEFAULT 0,
    quantidade_reservada  INTEGER        NOT NULL DEFAULT 0,
    estoque_minimo        INTEGER        NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
    ativo                 BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT saldo_nunca_negativo    CHECK (saldo_em_estoque >= 0),
    CONSTRAINT reserva_nunca_negativa  CHECK (quantidade_reservada >= 0),
    CONSTRAINT reserva_dentro_do_saldo CHECK (quantidade_reservada <= saldo_em_estoque)
);

CREATE TABLE ordem_servico (
    id                    UUID PRIMARY KEY,
    cliente_id            UUID         NOT NULL REFERENCES cliente (id),
    veiculo_id            UUID         NOT NULL REFERENCES veiculo (id),
    status                VARCHAR(24)  NOT NULL,
    codigo_acompanhamento VARCHAR(64)  NOT NULL UNIQUE,
    relato_do_problema    VARCHAR(1000),
    criada_em             TIMESTAMP    NOT NULL,
    CONSTRAINT status_da_os CHECK (status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA'))
);
CREATE INDEX idx_os_cliente ON ordem_servico (cliente_id);
CREATE INDEX idx_os_veiculo ON ordem_servico (veiculo_id);
CREATE INDEX idx_os_status  ON ordem_servico (status);

CREATE TABLE transicao_status (
    id                UUID PRIMARY KEY,
    ordem_servico_id  UUID        NOT NULL REFERENCES ordem_servico (id),
    de_status         VARCHAR(24),
    para_status       VARCHAR(24) NOT NULL,
    data_hora         TIMESTAMP   NOT NULL,
    CONSTRAINT de_status_valido CHECK (de_status IS NULL OR de_status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA')),
    CONSTRAINT para_status_valido CHECK (para_status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA'))
);
-- Insumo do Tempo medio de execucao: a consulta varre as transicoes de uma OS
-- em ordem cronologica para somar os segmentos em Em execucao (ADR-008).
CREATE INDEX idx_transicao_os_data ON transicao_status (ordem_servico_id, data_hora);

CREATE TABLE orcamento (
    id                UUID PRIMARY KEY,
    ordem_servico_id  UUID           NOT NULL REFERENCES ordem_servico (id),
    versao            INTEGER        NOT NULL CHECK (versao >= 1),
    situacao          VARCHAR(12)    NOT NULL,
    total             NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    moeda             VARCHAR(3)     NOT NULL DEFAULT 'BRL' CHECK (moeda = 'BRL'),
    data_envio        TIMESTAMP,
    data_resposta     TIMESTAMP,
    validade_dias     INTEGER        NOT NULL DEFAULT 10 CHECK (validade_dias >= 1),
    -- Preenchida quando a versao nasce de um Reparo adicional: e o que o Cliente
    -- le antes de autorizar o servico extra.
    descricao         VARCHAR(500),
    CONSTRAINT versao_unica_por_os UNIQUE (ordem_servico_id, versao),
    CONSTRAINT situacao_do_orcamento CHECK (situacao IN ('PENDENTE', 'APROVADO', 'REPROVADO'))
);

CREATE TABLE item_servico (
    id                          UUID PRIMARY KEY,
    ordem_servico_id            UUID           NOT NULL REFERENCES ordem_servico (id),
    orcamento_origem_id         UUID           NOT NULL REFERENCES orcamento (id),
    servico_id                  UUID           NOT NULL REFERENCES servico (id),
    valor_mao_de_obra_snapshot  NUMERIC(12, 2) NOT NULL CHECK (valor_mao_de_obra_snapshot >= 0),
    moeda                       VARCHAR(3)     NOT NULL DEFAULT 'BRL' CHECK (moeda = 'BRL')
);
CREATE INDEX idx_item_servico_os ON item_servico (ordem_servico_id);
CREATE INDEX idx_item_servico_orcamento ON item_servico (orcamento_origem_id);
-- Recorte do Tempo medio de execucao por Servico: a consulta filtra por servico_id.
CREATE INDEX idx_item_servico_servico ON item_servico (servico_id);

CREATE TABLE item_peca (
    id                   UUID PRIMARY KEY,
    ordem_servico_id     UUID           NOT NULL REFERENCES ordem_servico (id),
    orcamento_origem_id  UUID           NOT NULL REFERENCES orcamento (id),
    peca_id              UUID           NOT NULL REFERENCES peca (id),
    quantidade           INTEGER        NOT NULL CHECK (quantidade > 0),
    preco_snapshot       NUMERIC(12, 2) NOT NULL CHECK (preco_snapshot >= 0),
    moeda                VARCHAR(3)     NOT NULL DEFAULT 'BRL' CHECK (moeda = 'BRL')
);
CREATE INDEX idx_item_peca_os ON item_peca (ordem_servico_id);
CREATE INDEX idx_item_peca_orcamento ON item_peca (orcamento_origem_id);
-- Guard do DELETE de peca (ADR-014): a recusa consulta itens e reservas pela peca.
CREATE INDEX idx_item_peca_peca ON item_peca (peca_id);

CREATE TABLE reserva_peca (
    id                UUID PRIMARY KEY,
    ordem_servico_id  UUID        NOT NULL REFERENCES ordem_servico (id),
    peca_id           UUID        NOT NULL REFERENCES peca (id),
    quantidade        INTEGER     NOT NULL CHECK (quantidade > 0),
    situacao          VARCHAR(12) NOT NULL,
    criada_em         TIMESTAMP   NOT NULL,
    CONSTRAINT situacao_da_reserva CHECK (situacao IN ('ATIVA', 'CONSUMIDA', 'DEVOLVIDA'))
);
CREATE INDEX idx_reserva_os ON reserva_peca (ordem_servico_id);
CREATE INDEX idx_reserva_peca ON reserva_peca (peca_id);

-- Fonte do modelo de leitura "Pendencias de pecas por OS" (ADR-013). Quando falta
-- peca com o orcamento ja aprovado, a OS SEGUE Em execucao e a falta vive aqui;
-- nao existe status "Aguardando pecas".
CREATE TABLE pendencia_peca (
    id                  UUID PRIMARY KEY,
    ordem_servico_id    UUID      NOT NULL REFERENCES ordem_servico (id),
    peca_id             UUID      NOT NULL REFERENCES peca (id),
    quantidade_faltante INTEGER   NOT NULL CHECK (quantidade_faltante > 0),
    detectada_em        TIMESTAMP NOT NULL,
    resolvida_em        TIMESTAMP
);
CREATE INDEX idx_pendencia_os ON pendencia_peca (ordem_servico_id);
CREATE INDEX idx_pendencia_peca ON pendencia_peca (peca_id);
