# Baterias de testes

Execute os comandos a partir da raiz do projeto.

## Java — Maven/JUnit

```powershell
.\mvnw.cmd test
```

Somente a documentação OpenAPI:

```powershell
.\mvnw.cmd -Dtest=OpenApiDocumentationTests test
```

Os testes opcionais que usam MySQL real exigem as variáveis `AUDIT_MYSQL_URL`, `AUDIT_MYSQL_USER` e `AUDIT_MYSQL_PASSWORD`. Não aponte essas variáveis para o banco de uso normal.

## React — Jest

Execução única:

```powershell
cd frontend
npm test
```

Execução com cobertura:

```powershell
cd frontend
npm run test:coverage
```

O relatório navegável é gerado em `frontend/coverage/lcov-report/index.html`.

Modo contínuo durante o desenvolvimento:

```powershell
cd frontend
npm run test:watch
```

A bateria usa Jest, React Testing Library e ambiente JSDOM. Ela não acessa câmera, rede ou banco reais: mídia, HTTP, armazenamento de sessão e canvas são simulados de forma determinística.

## Python — unittest

A forma recomendada é executar no mesmo ambiente isolado usado pelo serviço:

```powershell
docker compose build vision
docker compose run --rm --no-deps vision python -m unittest discover -s tests -v
```

Os testes não carregam o modelo neural e não chamam o backend real. A bateria valida:

- prova de vida passiva e expiração da sessão;
- consistência da identidade e rejeição de frames repetidos;
- normalização e seleção do rosto principal;
- validação de URL e respostas do backend;
- JSON estrito, UUID de sessão e decodificação segura da imagem.

Para executar diretamente no Python local, instale primeiro as dependências de `vision-service/requirements.txt` e use:

```powershell
cd vision-service
python -m unittest discover -s tests -v
```

## Docker — integração dos contêineres

No PowerShell:

```powershell
.\scripts\test-docker.ps1
```

O script valida o Compose, constrói todas as imagens, inicia os serviços e confirma:

- React em `http://localhost:3000/`;
- contrato OpenAPI em `http://localhost:8080/v3/api-docs`;
- visão em `http://localhost:3000/vision/health`.

Os contêineres permanecem ativos para inspeção. Para encerrá-los sem apagar dados:

```powershell
docker compose down
```

## Testes manuais recomendados

- câmera negada, rosto desconhecido, foto/tela e sessão expirada;
- login de ADMIN, tentativa de aluno e JWT expirado;
- cadastro de aluno e funcionário, CPF inválido/duplicado e embedding inválido;
- banco e serviço de visão indisponíveis, confirmando que o acesso permanece negado.
