-- ========================================
-- SCRIPT PARA EXECUTAR MANUALMENTE
-- Execute este script APÓS a aplicação criar as tabelas
-- ========================================

-- Executar via psql:
-- psql -U postgres -d bordero_db -f insert_dados_iniciais.sql

-- 1. Limpar dados existentes (se necessário)
TRUNCATE TABLE tarifas CASCADE;
TRUNCATE TABLE feriados CASCADE;
TRUNCATE TABLE tipos_titulo CASCADE;

-- 2. Inserir Tipos de Título
INSERT INTO tipos_titulo (tipo, nome, descricao, ativo, data_criacao, data_atualizacao) VALUES
('NF', 'Nota Fiscal', 'Título existente em uma nota fiscal', true, NOW(), NOW()),
('CHQ', 'Cheque', 'Título em forma de cheque', true, NOW(), NOW()),
('DUP', 'Duplicata', 'Duplicata mercantil', true, NOW(), NOW()),
('BOL', 'Boleto', 'Boleto bancário', true, NOW(), NOW());

-- 3. Inserir Tarifas (VALORES CONFORME ESPECIFICAÇÃO)
INSERT INTO tarifas (tipo, codigo, nome, descricao, valor, ativa, data_criacao, data_atualizacao) VALUES
('DOCUMENTO', 'TAR_DOC', 'Tarifa por Título', 'Tarifa cobrada por cada título do borderô', 15.00, true, NOW(), NOW()),
('CLIENTE', 'SERASA', 'Consulta Serasa', 'Consulta de crédito no Serasa (por sacado)', 50.00, true, NOW(), NOW()),
('GERAL', 'TAC', 'TAC - Tarifa de Abertura de Crédito', 'Taxa cobrada na abertura do crédito', 100.00, true, NOW(), NOW()),
('GERAL', 'TED', 'TED - Transferência Eletrônica', 'Taxa de transferência bancária', 50.00, true, NOW(), NOW());

-- 4. Inserir Feriados Nacionais 2025
INSERT INTO feriados (data, nome, tipo, descricao, ativo) VALUES
('2025-01-01', 'Confraternização Universal', 'NACIONAL', 'Ano Novo', true),
('2025-03-04', 'Carnaval', 'NACIONAL', 'Carnaval', true),
('2025-04-18', 'Sexta-feira Santa', 'NACIONAL', 'Paixão de Cristo', true),
('2025-04-21', 'Tiradentes', 'NACIONAL', 'Tiradentes', true),
('2025-05-01', 'Dia do Trabalho', 'NACIONAL', 'Dia do Trabalhador', true),
('2025-06-19', 'Corpus Christi', 'NACIONAL', 'Corpus Christi', true),
('2025-09-07', 'Independência do Brasil', 'NACIONAL', 'Sete de Setembro', true),
('2025-10-12', 'Nossa Senhora Aparecida', 'NACIONAL', 'Padroeira do Brasil', true),
('2025-11-02', 'Finados', 'NACIONAL', 'Dia de Finados', true),
('2025-11-15', 'Proclamação da República', 'NACIONAL', 'República', true),
('2025-11-20', 'Consciência Negra', 'NACIONAL', 'Consciência Negra', true),
('2025-12-25', 'Natal', 'NACIONAL', 'Natal', true);

-- 5. Inserir Feriados Nacionais 2026
INSERT INTO feriados (data, nome, tipo, descricao, ativo) VALUES
('2026-01-01', 'Confraternização Universal', 'NACIONAL', 'Ano Novo', true),
('2026-02-17', 'Carnaval', 'NACIONAL', 'Carnaval', true),
('2026-04-03', 'Sexta-feira Santa', 'NACIONAL', 'Paixão de Cristo', true),
('2026-04-21', 'Tiradentes', 'NACIONAL', 'Tiradentes', true),
('2026-05-01', 'Dia do Trabalho', 'NACIONAL', 'Dia do Trabalhador', true),
('2026-06-04', 'Corpus Christi', 'NACIONAL', 'Corpus Christi', true),
('2026-09-07', 'Independência do Brasil', 'NACIONAL', 'Sete de Setembro', true),
('2026-10-12', 'Nossa Senhora Aparecida', 'NACIONAL', 'Padroeira do Brasil', true),
('2026-11-02', 'Finados', 'NACIONAL', 'Dia de Finados', true),
('2026-11-15', 'Proclamação da República', 'NACIONAL', 'República', true),
('2026-11-20', 'Consciência Negra', 'NACIONAL', 'Consciência Negra', true),
('2026-12-25', 'Natal', 'NACIONAL', 'Natal', true);

-- 6. Inserir Feriados Estaduais SP
INSERT INTO feriados (data, nome, tipo, descricao, uf, ativo) VALUES
('2025-07-09', 'Revolução Constitucionalista', 'ESTADUAL', 'Revolução de 1932', 'SP', true),
('2026-07-09', 'Revolução Constitucionalista', 'ESTADUAL', 'Revolução de 1932', 'SP', true);

-- 7. Inserir Feriados Municipais São Paulo
INSERT INTO feriados (data, nome, tipo, descricao, uf, codigo_municipio, ativo) VALUES
('2025-01-25', 'Aniversário de São Paulo', 'MUNICIPAL', 'Aniversário da Cidade', 'SP', '3550308', true),
('2026-01-25', 'Aniversário de São Paulo', 'MUNICIPAL', 'Aniversário da Cidade', 'SP', '3550308', true);

-- 8. Verificar inserções
SELECT 'Tipos de Título:' as tabela, COUNT(*) as total FROM tipos_titulo
UNION ALL
SELECT 'Tarifas:', COUNT(*) FROM tarifas
UNION ALL
SELECT 'Feriados:', COUNT(*) FROM feriados;

-- Deve mostrar:
-- Tipos de Título: 4
-- Tarifas: 4
-- Feriados: 28