# AdaptFace API

API de controle de acesso por reconhecimento facial, RFID e código, desenvolvida para o TCC de ADS do SENAI Sorocaba.

## Requisitos

- Java 17 ou superior
- MySQL 8

## Configuração

No PowerShell, gere e defina as chaves antes de iniciar a aplicação:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:MACHINE_API_KEY = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLower()
$env:BOOTSTRAP_KEY = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLower()

$env:DB_URL = "jdbc:mysql://localhost:3306/adaptface?createDatabaseIfNotExist=true&serverTimezone=America/Sao_Paulo"
$env:DB_USER = "root"
$env:DB_PASSWORD = "sua-senha"
```

Execute com o Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

O Swagger fica disponível em `http://localhost:8080/swagger-ui.html`.

## Primeiro acesso

Enquanto não existir funcionário administrativo, use uma única vez `POST /auth/bootstrap`, enviando a chave de `BOOTSTRAP_KEY` no header `X-Bootstrap-Key`. O endpoint reutiliza a instituição pelo nome ou cria uma nova e cadastra o primeiro funcionário, devolvendo seu JWT. Alunos existentes são preservados e não são promovidos. `GET /auth/setup-status` informa somente se essa configuração ainda é necessária.

```json
{
  "institutionName": "SENAI Sorocaba",
  "name": "Administrador",
  "cpf": "12345678901",
  "email": "admin@adaptface.com",
  "password": "senha-segura",
  "gender": "UNDEFINED",
  "department": "TI"
}
```

Use `Authorization: Bearer <token>` nos endpoints administrativos. A máquina de acesso usa o header `X-API-Key` com o valor de `MACHINE_API_KEY` em `POST /access/validate`.

A aplicação Python usa DeepFace (`ArcFace`, detector `opencv`) e envia o embedding de 512 posições com `faceModel="DEEPFACE_ARCFACE_OPENCV_V1"` ao endpoint de validação. Para cadastrar ou atualizar uma face, envie vetor e modelo em `PUT /enrollments/{id}/credentials/face`; o backend normaliza o vetor antes de armazenar e compara por distância de cosseno somente dentro do mesmo modelo. O mesmo campo é aceito por `POST /registrations`. Credenciais antigas sem modelo são tratadas como `INSIGHTFACE_BUFFALO_L`, sem conversão ou comparação automática entre modelos.

## Endpoints principais

- `POST /auth/login`, `POST /auth/bootstrap` e `GET /auth/setup-status`
- `POST /registrations`: cadastro completo e transacional, restrito a funcionários
- `GET|POST /institutions`
- `POST|GET /users` e `GET|PUT /users/{id}`
- `POST /enrollments`, `GET /enrollments/{id}` e `PATCH /enrollments/{id}/status`
- `PUT /enrollments/{id}/credentials/face|rfid|code`
- `GET /enrollments/{id}/credentials`
- `POST /access/validate` e `GET /access/logs`

## Testes

O cadastro completo recebe `user` (o mesmo contrato de criação de usuário), `faceEmbedding` (512 valores) e `rfidTag` opcional. A resposta contém `userId`, `enrollmentId`, `name` e `hasFace`. Falhas no cadastro de uma credencial revertem também a pessoa e a matrícula, sem registros parciais. A interface PySide6 fornece login, primeiro acesso, formulário, análise da foto e confirmação; nenhuma foto é enviada ao backend.

```powershell
.\mvnw.cmd test
```

## Contrato e segurança

`POST /access/validate` exige `X-API-Key`; JWT administrativo não substitui essa chave. Envie `credentialType`, `machineModel`, `entry` e o campo da modalidade. Para face, envie `faceEmbedding` com 512 números finitos e `faceModel`. Campos adicionais, JSON duplicado e corpos acima de 64 KiB são rejeitados. O cliente não envia nome nem decisão.

Distância de cosseno menor ou igual a `FACE_DISTANCE_THRESHOLD` (padrão `0.5`, intervalo `[0,1)`) permite uma correspondência, não representa porcentagem de confiança. O backend identifica primeiro, depois verifica a matrícula ativa. Empates faciais e códigos ambíguos são negados. Cadastro impede reutilização do mesmo vetor normalizado, RFID ou código em outra matrícula, inclusive inativa.

Uma decisão de negócio retorna HTTP 200 com `result=GRANTED` e `name`, ou `result=DENIED` e `reason`. Dados estruturalmente inválidos retornam 400; autenticação inválida, 401; falta de permissão, 403; excesso de corpo, 413; indisponibilidade do banco/histórico, 503. Erros na validação incluem `result=DENIED`; nunca interprete erro HTTP como liberação. Não há resposta positiva antes de persistir e concluir a transação do histórico. Sem banco disponível, não é possível garantir persistência da tentativa e o cliente deve negar.

`GET /users` e `GET /access/logs` usam `page` (a partir de zero) e `size` (máximo 100). A resposta tem `content` e `page: {size, number, totalElements, totalPages}`. Usuários são ordenados por ID; histórico por data e ID decrescentes.

`PUT /users/{id}` recebe os dados completos do perfil, sem `userType`. Senha é opcional na edição; quando enviada, deve ter pelo menos oito caracteres e até 72 bytes UTF-8. A alteração revoga JWTs anteriores. CPF/email continuam únicos e a edição preserva o tipo de usuário. A política existente mantém funcionários como administradores e alunos sem permissão administrativa.

Para produção, use HTTPS, usuário MySQL com permissões mínimas, segredos persistidos fora do repositório e `DB_SCHEMA_MODE=validate` após revisar a [migração segura](docs/MIGRATION.md). O modo `update` continua restrito à conveniência de desenvolvimento. Desative Swagger quando não necessário usando `SPRINGDOC_API_DOCS_ENABLED=false` e `SPRINGDOC_SWAGGER_UI_ENABLED=false`.

A [auditoria técnica](docs/AUDIT.md) registra resultados, arquivos alterados e limitações. Testes MySQL adicionais só são habilitados por `AUDIT_MYSQL_URL`, com esquema novo `adaptface_audit_<hex>` em `127.0.0.1:3306`, e `AUDIT_MYSQL_USER`/`AUDIT_MYSQL_PASSWORD`. Nunca aponte esses testes para o banco de uso real. `scripts/audit_http_contract.py` é exclusivamente para uma instância HTTP temporária desse ambiente, não para o servidor normal.

## Docker

O Compose executa a API e um MySQL 8.4 isolado. O banco não é publicado na máquina e seus dados ficam no volume `adaptface_mysql_data`.

Crie o arquivo local de variáveis e substitua todos os valores de exemplo:

```powershell
Copy-Item .env.example .env
$jwt = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$machineKey = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLower()
$bootstrapKey = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLower()
```

Coloque os três valores gerados em `JWT_SECRET`, `MACHINE_API_KEY` e `BOOTSTRAP_KEY` no `.env`. O arquivo `.env` é ignorado pelo Git.

Valide e inicie:

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

A API ficará em `http://localhost:8080`, ou na porta definida em `API_PORT`. Para parar sem apagar o banco, use `docker compose down`. Não use `docker compose down -v` em um ambiente com dados que precisam ser preservados.

Em produção, aplique a migração revisada antes de definir `DB_SCHEMA_MODE=validate`. A imagem roda como usuário sem privilégios, não contém segredos e não inclui testes, relatórios ou arquivos locais no contexto final.
