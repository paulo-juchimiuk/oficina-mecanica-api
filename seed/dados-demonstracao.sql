-- Dados de demonstracao do ambiente local (ADR-015).
--
-- DADOS FICTICIOS. Nomes, CPFs, CNPJs, placas, e-mails e precos foram gerados
-- para o ambiente local; os digitos verificadores de CPF/CNPJ sao validos por
-- construcao, mas os numeros nao pertencem a pessoas nem a empresas reais.

INSERT INTO usuario (id, login, senha_hash, perfil) VALUES
  ('aa000000-0000-4000-8000-000000000001', 'admin', '$2b$10$UixwZYmjQ4G4ThX0XfLJfe3a3/cOV.0z9Cce5pLuARNhplyzvgMae', 'ADMINISTRADOR');

INSERT INTO cliente (id, nome, documento, email, telefone) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'Ana Beatriz Souza',        '10433218100', 'ana.souza.demo@example.com',      '11987650001'),
  ('c0000000-0000-4000-8000-000000000002', 'Carlos Eduardo Lima',      '96001338914', 'carlos.lima.demo@example.com',    '11987650002'),
  ('c0000000-0000-4000-8000-000000000003', 'Fernanda Alves Pereira',   '08386379499', 'fernanda.alves.demo@example.com', '21987650003'),
  ('c0000000-0000-4000-8000-000000000004', 'Joao Pedro Nascimento',    '02654235114', 'joao.pedro.demo@example.com',     '31987650004'),
  ('c0000000-0000-4000-8000-000000000005', 'Transportadora Rota Azul', '31034131000113', 'frota.rotaazul.demo@example.com', '1130220005'),
  ('c0000000-0000-4000-8000-000000000006', 'Padaria Trigo Dourado',    '64752553000183', 'compras.trigo.demo@example.com',  '1130220006');

INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'ABC1D23', 'Volkswagen', 'Gol',     2019, 'c0000000-0000-4000-8000-000000000001'),
  ('e0000000-0000-4000-8000-000000000002', 'DEF4E56', 'Chevrolet',  'Onix',    2021, 'c0000000-0000-4000-8000-000000000002'),
  ('e0000000-0000-4000-8000-000000000003', 'GHI7890', 'Fiat',       'Strada',  2017, 'c0000000-0000-4000-8000-000000000003'),
  ('e0000000-0000-4000-8000-000000000004', 'JKL1234', 'Toyota',     'Corolla', 2015, 'c0000000-0000-4000-8000-000000000004'),
  ('e0000000-0000-4000-8000-000000000005', 'MNO5P67', 'Renault',    'Master',  2020, 'c0000000-0000-4000-8000-000000000005'),
  ('e0000000-0000-4000-8000-000000000006', 'QRS8T90', 'Fiat',       'Fiorino', 2018, 'c0000000-0000-4000-8000-000000000006');

INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'Troca de oleo',               'Troca de oleo do motor com substituicao do filtro', 120.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000002', 'Alinhamento e balanceamento', 'Alinhamento de direcao e balanceamento das rodas',  180.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000003', 'Revisao de freios',           'Inspecao e substituicao de pastilhas e fluido',     250.00, 'BRL'),
  ('f0000000-0000-4000-8000-000000000004', 'Diagnostico eletronico',      'Leitura de falhas com scanner',                     150.00, 'BRL');

-- Pecas e insumos. Pastilha de freio e correia dentada entram ABAIXO do estoque
-- minimo, entao a consulta abaixoDoMinimo ja tem o que mostrar.
INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada, estoque_minimo) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'Filtro de oleo',              'unidade', 35.90,  'BRL', 24, 0, 10),
  ('a0000000-0000-4000-8000-000000000002', 'Oleo lubrificante 5W30',      'litro',   52.00,  'BRL', 60, 0, 20),
  ('a0000000-0000-4000-8000-000000000003', 'Pastilha de freio dianteira', 'unidade', 189.90, 'BRL', 1,  1, 4),
  ('a0000000-0000-4000-8000-000000000004', 'Correia dentada',             'unidade', 240.00, 'BRL', 2,  0, 3),
  ('a0000000-0000-4000-8000-000000000005', 'Palheta do limpador',         'unidade', 45.00,  'BRL', 15, 0, 5);

