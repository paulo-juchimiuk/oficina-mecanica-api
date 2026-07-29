-- Inerte: so contem INSERTs. Vira V2__dados_demonstracao.sql em db/migration/
-- quando a migration de schema existir. O Flyway le apenas db/migration.
-- ============================================================================
-- Dados de demonstracao do oficina-mecanica-api
--
-- DADOS FICTICIOS DE DEMONSTRACAO. Nomes, CPFs, CNPJs, placas, e-mails e precos
-- foram gerados para avaliacao local; os digitos verificadores de CPF/CNPJ sao
-- validos por construcao, mas os numeros nao pertencem a pessoas reais.
--
-- PREMISSA A VALIDAR PELO ALUNO: este arquivo contem APENAS INSERTs. As tabelas
-- serao criadas pela ferramenta de migracao do app (premissa: Flyway). Aplicar
-- como migration de dados do proprio Flyway (ex.: R__seed.sql), DEPOIS do schema.
--
-- TABELAS E COLUNAS PRESSUPOSTAS (conferir quando o app Java existir):
--   usuario(id, login, senha_hash, perfil)
--   cliente(id, nome, documento, email, telefone)
--   veiculo(id, placa, marca, modelo, ano, cliente_id)
--   servico(id, nome, descricao, valor_mao_de_obra, moeda)
--   peca(id, nome, unidade_medida, preco, moeda, saldo_em_estoque,
--        quantidade_reservada, estoque_minimo)
--   ordem_servico(id, cliente_id, veiculo_id, status, codigo_acompanhamento,
--        relato_do_problema, criada_em)
--   item_servico(id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot)
--   item_peca(id, ordem_servico_id, peca_id, quantidade, preco_snapshot)
--   orcamento(id, ordem_servico_id, versao, situacao, total, data_envio,
--        data_resposta, validade_dias)
--   transicao_status(id, ordem_servico_id, de_status, para_status, data_hora)
--
-- Cobertura para o corretor testar sem cadastrar nada:
--   - usuario administrativo para o login JWT
--   - clientes PF e PJ, veiculos nos dois formatos de placa
--   - catalogo de servicos e pecas (uma peca ABAIXO do estoque minimo, para o
--     alerta de reposicao)
--   - uma OS em CADA status relevante, com TODAS as transicoes de status e seus
--     timestamps preenchidos (sem eles o tempo medio de execucao nao calcula)
--   - uma OS com duas versoes de orcamento (reparo adicional)
-- ============================================================================

-- Usuario administrativo (login: admin)
-- PREMISSA A VALIDAR PELO ALUNO: substituir senha_hash por hash BCrypt gerado
-- quando o app existir (a senha em claro escolhida vai documentada no README).
INSERT INTO usuario (id, login, senha_hash, perfil) VALUES
  ('aa000000-0000-4000-8000-000000000001', 'admin', '<HASH_BCRYPT_A_GERAR_PELO_APP>', 'ADMINISTRADOR');

-- Clientes (4 PF com CPF valido, 2 PJ com CNPJ valido; documento so digitos)
INSERT INTO cliente (id, nome, documento, email, telefone) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'Ana Beatriz Souza',        '10433218100', 'ana.souza.demo@example.com',      '11987650001'),
  ('c0000000-0000-4000-8000-000000000002', 'Carlos Eduardo Lima',      '96001338914', 'carlos.lima.demo@example.com',    '11987650002'),
  ('c0000000-0000-4000-8000-000000000003', 'Fernanda Alves Pereira',   '08386379499', 'fernanda.alves.demo@example.com', '21987650003'),
  ('c0000000-0000-4000-8000-000000000004', 'Joao Pedro Nascimento',    '02654235114', 'joao.pedro.demo@example.com',     '31987650004'),
  ('c0000000-0000-4000-8000-000000000005', 'Transportadora Rota Azul', '31034131000113', 'frota.rotaazul.demo@example.com', '1130220005'),
  ('c0000000-0000-4000-8000-000000000006', 'Padaria Trigo Dourado',    '64752553000183', 'compras.trigo.demo@example.com',  '1130220006');

-- Veiculos (placas nos formatos antigo e Mercosul)
INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'ABC1D23', 'Volkswagen', 'Gol',     2019, 'c0000000-0000-4000-8000-000000000001'),
  ('e0000000-0000-4000-8000-000000000002', 'DEF4E56', 'Chevrolet',  'Onix',    2021, 'c0000000-0000-4000-8000-000000000002'),
  ('e0000000-0000-4000-8000-000000000003', 'GHI7890', 'Fiat',       'Strada',  2017, 'c0000000-0000-4000-8000-000000000003'),
  ('e0000000-0000-4000-8000-000000000004', 'JKL1234', 'Toyota',     'Corolla', 2015, 'c0000000-0000-4000-8000-000000000004'),
  ('e0000000-0000-4000-8000-000000000005', 'MNO5P67', 'Renault',    'Master',  2020, 'c0000000-0000-4000-8000-000000000005'),
  ('e0000000-0000-4000-8000-000000000006', 'QRS8T90', 'Fiat',       'Fiorino', 2018, 'c0000000-0000-4000-8000-000000000006');

