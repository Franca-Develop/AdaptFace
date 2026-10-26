# Baterias de testes

Execute os comandos a partir da raiz do projeto.

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
