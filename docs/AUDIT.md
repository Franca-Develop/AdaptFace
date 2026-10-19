# Auditoria técnica AdaptFace — 2026-10-19

## Escopo e arquitetura inspecionada

Backend Spring Boot 4.1.1/Java 17: controller → service transacional → Spring Data JPA → MySQL. Interface e captura em Python/PySide6/OpenCV; `DeepFace.represent` com ArcFace/ONNX, alinhamento e embedding L2 de 512 valores. Python envia embedding e versão do modelo, não uma autorização. Spring compara distância de cosseno, verifica matrícula e persiste a decisão. Este fluxo foi preservado, sem substituição do modelo ou da métrica.

Raízes: backend neste diretório; Python no projeto `AdaptFace_Vision_ROI_Completo (1)` já utilizado. Nenhum dado real foi apagado, nenhum usuário real foi criado e nenhuma credencial biométrica real foi alterada por esta auditoria. Testes usam identidades e vetores sintéticos em ambiente separado.

## Matriz inicial — antes das correções desta auditoria

Legenda: A = atendido na inspeção; P = parcialmente atendido; N = não atendido; V = não verificável apenas por inspeção. A não substitui execução de teste.

| ID / requisito | Antes | Evidência / lacuna |
|---|---|---|
| 1 / preservar arquitetura, nomes e dados | A | Reaproveitamento de Spring/Python/MySQL/DeepFace/OpenCV; sem recriação |
| 2 / captura → extração → backend → decisão → histórico | P | Fluxo conectado; faltam testes negativos de persistência e falhas |
| 3.1 / DeepFace efetivo, não só dependência | A | `recognizer.py` chama `DeepFace.represent` |
| 3.2 / OpenCV na captura e processamento | A | `camera.py`, ROI/máscara em `recognition_worker.py` |
| 3.3 / imagem válida e ausência de rosto | P | Detecção obrigatória; testes de contrato e casos sem rosto, mas faltam imagens corrompidas |
| 3.4 / imagem inválida, falha de captura e erro da biblioteca | P | Captura assíncrona e recuperação; carregamento de foto duplicado e leitura de arquivo não limitada após `stat` |
| 3.5 / correspondência, desconhecido e métrica | P | Cosseno no backend; candidatos inativos excluídos antes da identificação |
| 3.6 / limiar configurável e coerente | P | Variável de ambiente existe; fallback diverge e não valida configuração |
| 3.7 / modelo, formato, dimensão e normalização | P | Modelo versionado; compatibilidade legada aceita dimensão arbitrária |
| 3.8 / detecção não equivale a identificação; sem confiança em nome | A | `GRANTED` depende de comparação com credencial persistida |
| 3.9 / saída consistente e falha nunca autoriza | P | Contrato existente; faltam testes de respostas excessivas, duplicadas e redirecionamentos |
| 4.1 / contrato Python/API | A | `faceEmbedding`, `faceModel`, `credentialType`, `machineModel`, `entry` |
| 4.2 / validação, desconhecidos e malformados | P | Bean Validation e normalização; erros sem histórico de negativa semântica e sem limite geral do corpo |
| 4.3 / timeouts, rede e respostas inválidas | P | Falha fecha acesso; redirects HTTP e JSON duplicado precisam endurecimento |
| 4.4 / identidade real e ausência de duplicação do reconhecimento | A | Extração em Python; comparação/autorização exclusivamente no backend |
| 4.5 / HTTP e erros seguros | P | Erros de negócio tratados; falta padronizar indisponibilidade, erros internos e filtros |
| 5.1 / driver, ambiente e runtime MySQL | A | Connector/J runtime, conexão por ambiente; H2 restrito aos testes |
| 5.2 / persistência única em MySQL | P | Existe `database.py` SQLite antigo sem uso no fluxo, mas ainda executável |
| 5.3 / entidades, relações, FKs e unicidade CPF/email/RFID | A | Restrições JPA/MySQL existentes; validar também em MySQL isolado |
| 5.4 / migração segura e falhas de conexão | P | `ddl-auto=update` de desenvolvimento; falta roteiro aditivo e teste de indisponibilidade |
| 5.5 / referências PostgreSQL | V | Busca a concluir e documentar, sem troca de banco |
| 6.1 / criar, consultar, listar e editar usuários | P | Criar/consultar existem; listar/editar não existem |
| 6.2 / perfis e campos obrigatórios | P | Perfis separados; limites e validação de senha em bytes insuficientes |
| 6.3 / unicidade e transação de cadastro | A | Índices CPF/email e cadastro completo transacional; rollback já testado |
| 6.4 / hash e DTOs sem segredos | A | BCrypt, respostas com presença de credencial sem vetor/hash |
| 6.5 / usuário inexistente ou inativo não autoriza | P | Negativa ocorre; falta preservar identidade/motivo para face e código inativos |
| 6.6 / consistência na edição | N | Não há operação de edição |
| 7.1 / cadastro e uso posterior da face | A | Fluxo DeepFace/API já integrado; verificar com testes sintéticos |
| 7.2 / credencial ausente, inválida ou duplicada | P | RFID duplicado bloqueado; falta tratar faces/códigos repetidos e registros corrompidos |
| 7.3 / vínculo e matrícula ativa | P | FKs presentes; filtros antecipados prejudicam auditoria e ambiguidades |
| 7.4 / código secreto seguro | P | BCrypt, mas código repetido pode identificar arbitrariamente outro usuário |
| 8.1 / decisão exclusivamente no backend e sem nome confiado | A | `AccessService.validate` e entidades persistidas |
| 8.2 / negativa segura em todas as falhas | P | Não há fallback positivo; faltam cenários de banco/log/exceção e configuração inválida |
| 8.3 / motivos e resultado estruturados | P | `GRANTED`/`DENIED` existentes; erros de infraestrutura precisam contrato seguro |
| 9.1 / histórico com vínculo, data, modalidade, resultado, distância e motivo | A | `AccessLog`/`AccessLogService` integrados à transação de decisão |
| 9.2 / desconhecido sem usuário | A | `enrollment` nulo permitido |
| 9.3 / negativas relevantes e falha de gravação | P | Não encontrado é registrado; negativas semânticas e falha de gravação não cobertas |
| 9.4 / consulta, ordenação e paginação | P | Paginação existe; desempate temporal e tamanho máximo precisam verificação |
| 10.1 / autenticação administrativa e permissões | P | JWT e chave de serviço; validar vínculo do token ao ID e revisão de credenciais |
| 10.2 / configuração inicial concorrente | P | Exclusão por instância Java, sem garantia suficiente entre transações concorrentes |
| 10.3 / dados biométricos, arquivos e parâmetros | P | Sem fotos no backend; falta consolidar limites de arquivos/JSON e transporte |
| 10.4 / segredos e erros sem informação interna | P | Ambiente utilizado; falta padronização de erros de infraestrutura e verificação de logs |
| 10.5 / vulnerabilidades de dependências | V | `pip check` inicial acusa três dependências ausentes; análise de avisos a executar |
| 11 / testes críticos e ambientes independentes | P | Linha de base: 44 testes Python; Java em execução. Faltam múltiplas negativas e MySQL isolado |
| 12–13 / implementação, evidências, pendências e ações manuais | P | Este arquivo registra linha de base; resultados finais serão atualizados após execução |

