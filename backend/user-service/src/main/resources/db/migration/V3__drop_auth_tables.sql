-- User Service — V3: remove tabelas de autenticação (extraídas para o Auth Service, auth_db)
-- Ver docs/planning/action-plan.md § Decisões de Consolidação

DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS password_reset_tokens;
