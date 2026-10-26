# adaptFace-API — Estrutura do projeto

API de controle de acesso por reconhecimento facial, RFID ou código.
Stack: Java 17, Spring Boot, Spring Data JPA, Spring Security, MySQL, springdoc (Swagger).

## 1. Pastas (`src/main/java/com/dev/adaptface`)

```
adaptface/
├── AdaptFaceApiApplication.java   Classe que sobe a aplicação
├── controller/                    Recebe HTTP, valida o DTO, chama o service. Sem regra de negócio.
├── service/                       Regras de negócio e @Transactional. Converte entity <-> DTO.
├── repository/                    Interfaces Spring Data JPA (acesso ao banco). Sem regra de negócio.
├── entities/                      Tabelas do banco (@Entity)
│   └── enums/                     Enums usados pelas entidades e DTOs
├── dto/
│   ├── request/                   O que a API recebe (com validações @NotBlank, @Email...)
│   └── response/                  O que a API devolve (nunca devolve entity nem senha)
│
├── config/                        SecurityConfig, JsonConfig, OpenApiConfig, PasswordEncoder
├── security/                      JWT, API key, limite de corpo e erros seguros
└── exceptions/                    BusinessException, NotFoundException, GlobalExceptionHandler
```

Regra de dependência (sempre nesse sentido, nunca ao contrário):
`controller -> service -> repository -> entity`. O controller nunca usa repository direto, e a entity nunca sai da camada de service.

## 2. Entidades

| Entidade | Tabela | Para que serve |
|---|---|---|
| Institution | institutions | Escola/faculdade dona dos usuários |
| User | users | Login + dados pessoais (`userType` = STUDENT, EMPLOYEE ou ADMIN) |
| Student | students | Perfil do aluno (RM, período, curso). Mesmo id do User |
| Employee | employees | Perfil do funcionário (departamento). Mesmo id do User |
| Enrollment | enrollments | Matrícula no controle de acesso (ativa/inativa). Liga User, Credential e AccessLog |
| Credential | credentials | Face (embedding), tag RFID e código de acesso da matrícula |
| AccessLog | access_logs | Histórico de tentativas de acesso (GRANTED/DENIED) |

Relações: `Institution 1—N User`, `User 1—1 Student|Employee`, `User 1—1 Enrollment`, `Enrollment 1—1 Credential`, `Enrollment 1—N AccessLog`.

## 3. Nomes: antigo -> novo

| Antes | Agora |
|---|---|
| Usuario / Aluno / Funcionario / Instituicao | User / Student / Employee / Institution |
| Cadastro | Enrollment |
| Credencial | Credential |
| Maquina (era o histórico de acessos) | AccessLog |
| TipoUsuario (ALUNO, FUNCIONARIO) | UserType (STUDENT, EMPLOYEE, ADMIN) |
| TipoCredencial (FACIAL, RFID, CODIGO) | CredentialType (FACE, RFID, CODE) |
| ResultadoAcesso (LIBERADO, NEGADO) | AccessResult (GRANTED, DENIED) |
| RegistroAcessoService / AcessoService | AccessLogService / AccessService |
| `*RequestDTO` / `*ResponseDTO` | mesmos sufixos, em `dto/request` e `dto/response` |

## 4. Quem faz o quê

| Controller | Service | Repositories | Endpoints sugeridos |
|---|---|---|---|
| AuthController | AuthService | UserRepository | `POST /auth/login` |
| InstitutionController | InstitutionService | InstitutionRepository | `GET`, `POST /institutions` |
| UserController | UserService | UserRepository, StudentRepository, EmployeeRepository, InstitutionRepository | `POST|GET /users`, `GET|PUT /users/{id}` |
| EnrollmentController | EnrollmentService | EnrollmentRepository, UserRepository | `POST /enrollments`, `PATCH /enrollments/{id}/status` |
| CredentialController | CredentialService | CredentialRepository, EnrollmentRepository | `PUT /enrollments/{id}/credentials/face`, `.../rfid`, `.../code`, `GET .../credentials` |
| AccessController | AccessService, AccessLogService | CredentialRepository, AccessLogRepository | `POST /access/validate` (chamado pela máquina), `GET /access/logs?userId=` |

## 5. Fluxo para executar o back-end

1. Definir `JWT_SECRET`, `MACHINE_API_KEY` e `BOOTSTRAP_KEY`.
2. Configurar o MySQL por `DB_URL`, `DB_USER` e `DB_PASSWORD`.
3. Executar `mvnw.cmd spring-boot:run`.
4. Chamar uma vez `POST /auth/bootstrap` com `X-Bootstrap-Key`.
5. Usar o JWT retornado nos endpoints administrativos.
6. Usar `X-API-Key` em `POST /access/validate`.

## 6. Pendências e decisões em aberto

- **Embedding facial:** DeepFace/ArcFace/OpenCV gera 512 valores. O Spring normaliza, persiste em JSON e compara distância de cosseno entre modelos iguais; vetores legados não são convertidos automaticamente.
- **Comparação facial:** `findFacialCredentials()` carrega credenciais com vínculo de usuário, inclusive inativas, para identificar antes de autorizar. Códigos também incluem inativos na comparação. Consultas em memória são adequadas ao protótipo, não constituem estratégia validada de grande escala.
- **Histórico:** negativas semânticas e desconhecidos são registrados; requisições sem autenticação ou contrato estrutural válido não viram tentativas biométricas. Nenhuma imagem ou embedding vai ao histórico.
- **Auditoria:** resultados verificáveis em `AUDIT.md`; roteiro aditivo em `MIGRATION.md`.
- **Lazy loading:** as relações são `LAZY` e `open-in-view=false`. Converta para DTO dentro do método `@Transactional` do service.
- **`gender`:** usa o enum `Gender`.
- **Mensagens de validação:** estão em português (texto que o usuário final vê). Identificadores de código estão em inglês.