## Prioridade de correção

1. Preservar identidade em toda decisão, bloquear credenciais ambíguas/inconsistentes, garantir falha fechada e histórico antes de qualquer resposta positiva.
2. Endurecer tokens, inicialização, limites e erros sem expor dados; manter a política existente de funcionário administrativo.
3. Completar operações de usuário e padronizar validações e fotos; desativar execução do caminho SQLite sem apagar seus dados.
4. Executar contratos e falhas em testes sintéticos, verificar MySQL em esquema isolado e registrar riscos de dependências.

## Resultados finais

## Resultado final e correções

A auditoria foi executada no código real, sem recriar o projeto. O fluxo validado termina na decisão e no histórico. A política existente foi mantida: funcionário administra, aluno não; não foram criadas regras por curso, horário ou instituição.

Já funcionavam antes: extração DeepFace, captura OpenCV em thread separada, interface automática, comunicação por embedding versionado, BCrypt, JWT/chave de serviço, criação transacional de cadastro, RFID, decisão no Spring e histórico com vínculo opcional. A linha de base executada confirmou 44 testes Python e 9 Java.

Problemas corrigidos:

- Identificação facial/código filtrava matrículas inativas cedo demais. Agora identifica antes de verificar estado e registra o vínculo e motivo da negativa.
- Vetores inválidos, dimensões incompatíveis e credenciais ambíguas são negados. O cadastro impede reutilizar o mesmo vetor normalizado, código ou RFID em outra matrícula. A checagem facial de duplicidade é numérica, não uma garantia de deduplicação de pessoas por fotos diferentes.
- O limiar agora tem fallback único de 0,5 e validação no início. Não foram trocados modelo/métrica nem aumentado limiar para produzir sucesso.
- Negativas semânticas são persistidas. O histórico usa flush e transação antes da resposta; indisponibilidade de consulta, gravação ou commit não produz GRANTED.
- JWT passa a validar ID persistido, email, papel e revisão de autenticação. Troca de senha invalida tokens anteriores; reaproveitar email de outra identidade não reaproveita sua sessão.
- Listagem e edição completa de usuário foram implementadas, com unicidade, perfil consistente, senha opcional segura e tipo de usuário imutável.
- Configuração inicial e verificação de duplicidade de face/código usam isolamento SERIALIZABLE. Cadastro completo mantém a mesma transação. A inicialização concorrente foi incluída nos testes MySQL.
- Requisições limitadas a 64 KiB, strings/vetores com limites, rejeição de campos desconhecidos/duplicados e erros HTTP estruturados sem detalhes internos.
- Cliente exige HTTPS fora de loopback, não segue redirects, limita respostas e rejeita JSON ambíguo/não finito, campos inválidos, timeout e falhas HTTP.
- Foto tem validação compartilhada entre formulário e CLI: leitura limitada, dimensões antes de decodificar, exatamente um rosto, enquadramento e vetor válidos.
- Caminho antigo de persistência local foi desativado sem apagar arquivos existentes. Runtime de persistência permanece MySQL; H2 existe somente em testes.
- Dependências incompletas foram instaladas. Foram aplicadas versões corretivas de Jackson, Tomcat, urllib3 e setuptools; novas verificações foram executadas.

