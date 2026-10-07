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
├── entity/                        Tabelas do banco (@Entity)
│   └── enums/                     Enums usados pelas entidades e DTOs
├── dto/
│   ├── request/                   O que a API recebe (com validações @NotBlank, @Email...)
│   └── response/                  O que a API devolve (nunca devolve entity nem senha)
│
│   -- criar só quando tiver o primeiro arquivo (por isso não existem ainda) --
├── config/                        SecurityConfig, OpenApiConfig, PasswordEncoder
├── security/                      JwtService, JwtAuthenticationFilter
├── exception/                     BusinessException, NotFoundException, GlobalExceptionHandler
├── mapper/                        Conversão entity <-> DTO (se o service ficar poluído)
└── client/                        Chamada ao serviço externo de reconhecimento facial (se houver)
```

Regra de dependência (sempre nesse sentido, nunca ao contrário):
`controller -> service -> repository -> entity`. O controller nunca usa repository direto, e a entity nunca sai da camada de service.

## 2. Entidades

| Entidade | Tabela | Para que serve |
|---|---|---|
| Institution | institutions | Escola/faculdade dona dos usuários |
| User | users | Login + dados pessoais de qualquer pessoa (`userType` = STUDENT ou EMPLOYEE) |
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
| TipoUsuario (ALUNO, FUNCIONARIO) | UserType (STUDENT, EMPLOYEE) |
| TipoCredencial (FACIAL, RFID, CODIGO) | CredentialType (FACE, RFID, CODE) |
| ResultadoAcesso (LIBERADO, NEGADO) | AccessResult (GRANTED, DENIED) |
| RegistroAcessoService / AcessoService | AccessLogService / AccessService |
| `*RequestDTO` / `*ResponseDTO` | mesmos sufixos, em `dto/request` e `dto/response` |

## 4. Quem faz o quê (services e controllers estão vazios de propósito)

| Controller | Service | Repositories | Endpoints sugeridos |
|---|---|---|---|
| AuthController | AuthService | UserRepository | `POST /auth/login` |
| UserController | UserService | UserRepository, StudentRepository, EmployeeRepository, InstitutionRepository | `POST /users`, `GET /users/{id}` |
| EnrollmentController | EnrollmentService | EnrollmentRepository, UserRepository | `POST /enrollments`, `PATCH /enrollments/{id}/status` |
| CredentialController | CredentialService | CredentialRepository, EnrollmentRepository | `PUT /enrollments/{id}/credentials/face`, `.../rfid`, `.../code`, `GET .../credentials` |
| AccessController | AccessService, AccessLogService | CredentialRepository, AccessLogRepository | `POST /access/validate` (chamado pela máquina), `GET /access/logs?userId=` |

## 5. Ordem sugerida para terminar o back-end

1. Conferir `application.properties` (MySQL) e subir a aplicação com o `contextLoads` passando.
2. `exception/`: `GlobalExceptionHandler` devolvendo `ErrorResponseDTO`.
3. `config/` + `security/`: `PasswordEncoder` (BCrypt), `SecurityConfig` e JWT. Sem isso o Spring Security bloqueia todos os endpoints.
4. `AuthService` + `AuthController` (login).
5. `UserService` + `UserController` (cria User + Student/Employee na mesma transação).
6. `EnrollmentService` + `EnrollmentController`.
7. `CredentialService` + `CredentialController` (salvar `accessCode` com hash).
8. `AccessService` + `AccessLogService` + `AccessController` (criar `AccessValidationRequestDTO`).

## 6. Pendências e decisões em aberto

- **Instituição no cadastro de usuário:** `User.institution` é obrigatório, mas `UserRequestDTO` não recebe `institutionId`. Decidir se vem no body ou do usuário logado.
- **Embedding facial:** hoje é `TEXT`. Decidir o formato (JSON de floats) e quem gera (biblioteca Java ou serviço externo em `client/`).
- **Comparação facial:** `findByEnrollmentActiveTrue()` carrega todas as credenciais ativas para comparar em memória. Serve para protótipo; com muitos usuários precisa de outra estratégia.
- **Lazy loading:** as relações são `LAZY` e `open-in-view=false`. Converta para DTO dentro do método `@Transactional` do service.
- **`gender`:** hoje é `Character`. Se quiser, vira enum.
- **Mensagens de validação:** estão em português (texto que o usuário final vê). Identificadores de código estão em inglês.