-- Catalogo de servicos (valor de mao de obra em BRL)
INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'Troca de oleo',               'Troca de oleo do motor com substituicao do filtro', 120.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000002', 'Alinhamento e balanceamento', 'Alinhamento de direcao e balanceamento das rodas',  180.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000003', 'Revisao de freios',           'Inspecao e substituicao de pastilhas e fluido',     250.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000004', 'Diagnostico eletronico',      'Leitura de falhas com scanner',                     150.00, 'BRL');

-- Pecas e insumos (correia dentada ABAIXO do estoque minimo: alerta de reposicao)
INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada, estoque_minimo) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'Filtro de oleo',              'unidade', 35.90,  'BRL', 24, 0, 10),
  ('a0000000-0000-4000-8000-000000000002', 'Oleo lubrificante 5W30',      'litro',   52.00,  'BRL', 60, 4, 20),
  ('a0000000-0000-4000-8000-000000000003', 'Pastilha de freio dianteira', 'unidade', 189.90, 'BRL', 8,  1, 4),
  ('a0000000-0000-4000-8000-000000000004', 'Correia dentada',             'unidade', 240.00, 'BRL', 2,  0, 3),
  ('a0000000-0000-4000-8000-000000000005', 'Palheta do limpador',         'unidade', 45.00,  'BRL', 15, 0, 5);

-- ============================================================================
-- Ordens de Servico: uma em cada status relevante, timestamps completos
-- ============================================================================

-- OS-1: RECEBIDA (criada agora, aguardando diagnostico)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000001',
   'RECEBIDA', 'ACMP-7f3k9q2w8e5r1t6y', 'Barulho ao frear em baixa velocidade', '2026-07-15 09:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000001', NULL, 'RECEBIDA', '2026-07-15 09:00:00');