### Matriz final por conjunto de requisitos

A = implementado e verificado no escopo indicado; P = atendido com limitação explicitada; V = exige validação externa/manual. A matriz inicial acima foi preservada para distinguir o que já existia.

| Requisitos | Depois | Evidência efetiva |
|---|---|---|
| 1 / preservar arquitetura, tecnologias, nomes e dados reais | A | Spring/PySide6/DeepFace/OpenCV/MySQL mantidos; sem recadastro biométrico ou alteração de registros reais |
| 2 / fluxo integrado de identificação, decisão e histórico | A | Cliente Python real → HTTP → Spring empacotado → MySQL isolado: cadastro, liberação, desconhecido, inativo e 3 registros |
| 3.1–3.2 / bibliotecas efetivamente usadas | A | DeepFace real/ArcFace/ONNX executou inferência de 512 valores em entrada sintética; OpenCV real; chamada de produção represent inspecionada e testada |
| 3.3–3.4 / imagem, ausência, corrupção, captura e exceção | A | Testes Python sintéticos e mocks de câmera/modelo; ausência real em imagem vazia; foto Unicode, limites, arquivo inválido, zero/múltiplos rostos |
| 3.5–3.8 / distância, limiar, dimensão, modelo e identidade | A | Testes de rosto compatível, desconhecido/distante, modelos distintos, vetor zero/não finito/incorreto e empate; nome vem do banco |
| 3.9 / formato e falha fechada | A | Contrato de cliente e HTTP; respostas inválidas e falhas não confirmam identidade |
| 4 / comunicação, validação, vinculação e HTTP | A | Testes HTTP de chave/JWT, JSON malformado/forjado/duplicado, corpo excessivo, negativas e indisponibilidade; POST sem repetição automática |
| 5.1–5.3 / MySQL, persistência, entidades e relações | A | Connector/J real, metadados MySQL, gravação de credenciais/JSON/vínculos e decisões em esquema separado; unicidade e transações exercitadas |
| 5.4 / esquema e falhas de banco | A | Conexão sem resposta via JDBC é recusada; falha de consulta/histórico retorna 503 DENIED; roteiro aditivo em MIGRATION.md |
| 5.5 / compatibilidade de banco | A | Busca sem runtime PostgreSQL; persistência local legada inexecutável |
| 6 / criar, consultar, editar, listar, hash e consistência | A | Testes integrados de CRUD previsto, unicidade de email, rejeição de troca de papel, hash, revogação e rollback de cadastro |
| 7 / modalidades, duplicidade e cadastro inativo | A | Face de 512 valores persistida e reutilizada, RFID normalizado, código BCrypt; duplicatas recusadas inclusive inativas |
| 8 / decisão segura no backend | A | Identidade e matrícula válidas/ativas, modalidade e limiar; falha de consulta ou histórico nunca devolve liberação |
| 9 / negativas, desconhecidos e histórico paginado | A | Vínculo nulo para desconhecido; motivo de inativo; campos sem biometria/segredos; desempate por ID e paginação testados |
| 10.1–10.4 / autenticação, autorização, parâmetros e segredos | A | JWT vinculado à identidade/revisão, token expirado recusado, chave restrita à validação, aluno sem administração, erros seguros e limites |
| 10.2 / inicialização concorrente | A | Duas chamadas concorrentes reais no MySQL, apenas um administrador confirmado; não equivale a teste de carga/múltiplos processos |
| 10.5 / dependências conhecidas | A | Consulta OSV de 155 pacotes resolvidos: alertas corrigidos e nova consulta sem ocorrências; não constitui garantia de ausência de vulnerabilidades |
| 11 / testes críticos | A | Suítes Java/Python, integração MySQL e HTTP e diagnóstico DeepFace executados; dados sintéticos, sem câmera/credenciais reais nas suítes |
| 12–13 / correção, evidências e relatório | A | Implementação, build, testes, matriz, migração, arquivos e pendências registrados |
| Precisão biométrica em pessoas/cenários reais | V | Testes sintéticos não medem falso positivo/negativo, iluminação, posicionamento ou qualidade de câmera |
| Operação endurecida de produção | P | TLS externo, proteção operacional, retenção, escala e políticas adicionais dependem da implantação; ver riscos abaixo |

