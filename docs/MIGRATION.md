# Atualização segura do esquema

Não executar alterações em um banco real sem backup verificado e revisão dos metadados. Nenhum script destrutivo ou recriação do banco real faz parte desta auditoria. MySQL DDL pode efetuar commit implícito: não conte com rollback para desfazer `ALTER TABLE`.

1. Confirme `SELECT DATABASE()` e consulte `information_schema.columns` para `users.auth_version`, `credentials.face_model` e `access_logs.calculated_distance`.
2. Compare restrições únicas de CPF/email, RFID e vínculos de matrícula. Corrija dados conflitantes por processo administrativo, sem apagar cadastros automaticamente.
3. Em uma cópia de homologação, aplique apenas os comandos necessários abaixo. Não repita `ADD COLUMN` em coluna existente.

```sql
ALTER TABLE users ADD COLUMN auth_version BIGINT NULL;
ALTER TABLE credentials ADD COLUMN face_model VARCHAR(64) NULL;
ALTER TABLE access_logs MODIFY COLUMN calculated_distance DECIMAL(50,12) NULL;
```

`auth_version` nulo equivale a zero no Java e é incrementado na troca de senha. `face_model` nulo continua representando exclusivamente o modelo legado; nunca preencha o identificador novo sem gerar um novo embedding da foto correta e autorizada.

Antes da alteração decimal, confirme que o tipo atual é compatível e possui no máximo 38 dígitos inteiros e 12 decimais. Se tiver maior capacidade, preserve essa capacidade e ajuste o mapeamento antes de migrar. O novo mapeamento preserva mais casas para novas distâncias; não recupera precisão já arredondada de registros antigos.

4. Verifique integridade, contagens e leitura dos registros após a alteração em homologação. Planeje janela/lock conforme volume.
5. Após aprovação, replique as mudanças necessárias e inicie com `DB_SCHEMA_MODE=validate`, sem `create` ou `create-drop` no ambiente real.

Durante esta auditoria o servidor de desenvolvimento já em execução usa `update`. A consulta final somente de metadados confirmou `auth_version BIGINT NULL` e `calculated_distance DECIMAL(50,12)` no banco local. Não foram executados comandos de recriação nem alterações de registros reais. Isso não substitui o procedimento de migração para outras instalações.
