# Modelo de dados executável

Este documento registra o modelo efetivamente implementado pelas entidades JPA e pelo schema MySQL. Ele deve permanecer sincronizado com o MER/DER acadêmico.

```mermaid
erDiagram
    INSTITUTIONS ||--o{ USERS : possui
    USERS ||--o| STUDENTS : perfil_aluno
    USERS ||--o| EMPLOYEES : perfil_funcionario
    USERS ||--o| ENROLLMENTS : matricula
    ENROLLMENTS ||--o| CREDENTIALS : credenciais
    ENROLLMENTS ||--o{ ACCESS_LOGS : historico

    INSTITUTIONS {
        bigint id PK
        varchar name
    }
    USERS {
        bigint id PK
        bigint institution_id FK
        varchar cpf UK
        varchar email UK
        varchar password
        enum user_type "ADMIN, EMPLOYEE, STUDENT"
        bigint auth_version
    }
    STUDENTS {
        bigint user_id PK,FK
        int registration_number
        varchar period
        varchar course
    }
    EMPLOYEES {
        bigint user_id PK,FK
        varchar department
    }
    ENROLLMENTS {
        bigint id PK
        bigint user_id FK,UK
        date enrollment_date
        boolean active
    }
    CREDENTIALS {
        bigint id PK
        bigint enrollment_id FK,UK
        json face_embedding
        varchar face_model
        varchar rfid_tag UK
        varchar access_code
    }
    ACCESS_LOGS {
        bigint id PK
        bigint enrollment_id FK "opcional para desconhecido"
        enum credential_type
        datetime logged_at
        boolean entry
        enum result
        decimal calculated_distance
        varchar denial_reason
    }
```

## Regras estruturais

- Uma pessoa pertence obrigatoriamente a uma instituição.
- `Student` e `Employee` compartilham a chave primária de `User` por `@MapsId`.
- `ADMIN` é um tipo de usuário com perfil de funcionário e departamento.
- Uma pessoa possui no máximo uma matrícula no protótipo atual.
- Uma matrícula possui no máximo um agregado de credenciais.
- Uma tentativa desconhecida mantém `enrollment_id` nulo, mas ainda registra modalidade, máquina, sentido e negativa.
- Senhas e códigos são persistidos com BCrypt; embeddings ficam em JSON e nunca são devolvidos nos DTOs de consulta.

## Inicialização administrativa

Para bancos antigos, execute `scripts/migrate-admin-role.sql` uma vez. Em seguida, quando `GET /auth/setup-status` retornar `requiresSetup=true`, use a opção **Configurar primeiro administrador** em `/admin`. A criação permanece transacional e passa pelo backend; nenhuma senha pronta é incluída em SQL ou no repositório.