### Testes e resultados reais

Resultado final Java: **41 testes aprovados, zero falhas, zero erros e zero ignorados**, com mvnw.cmd verify concluído e JAR empacotado. Destes, 11 executaram no MySQL real isolado (10 de integração/indisponibilidade e 1 de bootstrap concorrente); 30 cobrem unidade, HTTP com mocks e integração H2. Os XMLs Surefire foram conferidos individualmente.

| Suíte Java | Testes | Resultado |
|---|---:|---|
| AdaptFaceApiApplicationTests | 1 | aprovado |
| AdaptFaceFlowIntegrationTests | 1 | aprovado |
| RegistrationIntegrationTests | 1 | aprovado |
| AccessServiceTests | 14 | aprovados |
| AccessFailureHttpTests | 2 | aprovados |
| RequestBodyLimitFilterTests | 2 | aprovados |
| SecurityAuditIntegrationTests | 9 | aprovados |
| MySqlAuditIntegrationTests | 10 | aprovados em MySQL |
| BootstrapConcurrencyMySqlTests | 1 | aprovado em MySQL |

Os resultados intermediários não foram omitidos: primeiras execuções ampliadas falharam por fixtures antigas de 3 posições/sem IDs, um import incorreto no teste novo e expectativas de contrato/paginação. As fixtures foram corrigidas sem desabilitar testes ou relaxar validações.

