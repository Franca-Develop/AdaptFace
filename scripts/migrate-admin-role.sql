-- Migração idempotente para bancos criados antes da inclusão do perfil ADMIN.
-- A conta não é criada por SQL: use a tela de configuração inicial, que aplica BCrypt e validações.
ALTER TABLE users
    MODIFY COLUMN user_type ENUM('ADMIN', 'EMPLOYEE', 'STUDENT') NOT NULL;

UPDATE users
SET auth_version = 0
WHERE auth_version IS NULL;