-- Ordens de Servico: uma em cada status.
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000001',
   'RECEBIDA', 'ACMP-8731a4acc01cc83d57ec255987403be8', 'Barulho ao frear em baixa velocidade', (NOW() - INTERVAL '3 hours'));
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000001', NULL, 'RECEBIDA', (NOW() - INTERVAL '3 hours'));

INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002',
   'EM_DIAGNOSTICO', 'ACMP-87ef44dca16768bdbe7e39e49666d1cb', 'Luz de injecao acesa no painel', (NOW() - INTERVAL '1 day 3 hours'));
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000201', 'd0000000-0000-4000-8000-000000000002', NULL, 'RECEBIDA', (NOW() - INTERVAL '1 day 3 hours')),
  ('90000000-0000-4000-8000-000000000202', 'd0000000-0000-4000-8000-000000000002', 'RECEBIDA', 'EM_DIAGNOSTICO', (NOW() - INTERVAL '1 day 2 hours 20 minutes'));

-- OS-3: AGUARDANDO_APROVACAO (orcamento v1 PENDENTE enviado). Enviado ha 4 dias,
-- ou seja, passou dos 3 e ainda esta dentro da validade de 10: e a janela em que
-- o lembrete seria devido. A explicacao das datas relativas esta no README.
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000003',
   'AGUARDANDO_APROVACAO', 'ACMP-e5a312adaec084e9ea783e0ff3f142a6', 'Revisao dos 60 mil km', (NOW() - INTERVAL '4 days 2 hours'));
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', NULL, 'RECEBIDA', (NOW() - INTERVAL '4 days 2 hours')),
  ('90000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'RECEBIDA', 'EM_DIAGNOSTICO', (NOW() - INTERVAL '4 days 1 hour')),
  ('90000000-0000-4000-8000-000000000303', 'd0000000-0000-4000-8000-000000000003', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', (NOW() - INTERVAL '4 days'));
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 1, 'PENDENTE', 543.90, (NOW() - INTERVAL '4 days'), NULL, 10);
INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000301', 'f0000000-0000-4000-8000-000000000001', 120.00),
  ('91000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000301', 'f0000000-0000-4000-8000-000000000002', 180.00);
INSERT INTO item_peca (id, ordem_servico_id, orcamento_origem_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000301', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000001', 1, 35.90),
  ('92000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000002', 4, 52.00);
-- total: 120 + 180 + 35.90 + 4x52.00 = 543.90

-- OS-4: EM_EXECUCAO (orcamento v1 APROVADO). Demonstra o caminho de estoque
-- insuficiente: a revisao de freios precisa de 2 pastilhas e havia 1 em estoque,
-- entao a aprovacao reservou 1 e a falta de 1 virou pendencia. A OS SEGUE
-- EM_EXECUCAO, porque nao existe status "Aguardando pecas" (ADR-013).
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004', 'e0000000-0000-4000-8000-000000000004',
   'EM_EXECUCAO', 'ACMP-797ea4412bf429e9f9730f3c3cfb14b4', 'Freio rangendo e pedal baixo', (NOW() - INTERVAL '2 days 8 hours'));
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', NULL, 'RECEBIDA', (NOW() - INTERVAL '2 days 8 hours')),
  ('90000000-0000-4000-8000-000000000402', 'd0000000-0000-4000-8000-000000000004', 'RECEBIDA', 'EM_DIAGNOSTICO', (NOW() - INTERVAL '2 days 7 hours 15 minutes')),
  ('90000000-0000-4000-8000-000000000403', 'd0000000-0000-4000-8000-000000000004', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', (NOW() - INTERVAL '2 days 6 hours')),
  ('90000000-0000-4000-8000-000000000404', 'd0000000-0000-4000-8000-000000000004', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', (NOW() - INTERVAL '2 days 2 hours'));
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 1, 'APROVADO', 629.80, (NOW() - INTERVAL '2 days 6 hours'), (NOW() - INTERVAL '2 days 2 hours'), 10);
INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000401', 'f0000000-0000-4000-8000-000000000003', 250.00);
INSERT INTO item_peca (id, ordem_servico_id, orcamento_origem_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000003', 2, 189.90);
-- total: 250 + 2x189.90 = 629.80

-- Reserva ativa da OS-4: casa com peca.quantidade_reservada = 1 da pastilha.
-- Reservou 1 das 2 pedidas porque so havia 1 em estoque, e a diferenca virou a
-- pendencia do fim do arquivo. A OS-3 nao tem reserva porque o orcamento dela
-- ainda esta PENDENTE: a reserva dela nasce ao ser aprovada pela API.
INSERT INTO reserva_peca (id, ordem_servico_id, peca_id, quantidade, situacao, criada_em) VALUES
  ('93000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003', 1, 'ATIVA', (NOW() - INTERVAL '2 days 2 hours'));

-- OS-5: FINALIZADA (execucao de 11:00 as 16:30 = 5,5 h para o tempo medio)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000005', 'e0000000-0000-4000-8000-000000000005',
   'FINALIZADA', 'ACMP-fda4781c4f13bf649d5767e528cf19a1', 'Troca de oleo da van da frota', '2026-07-08 09:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', NULL, 'RECEBIDA', '2026-07-08 09:00:00'),
  ('90000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000005', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-08 09:30:00'),
  ('90000000-0000-4000-8000-000000000503', 'd0000000-0000-4000-8000-000000000005', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-08 10:15:00'),
  ('90000000-0000-4000-8000-000000000504', 'd0000000-0000-4000-8000-000000000005', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-08 11:00:00'),
  ('90000000-0000-4000-8000-000000000505', 'd0000000-0000-4000-8000-000000000005', 'EM_EXECUCAO', 'FINALIZADA', '2026-07-08 16:30:00');
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 1, 'APROVADO', 415.90, '2026-07-08 10:15:00', '2026-07-08 11:00:00', 10);
INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000501', 'f0000000-0000-4000-8000-000000000001', 120.00);
INSERT INTO item_peca (id, ordem_servico_id, orcamento_origem_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000501', 'a0000000-0000-4000-8000-000000000001', 1, 35.90),
  ('92000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000501', 'a0000000-0000-4000-8000-000000000002', 5, 52.00);
-- total: 120 + 35.90 + 5x52.00 = 415.90

-- Reservas ja CONSUMIDAS das OSs encerradas: nasceram na aprovacao e foram
-- consumidas na retirada pelo Mecanico (ADR-013). Situacao CONSUMIDA nao entra
-- em peca.quantidade_reservada, entao os contadores seguem batendo.
INSERT INTO reserva_peca (id, ordem_servico_id, peca_id, quantidade, situacao, criada_em) VALUES
  ('93000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001', 1, 'CONSUMIDA', '2026-07-08 11:00:00'),
  ('93000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 5, 'CONSUMIDA', '2026-07-08 11:00:00');

-- OS-6: ENTREGUE, com REPARO ADICIONAL (duas versoes de orcamento; a execucao
-- passa por AGUARDANDO_APROVACAO no meio. Tempo medio pela SOMA DE SEGMENTOS
-- (ADR-008): 1,5 h de 13:00 a 14:30, mais 3,0 h de 15:30 a 18:30, total 4,5 h.
-- A espera de aprovacao do adicional, de 14:30 a 15:30, NAO conta.)
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000006', 'e0000000-0000-4000-8000-000000000006',
   'ENTREGUE', 'ACMP-bdba8a71e0e74b6457e341e05746ff4f', 'Correia rangendo; revisao geral', '2026-07-01 08:15:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', NULL, 'RECEBIDA', '2026-07-01 08:15:00'),
  ('90000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-01 09:00:00'),
  ('90000000-0000-4000-8000-000000000603', 'd0000000-0000-4000-8000-000000000006', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-01 10:30:00'),
  ('90000000-0000-4000-8000-000000000604', 'd0000000-0000-4000-8000-000000000006', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-01 13:00:00'),
  ('90000000-0000-4000-8000-000000000605', 'd0000000-0000-4000-8000-000000000006', 'EM_EXECUCAO', 'AGUARDANDO_APROVACAO', '2026-07-01 14:30:00'),
  ('90000000-0000-4000-8000-000000000606', 'd0000000-0000-4000-8000-000000000006', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', '2026-07-01 15:30:00'),
  ('90000000-0000-4000-8000-000000000607', 'd0000000-0000-4000-8000-000000000006', 'EM_EXECUCAO', 'FINALIZADA', '2026-07-01 18:30:00'),
  ('90000000-0000-4000-8000-000000000608', 'd0000000-0000-4000-8000-000000000006', 'FINALIZADA', 'ENTREGUE', '2026-07-02 09:30:00');
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias, descricao) VALUES
  ('b0000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 1, 'APROVADO', 420.00, '2026-07-01 10:30:00', '2026-07-01 13:00:00', 10, NULL),
  ('b0000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 2, 'APROVADO', 660.00, '2026-07-01 14:30:00', '2026-07-01 15:30:00', 10,
   'Substituicao da correia dentada: desgaste identificado durante a execucao');
INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000601', 'f0000000-0000-4000-8000-000000000004', 150.00),
  ('91000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000601', 'f0000000-0000-4000-8000-000000000002', 180.00);
INSERT INTO item_peca (id, ordem_servico_id, orcamento_origem_id, peca_id, quantidade, preco_snapshot) VALUES
  ('92000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000602', 'a0000000-0000-4000-8000-000000000004', 1, 240.00),
  ('92000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000601', 'a0000000-0000-4000-8000-000000000005', 2, 45.00);
-- v1 (diagnostico + alinhamento + palhetas): 150 + 180 + 2x45.00 = 420.00
-- v2 (reparo adicional: correia dentada): 420.00 + 240.00 = 660.00

INSERT INTO reserva_peca (id, ordem_servico_id, peca_id, quantidade, situacao, criada_em) VALUES
  ('93000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000005', 2, 'CONSUMIDA', '2026-07-01 13:00:00'),
  ('93000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000004', 1, 'CONSUMIDA', '2026-07-01 15:30:00');

-- OS-7: CANCELADA, 7o status decidido no ADR-008. E o ramo 1 do guard da
-- reprovacao: nao existe versao aprovada nesta OS, entao a reprovacao leva a
-- Cancelada. Por construcao, ela nao tem reserva de pecas ativa (ADR-013).
INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, relato_do_problema, criada_em) VALUES
  ('d0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002',
   'CANCELADA', 'ACMP-50ffc2a47be73697821264f6b07a8bb5', 'Cliente desistiu apos o diagnostico', '2026-07-05 10:00:00');
INSERT INTO transicao_status (id, ordem_servico_id, de_status, para_status, data_hora) VALUES
  ('90000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', NULL, 'RECEBIDA', '2026-07-05 10:00:00'),
  ('90000000-0000-4000-8000-000000000702', 'd0000000-0000-4000-8000-000000000007', 'RECEBIDA', 'EM_DIAGNOSTICO', '2026-07-05 10:30:00'),
  ('90000000-0000-4000-8000-000000000703', 'd0000000-0000-4000-8000-000000000007', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', '2026-07-05 11:20:00'),
  ('90000000-0000-4000-8000-000000000704', 'd0000000-0000-4000-8000-000000000007', 'AGUARDANDO_APROVACAO', 'CANCELADA', '2026-07-06 09:00:00');
INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total, data_envio, data_resposta, validade_dias) VALUES
  ('b0000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', 1, 'REPROVADO', 150.00, '2026-07-05 11:20:00', '2026-07-06 09:00:00', 10);
INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot) VALUES
  ('91000000-0000-4000-8000-000000000701', 'd0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000701', 'f0000000-0000-4000-8000-000000000004', 150.00);

-- Pendencia de peca da OS-4. Nasceu no caminho automatico: na aprovacao do
-- orcamento, a reserva pediu 2 pastilhas e o Saldo em estoque tinha 1, entao
-- reservou 1 e a falta de 1 virou pendencia. Por isso a deteccao acompanha o
-- instante da aprovacao. A peca faltante e item do escopo JA APROVADO: falta de
-- peca nao introduz escopo novo, o que exigiria Reparo adicional e nova versao.
-- A OS NAO muda de status (ADR-013). Alimenta o modelo de leitura Pendencias de
-- pecas por OS e a decisao de compra do Estoquista.
INSERT INTO pendencia_peca (id, ordem_servico_id, peca_id, quantidade_faltante, detectada_em) VALUES
  ('94000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003', 1, (NOW() - INTERVAL '2 days 2 hours'));