- Python: **58 testes aprovados**, zero falhas, com QT_QPA_PLATFORM=offscreen e python.ps1 -m unittest discover -s tests.
- pip check: **No broken requirements found** no ambiente selecionado pelo launcher.
- DeepFace real: inferência ArcFace/ONNX de 512 valores finitos em imagem sintética e imagem vazia sem rosto, ambas aprovadas. O detector skip foi usado apenas no diagnóstico de inferência com pixels artificiais, nunca no fluxo de produção.
- HTTP real: **aprovado**. scripts/audit_http_contract.py usou o BackendClient real e um servidor Spring temporário em loopback com DB_SCHEMA_MODE=validate e MySQL isolado. Criou duas identidades sintéticas, cadastrou vetor, recebeu GRANTED para a correta, DENIED para desconhecido e para matrícula desativada, e confirmou três registros ordenados.
- Segurança de dependências: **155 pacotes consultados, zero alertas na consulta final OSV** em 2026-10-19. A consulta envia somente nomes/versões. É uma análise de avisos públicos, não teste de exploração nem auditoria de binários/pesos.
- Runtime Java usado: JDK 24, compilação configurada para Java 17. Não foi feita uma execução separada em JDK 17; Maven emite aviso de API interna do Guice no JDK 24, sem erro de build.

As evidências locais geradas ficam em .runtime/audit-final-java.log, target/surefire-reports/TEST-*.xml, .runtime/audit-dependencies-final.json e .runtime/audit-http-server.log. São artefatos ignorados pelo Git. O servidor temporário de auditoria foi encerrado; nenhum processo normal da aplicação foi encerrado.

### Arquivos criados nesta auditoria

Backend:

- config/JsonConfig.java; security/ApiErrorWriter.java; security/RequestBodyLimitFilter.java.
- dto/request/UserUpdateRequestDTO.java.
- SecurityAuditIntegrationTests.java; MySqlAuditIntegrationTests.java; BootstrapConcurrencyMySqlTests.java; service/AccessFailureHttpTests.java; security/RequestBodyLimitFilterTests.java em src/test/java/com/dev/adaptface.
- scripts/audit_dependencies.py; scripts/audit_http_contract.py.
- docs/AUDIT.md; docs/MIGRATION.md.

Python: photo_validation.py e tests/test_audit_validation.py.

### Arquivos alterados nesta auditoria

Caminhos Java relativos a src/main/java/com/dev/adaptface:

- service/AccessService.java, CredentialService.java, AccessLogService.java, RegistrationService.java, UserService.java, AuthService.java.
- repository/CredentialRepository.java, AccessLogRepository.java, UserRepository.java.
- entity/User.java, AccessLog.java.
- controller/UserController.java; config/SecurityConfig.java; exceptions/GlobalExceptionHandler.java.
- security/JwtService.java, JwtAuthenticationFilter.java, MachineApiKeyFilter.java.
- dto/request/AccessValidationRequestDTO.java, CredentialFaceRequestDTO.java, RegistrationRequestDTO.java, UserRequestDTO.java, LoginRequestDTO.java.
- dto/response/CredentialResponseDTO.java, ErrorResponseDTO.java.

Demais backend: pom.xml, src/main/resources/application.properties, src/test/java/com/dev/adaptface/service/AccessServiceTests.java, AdaptFaceFlowIntegrationTests.java, RegistrationIntegrationTests.java, README.md, docs/STRUCTURE.md.

Python: backend_client.py, recognizer.py, registration_window.py, enroll_face.py, database.py, config.py, requirements.txt, README.md, tests/test_deepface_recognizer.py. Os pacotes locais de runtime também foram completados/atualizados; não são código-fonte versionado.

### Configuração e ações manuais

1. Backend: DB_URL/DB_USER/DB_PASSWORD apontando MySQL; JWT_SECRET Base64 de chave forte; MACHINE_API_KEY e BOOTSTRAP_KEY com ao menos 32 caracteres. Não coloque segredo na URL JDBC, no repositório ou na linha de comando. Use variáveis de ambiente/gestão de segredos.
2. Inicie com mvnw.cmd spring-boot:run. Para instalações existentes, revise docs/MIGRATION.md e use DB_SCHEMA_MODE=validate após backup e migração aprovada. Não use create/create-drop no banco real.
3. Python: mesma MACHINE_API_KEY e ADAPTFACE_API_URL, depois python.ps1 app.py; cadastro com python.ps1 admin.py. Reinicie processos antigos para carregar as bibliotecas corrigidas.
4. Crie o primeiro administrador uma única vez com bootstrap; demais cadastros exigem JWT de funcionário. Edição/listagem estão implementadas na API; não foi adicionada uma nova tela de edição nesta auditoria.
5. Testes comuns: mvnw.cmd verify e, na raiz Python, python.ps1 -m unittest discover -s tests. Testes MySQL exigem esquema **novo e vazio**, prefixo adaptface_audit_<hex>, URL local em AUDIT_MYSQL_URL e usuário/senha em AUDIT_MYSQL_USER/AUDIT_MYSQL_PASSWORD. Os testes são condicionais quando essas variáveis não existem; a rodada registrada neste relatório os habilitou.
6. O teste de bootstrap concorrente deixa apenas uma conta sintética no esquema isolado. O teste HTTP também deixa somente seus registros sintéticos nesse esquema, nunca no banco real. Foram preservados os esquemas de auditoria; sua remoção não foi executada.
7. Para reproduzir a consulta OSV, gere dependency:list com includeScope=runtime e outputFile em .runtime e execute scripts/audit_dependencies.py pelo launcher Python, passando esse arquivo. Instalação nova pode resolver versões diferentes; repita a consulta.