-- OS-2: EM_DIAGNOSTICO
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002',
   'EM_DIAGNOSTICO', 'ACMP-2m4n6b8v0c1x3z5a', 'Luz de injecao acesa no painel', '2026-07-14 08:30:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000201', 'd0000000-0000-4000-8000-000000000002', NULL, 'RECEBIDA', '2026-07-14 08:30:00'),
  ('90000000-0000-4000-8000-000000000202', 'd0000000-0000-4000-8000-000000000002', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-14 09:10:00');

-- OS-3: AGUARDANDO_APROVACAO (orcamento v1 PENDENTE enviado)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000003',
   'AGUARDANDO_APROVACAO', 'ACMP-9d8f7g6h5j4k3l2p', 'Revisao dos 60 mil km', '2026-07-13 10:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', NULL, 'RECEBIDA', '2026-07-13 10:00:00'),
  ('90000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-13 10:40:00'),
  ('90000000-0000-4000-8000-000000000303', 'd0000000-0000-4000-8000-000000000003', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-13 11:30:00');
INSERT INTO item_servico (id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000001', 120.00),
  ('91000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000002', 180.00);
INSERT INTO item_peca (id, ordem_servico_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 1, 35.90),
  ('92000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 4, 52.00);
-- total: 120 + 180 + 35.90 + 4x52.00 = 543.90
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 1, 'PENDENTE', 543.90, '2026-07-13 11:30:00', NULL, 10);

-- OS-4: EM_EXECUCAO (orcamento v1 APROVADO; pecas reservadas, ver quantidade_reservada)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004', 'e0000000-0000-4000-8000-000000000004',
   'EM_EXECUCAO', 'ACMP-1q2w3e4r5t6y7u8i', 'Freio rangendo e pedal baixo', '2026-07-10 08:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', NULL, 'RECEBIDA', '2026-07-10 08:00:00'),
  ('90000000-0000-4000-8000-000000000402', 'd0000000-0000-4000-8000-000000000004', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-10 08:45:00'),
  ('90000000-0000-4000-8000-000000000403', 'd0000000-0000-4000-8000-000000000004', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-10 10:00:00'),
  ('90000000-0000-4000-8000-000000000404', 'd0000000-0000-4000-8000-000000000004', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-10 14:00:00');
INSERT INTO item_servico (id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000003', 250.00);
INSERT INTO item_peca (id, ordem_servico_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003', 1, 189.90);
-- total: 250 + 189.90 = 439.90
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 1, 'APROVADO', 439.90, '2026-07-10 10:00:00', '2026-07-10 14:00:00', 10);

-- OS-5: FINALIZADA (execucao de 11:00 as 16:30 = 5,5 h para o tempo medio)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000005', 'e0000000-0000-4000-8000-000000000005',
   'FINALIZADA', 'ACMP-0p9o8i7u6y5t4r3e', 'Troca de oleo da van da frota', '2026-07-08 09:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', NULL, 'RECEBIDA', '2026-07-08 09:00:00'),
  ('90000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000005', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-08 09:30:00'),
  ('90000000-0000-4000-8000-000000000503', 'd0000000-0000-4000-8000-000000000005', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-08 10:15:00'),
  ('90000000-0000-4000-8000-000000000504', 'd0000000-0000-4000-8000-000000000005', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-08 11:00:00'),
  ('90000000-0000-4000-8000-000000000505', 'd0000000-0000-4000-8000-000000000005', 'EM_EXECUCAO', 'FINALIZADA', '2026-07-08 16:30:00');
INSERT INTO item_servico (id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000001', 120.00);
INSERT INTO item_peca (id, ordem_servico_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001', 1, 35.90),
  ('92000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 5, 52.00);
-- total: 120 + 35.90 + 5x52.00 = 415.90
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 1, 'APROVADO', 415.90, '2026-07-08 10:15:00', '2026-07-08 11:00:00', 10);

-- OS-6: ENTREGUE, com REPARO ADICIONAL (duas versoes de orcamento; a execucao
-- passa por AGUARDANDO_APROVACAO no meio; para o tempo medio, o recorte usa a
-- primeira entrada em EM_EXECUCAO (13:00) ate FINALIZADA (18:30) = 5,5 h)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000006', 'e0000000-0000-4000-8000-000000000006',
   'ENTREGUE', 'ACMP-5g6h7j8k9l0z1x2c', 'Correia rangendo; revisao geral', '2026-07-01 08:15:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', NULL, 'RECEBIDA', '2026-07-01 08:15:00'),
  ('90000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-01 09:00:00'),
  ('90000000-0000-4000-8000-000000000603', 'd0000000-0000-4000-8000-000000000006', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-01 10:30:00'),
  ('90000000-0000-4000-8000-000000000604', 'd0000000-0000-4000-8000-000000000006', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-01 13:00:00'),
  ('90000000-0000-4000-8000-000000000605', 'd0000000-0000-4000-8000-000000000006', 'EM_EXECUCAO', 'AGUARDANDO_APROVACAO', '2026-07-01 14:30:00'),
  ('90000000-0000-4000-8000-000000000606', 'd0000000-0000-4000-8000-000000000006', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-01 15:30:00'),
  ('90000000-0000-4000-8000-000000000607', 'd0000000-0000-4000-8000-000000000006', 'EM_EXECUCAO', 'FINALIZADA', '2026-07-01 18:30:00'),
  ('90000000-0000-4000-8000-000000000608', 'd0000000-0000-4000-8000-000000000006', 'FINALIZADA', 'ENTREGUE', '2026-07-02 09:30:00');
INSERT INTO item_servico (id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 'f0000000-0000-4000-8000-000000000004', 150.00),
  ('91000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'f0000000-0000-4000-8000-000000000002', 180.00);
INSERT INTO item_peca (id, ordem_servico_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000004', 1, 240.00),
  ('92000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000005', 2, 45.00);
-- v1 (diagnostico + alinhamento + palhetas): 150 + 180 + 2x45.00 = 420.00
-- v2 (reparo adicional: correia dentada): 420.00 + 240.00 = 660.00
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 1, 'APROVADO', 420.00, '2026-07-01 10:30:00', '2026-07-01 13:00:00', 10),
  ('b0000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 2, 'APROVADO', 660.00, '2026-07-01 14:30:00', '2026-07-01 15:30:00', 10);

-- OS-7: CANCELADA (7o status PROPOSTO no ADR-008, [DECISÃO PENDENTE DO ALUNO];
-- se o aluno mantiver so os 6 status, remover este bloco e a OS fica parada em
-- AGUARDANDO_APROVACAO)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002',
   'CANCELADA', 'ACMP-3v4b5n6m7a8s9d0f', 'Orcamento de pintura recusado', '2026-07-05 10:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', NULL, 'RECEBIDA', '2026-07-05 10:00:00'),
  ('90000000-0000-4000-8000-000000000702', 'd0000000-0000-4000-8000-000000000007', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-05 10:30:00'),
  ('90000000-0000-4000-8000-000000000703', 'd0000000-0000-4000-8000-000000000007', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-05 11:20:00'),
  ('90000000-0000-4000-8000-000000000704', 'd0000000-0000-4000-8000-000000000007', 'AGUARDANDO_APROVACAO', 'CANCELADA', '2026-07-06 09:00:00');
INSERT INTO item_servico (id, ordem_servico_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', 'f0000000-0000-4000-8000-000000000004', 150.00);
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', 1, 'REPROVADO', 150.00, '2026-07-05 11:20:00', '2026-07-06 09:00:00', 10);