### Pendências e riscos conhecidos

- Credenciais legadas permanecem separadas por modelo. É necessário recadastrar cada face com a foto correta/autorizada; nenhuma credencial real foi alterada nesta auditoria. Não vincule foto a uma pessoa arbitrária.
- O limiar de cosseno preservado precisa de calibração com dados representativos e consentidos antes de uso real. Não há medição de precisão nesta auditoria nem garantia biométrica por teste sintético.
- Não há mecanismo validado de vivacidade/antifraude. Uma foto apresentada à câmera pode não ser distinguida de uma pessoa; não trate o protótipo como autenticação resistente a apresentação.
- Busca facial e verificação BCrypt de códigos são lineares no número de credenciais. Não foi feito teste de carga, validação multi-instância ou dimensionamento. SERIALIZABLE pode gerar contenção/deadlock; erros resultam em negativa/indisponibilidade, não em liberação.
- Administração usa a política existente EMPLOYEE. Se nem todo funcionário puder administrar, essa decisão de negócio precisa ser definida antes de mudar os papéis.
- A API key compartilhada autentica o cliente, não prova que o embedding veio de uma câmera confiável. Comprometimento da máquina ou da chave pode forjar/reutilizar vetores. Proteja ambiente, segredos e transporte.
- TLS para acesso remoto, restrição de rede, limitação de tentativas no proxy, conta MySQL mínima, backup/restauração, retenção/eliminação de biometria e proteção criptográfica operacional dependem de implantação. Não foram declarados como implementados.
- Dados biométricos não aparecem em DTOs comuns/histórico, mas o embedding persistido continua dado sensível; requer controle de acesso ao banco e proteção do armazenamento.
- Sem MySQL não é possível registrar uma tentativa no próprio MySQL. O comportamento é DENIED/503 e nenhuma liberação; não foi criada fila offline que altere essa regra.
- JSON estruturalmente inválido, não autenticado ou excessivo é recusado antes da regra de acesso e não gera AccessLog. A ausência de rosto em câmera ociosa não gera uma tentativa por frame.
- O ambiente Python portátil preserva diretórios legados. O launcher prioriza as bibliotecas corrigidas; executar por outro interpretador pode usar versões antigas. Recomenda-se ambiente limpo para distribuição, com verificação repetida de dependências.
- Atualizações aplicadas: Jackson 2.21.7/3.1.7, Tomcat 11.0.25, urllib3 2.8.0, setuptools 84.0.0. Não foi alterada a versão principal do Spring nem o modelo DeepFace. Pesos existentes não passaram por uma validação externa de cadeia de fornecimento.
- A auditoria não é certificação de segurança, conformidade ou adequação a produção.

### Referências primárias consultadas

[API OSV](https://google.github.io/osv.dev/post-v1-querybatch/) para a consulta de pacotes e paginação; [isolamento InnoDB](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html) para a análise transacional. Atualizações corretivas conferidas nos avisos dos mantenedores: [Jackson](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf), [Tomcat](https://tomcat.apache.org/security-11.html), [urllib3](https://github.com/urllib3/urllib3/security/advisories/GHSA-8988-9cw3-xx77), [setuptools](https://github.com/pypa/setuptools/security/advisories/GHSA-h35f-9h28-mq5c).
