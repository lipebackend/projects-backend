# Roadmap de Microtarefas — Task Management API

> **Para:** Pedro Barbosa (implementação solo)  
> **Projeto:** `01-simple-rest-api-task-management`  
> **Tipo:** documentação de aprendizado — **não** é um checklist de “copie e cole código pronto”.  
> **Referência:** `docs/ARCHITECTURE-V2.md` (PostgreSQL = fonte da verdade; Outbox; Redis cache-aside+evict; Rabbit só via Outbox Relay). Marque cada `- [ ]` ao concluir.

---

## Como usar este roadmap

1. **Uma microtarefa por sessão.** Termine o critério de aceite antes de abrir a próxima.
2. **Leia o Objetivo e os Pré-requisitos.** Se algo do pré-requisito não estiver verde, volte uma tarefa.
3. **Os Passos orientam o que criar/alterar** — não entregam a solução completa. Pesquise, experimente, quebre e corrija.
4. **Critério de aceite = definição de pronto da sessão.** Só marque como feita quando você (ou um teste) puder demonstrar o aceite.
5. **Quando pedir ajuda:** traga o ID da tarefa (ex.: T07), o que você tentou, o erro/log, e o trecho que você escreveu — não peça “faz pra mim”.
6. **Não altere várias fases de uma vez.** O caminho intencional é: alinhar stubs → DB → outbox → cache thread-safe → messaging wired → deploy consistente → hardening HTTP/testes.
7. **Placeholders Architecture V2:** onde aparecer *conforme Architecture V2*, escolha por enquanto a opção “típica” sugerida na tarefa (ex.: PostgreSQL + JDBC + Flyway). Quando o V2 sair, renomeie/troque tecnologias para bater com o doc — sem reescrever o restante do roadmap.

### Estado atual (baseline da revisão)

| Área | Situação hoje |
| :--- | :--- |
| CRUD / HTTP / Clean Architecture | **Sólido** — `TaskHandler` → `TaskService` → `InMemoryTaskRepository` |
| `Main` | Wire **somente** InMemory; Redis/Rabbit **não** ligados |
| `JedisTaskCache` / `RabbitQueuePublisher` | Classes existem; compose sobe Redis/Rabbit; **não** usados em runtime |
| Thread-safety (P1) | Jedis single-instance e Channel compartilhado **não** são seguros sob Virtual Threads se ligados assim |
| Deploy (P0) | `deployment.yaml` com `replicas: 3` + dados em memória; imagem `task-management:latest` **≠** imagem do compose (`task-management-api`) |
| Hardening (P2) | Falta `Location` no 201, max length de `title`, URL-decode do path, distinguir melhor 400 vs 500, `-DskipTests` no Docker, testes de concorrência extras |

### Caminho de migração (visão geral)

```
InMemory (hoje)
    → DB (JDBC + Flyway — conforme Architecture V2)
    → Outbox anti dual-write
    → Cache Redis thread-safe
    → Messaging (publisher wired de forma segura)
    → Deploy consistente (imagem, réplicas, health)
    → Hardening HTTP + testes
```

---

## Índice-mestre (marque ao concluir)

- [ ] **T01** — Inventário honesto: docs vs código vs compose
- [ ] **T02** — Decidir stubs e feature flags mentais
- [ ] **T03** — Contratos limpos antes de persistir
- [ ] **T04** — Subir o banco no compose (sem ainda trocar o Main)
- [ ] **T05** — Flyway (ou migrador do V2) + schema inicial `tasks`
- [ ] **T06** — `JdbcTaskRepository` implementando `TaskRepository`
- [ ] **T07** — Wire no `Main`: escolher InMemory vs JDBC por config
- [ ] **T08** — Entender dual-write e desenhar a tabela outbox
- [ ] **T09** — Migration da outbox + gravação na mesma transação do Task
- [ ] **T10** — Relay / publisher: ler outbox e publicar no broker
- [ ] **T11** — Thread-safety do Channel AMQP (P1) antes de escala
- [ ] **T24** — Consumer Rabbit idempotente (ack + dedup por event id)
- [ ] **T12** — Wire opcional do cache atrás do serviço (read-through / aside)
- [ ] **T13** — Trocar Jedis “solto” por pool thread-safe (P1)
- [ ] **T14** — Invalidação e consistência cache ↔ DB
- [ ] **T15** — Alinhar nome da imagem (compose ↔ Dockerfile ↔ deployment)
- [ ] **T16** — Réplicas só com estado compartilhado + probes
- [ ] **T17** — Dockerfile: testes no CI vs skip no build de imagem
- [ ] **T18** — Header `Location` no `201 Created`
- [ ] **T19** — Limite máximo de `title` (validação de domínio)
- [ ] **T20** — URL-decode do `{id}` no path
- [ ] **T21** — 400 vs 500: JSON de erro previsível
- [ ] **T22** — Testes de concorrência e regressão final
- [ ] **T23** — (Opcional) Paginação `limit`/`offset`

---

## Fase A — Alinhar o que existe hoje

> Meta: documentação, compose e `Main` contarem a mesma história. Decidir o que fica **stub** até a fase correspondente.

- [ ] **T01** — Inventário honesto: docs vs código vs compose

- **Objetivo:** Registrar, em um parágrafo no próprio `MICROTAREFAS.md` (seção “Notas do Pedro”) ou em `docs/NOTES.md`, o que está implementado, o que é stub e o que o compose promete.
- **Pré-requisitos:** Nenhum.
- **Passos:**
  1. Abra `Main.java`, `compose.yaml`, `deployment.yaml`, `JedisTaskCache`, `RabbitQueuePublisher` e o README.
  2. Liste: (a) o que o `Main` instancia de fato; (b) serviços no compose com env vars (`REDIS_*`, `RABBITMQ_*`); (c) classes de infra que existem mas não são injetadas.
  3. Escreva 5–10 bullets “realidade vs expectativa”. Não mude código ainda.
- **Critério de aceite:** Você consegue explicar em voz alta por que Redis/Rabbit no compose **não** afetam o comportamento atual da API.
- **Testes sugeridos:** Nenhum (só leitura). Opcional: `./http.bash` contra a API local para confirmar o baseline CRUD.
- **Estimativa:** S

- [ ] **T02** — Decidir stubs e feature flags mentais

- **Objetivo:** Definir a ordem de ativação: DB primeiro, depois outbox/messaging, depois cache — e o que permanece stub até lá.
- **Pré-requisitos:** T01.
- **Passos:**
  1. Decida: `InMemoryTaskRepository` continua como implementação padrão local até T05+.
  2. Decida: `JedisTaskCache` e `RabbitQueuePublisher` **não** entram no `Main` antes das fases D e C/E respectivamente (conforme Architecture V2).
  3. Anote no seu NOTES: “ligar X só depois de Y”.
  4. Se `docs/ARCHITECTURE-V2.md` aparecer, relia e ajuste esta decisão — não invente wiring antecipado.
- **Critério de aceite:** Uma tabela curta “componente → status (ativo/stub) → fase que ativa”.
- **Testes sugeridos:** Nenhum.
- **Estimativa:** S

- [ ] **T03** — Contratos limpos antes de persistir

- **Objetivo:** Revisar `TaskRepository`, `TaskCache` e `TaskQueuePublisher` e listar métodos que o DB/outbox/cache precisarão respeitar.
- **Pré-requisitos:** T02.
- **Passos:**
  1. Leia as três interfaces em `domain/contract`.
  2. Anote se o repositório precisará de algo extra para transações/outbox (ex.: operação na mesma conexão) — **conforme Architecture V2**; por enquanto só documente a hipótese.
  3. Não altere assinaturas ainda, a menos que o V2 diga explicitamente.
- **Critério de aceite:** Lista de “gaps de contrato” com no máximo uma página; zero mudança de comportamento da API.
- **Testes sugeridos:** `mvn test` deve continuar verde sem mudanças (smoke).
- **Estimativa:** S

---

## Fase B — Persistência com DB

> Meta: sair de memória para um banco real (stack típica: **PostgreSQL + JDBC + Flyway + pool** — **conforme Architecture V2**). Manter `TaskRepository` como fronteira.

- [ ] **T04** — Subir o banco no compose (sem ainda trocar o Main)

- **Objetivo:** Adicionar o serviço de banco ao `compose.yaml` (e volume/rede) com variáveis de conexão documentadas.
- **Pré-requisitos:** T03; Docker/Podman disponível.
- **Passos:**
  1. Escolha a imagem/porta **conforme Architecture V2** (placeholder típico: PostgreSQL 16 Alpine na 5432).
  2. Adicione volume persistente e healthcheck básico do container de DB.
  3. Exporte env vars para a API (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — nomes finais conforme V2).
  4. **Ainda não** mude o `Main` para usar JDBC.
- **Critério de aceite:** `compose up` sobe o DB; você conecta com um cliente CLI e roda `SELECT 1`.
- **Testes sugeridos:** Healthcheck do compose passando; sem regressão da API InMemory.
- **Estimativa:** S

- [ ] **T05** — Flyway (ou migrador do V2) + schema inicial `tasks`

- **Objetivo:** Criar a primeira migration com a tabela de tarefas alinhada ao modelo `Task`.
- **Pré-requisitos:** T04.
- **Passos:**
  1. Adicione a dependência do migrador **conforme Architecture V2** (típico: Flyway).
  2. Crie `V1__create_tasks.sql` (ou equivalente) com colunas para `id`, `title`, `completed`, `created_at` — tipos e constraints alinhados ao domínio.
  3. Decida PK (`id` UUID/string) e índices úteis (ex.: `completed`).
  4. Rode as migrations contra o DB do compose (CLI ou small main auxiliar — sem substituir o repositório ainda).
- **Critério de aceite:** Schema aplicado; tabela vazia existe; re-rodar migration é no-op (idempotente via tool).
- **Testes sugeridos:** Conferir `flyway_schema_history` (ou equivalente); dropar volume e subir de novo.
- **Estimativa:** M

- [ ] **T06** — `JdbcTaskRepository` implementando `TaskRepository`

- **Objetivo:** Implementar o repositório JDBC (ou API do V2) cobrindo `findAll`, `findById`, `save`, `update`, `deleteById`, `existsById`.
- **Pré-requisitos:** T05.
- **Passos:**
  1. Crie a classe em `infra/repository` (nome **conforme Architecture V2**).
  2. Use pool de conexões (HikariCP típico — conforme V2).
  3. Mapeie `ResultSet` → `Task` com cuidado com `Instant`/timestamp.
  4. Em `update`, preserve atomicidade o máximo possível (optimistic lock simples ou `UPDATE ... WHERE id = ?` retornando row count).
  5. **Não** delete o `InMemoryTaskRepository` — ele continua útil para testes unitários rápidos.
- **Critério de aceite:** Testes de repositório (unitários com Testcontainers **ou** DB do compose) passam para CRUD básico.
- **Testes sugeridos:** Salvar → buscar; update inexistente → empty; delete; filtro `completed`.
- **Estimativa:** L

- [ ] **T07** — Wire no `Main`: escolher InMemory vs JDBC por config

- **Objetivo:** Permitir trocar a implementação via env/flag sem recompilar a lógica de negócio.
- **Pré-requisitos:** T06.
- **Passos:**
  1. Introduza algo como `STORAGE=memory|jdbc` (nome **conforme Architecture V2**).
  2. No `Main`, instancie o repositório correspondente; rode migrations na subida se JDBC.
  3. Atualize README com as env vars mínimas.
  4. Mantenha o caminho feliz local em memória se quiser (default documentado).
- **Critério de aceite:** Com `STORAGE=jdbc` + compose, `./http.bash` passa; reiniciar a API **mantém** as tarefas no DB.
- **Testes sugeridos:** Criar tarefa, restart do container `api`, `GET` pelo mesmo id.
- **Estimativa:** M

---

## Fase C — Outbox / eventos sem dual-write

> Meta: quando uma tarefa muda, o evento não se perde se o broker falhar. Evitar “grava no DB + publica no Rabbit” em duas etapas soltas.

- [ ] **T08** — Entender dual-write e desenhar a tabela outbox

- **Objetivo:** Escrever (em NOTES) o problema do dual-write e o desenho da tabela `outbox` (ou nome do V2).
- **Pré-requisitos:** T07.
- **Passos:**
  1. Leia sobre *Transactional Outbox* (conceitualmente).
  2. Desenhe colunas típicas: `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `created_at`, `published_at` (ajuste **conforme Architecture V2**).
  3. Defina quais eventos existem já no contrato: created / updated / deleted.
  4. Ainda não implemente publisher em produção.
- **Critério de aceite:** Diagrama textual “request → TX (tasks + outbox) → relay → Rabbit”.
- **Testes sugeridos:** Nenhum de código.
- **Estimativa:** S

- [ ] **T09** — Migration da outbox + gravação na mesma transação do Task

- **Objetivo:** Ao criar/atualizar/apagar tarefa no JDBC, inserir linha na outbox **na mesma transação**.
- **Pré-requisitos:** T08.
- **Passos:**
  1. Crie `V2__create_outbox.sql` (ou equivalente V2).
  2. No serviço **ou** no repositório (decisão **conforme Architecture V2**), abra uma transação, persista `Task` e a mensagem outbox.
  3. Payload JSON deve ser suficiente para o consumidor futuro (id, title, completed, createdAt, tipo do evento).
  4. Em modo InMemory, ou ignore outbox, ou use no-op publisher — documente a escolha.
- **Critério de aceite:** Após um `POST /tasks` com JDBC, existe 1 linha pending na outbox; rollback de falha artificial não deixa task sem outbox (nem o contrário).
- **Testes sugeridos:** Teste de integração forçando falha após insert da task (se possível) e verificando atomicidade.
- **Estimativa:** L

- [ ] **T10** — Relay / publisher: ler outbox e publicar no broker

- **Objetivo:** Um componente periódico (ou thread dedicada) publica eventos pending e marca `published_at`.
- **Pré-requisitos:** T09; Rabbit no compose.
- **Passos:**
  1. Reuse ideias de `RabbitQueuePublisher`, mas **não** publique direto do request thread sem outbox.
  2. Trate falha de broker: mensagem permanece pending; retry com backoff simples.
  3. Considere idempotência do publish (pelo menos não duplicar marca de published sem ack — **conforme Architecture V2**).
  4. Registre o relay no shutdown hook (parar limpo).
- **Critério de aceite:** Criar tarefa → linha outbox → mensagem aparece na fila `task.created` (ou nome V2); matar Rabbit no meio e subir de novo eventualmente drena a outbox.
- **Testes sugeridos:** Teste manual com Management UI do Rabbit; teste automatizado com broker efêmero se o V2 indicar.
- **Estimativa:** L

- [ ] **T11** — Thread-safety do Channel AMQP (P1) antes de escala

- **Objetivo:** Garantir que o uso do client Rabbit seja seguro sob Virtual Threads / concorrência.
- **Pré-requisitos:** T10.
- **Passos:**
  1. Leia a doc do client: `Channel` **não** é thread-safe.
  2. Escolha abordagem **conforme Architecture V2** (ex.: channel por thread via `ThreadLocal`, pool de channels, ou serializar publishes num único worker).
  3. Ajuste `RabbitQueuePublisher` (ou sucessor) e o relay.
  4. Feche connection/channel no `close()` sem vazar.
- **Critério de aceite:** Stress leve (vários POST paralelos) sem exceções de channel; revisão do código mostra política explícita de concurrency.
- **Testes sugeridos:** Teste de concorrência publicando N eventos; sem `AlreadyClosedException` intermitente.
- **Estimativa:** M

---


- [ ] **T24** — Consumer Rabbit idempotente (ack + dedup por event id)
  - **Pré-requisitos:** T10 e T11 (relay publicando eventos de forma thread-safe).
  - **Passos:**
    1. Crie um consumer de exemplo (classe separada ou main auxiliar) que leia a fila/exchange definidos no Architecture V2.
    2. Faça ack só depois de processar; em reentrega, use o `event id` (ou chave de idempotência) para não aplicar o mesmo efeito duas vezes (tabela de processed_events ou set em memória só para lab).
    3. Logue create/update/delete recebidos; não publique de volta no request path.
  - **Aceite:** Publicar o mesmo evento duas vezes não duplica o efeito colateral do consumer; ack confirma a mensagem.
  - **Testes:** Teste de integração ou script: publish duplicado → consumer processa uma vez útil; falha antes do ack → reentrega e dedup.
  - **Tamanho:** M

## Fase D — Cache certo (Redis thread-safe)

> Meta: cache é **acelerador**, não fonte da verdade. Invalidação correta + Jedis seguro.

- [ ] **T12** — Wire opcional do cache atrás do serviço (read-through / aside)

- **Objetivo:** Integrar `TaskCache` no fluxo de leitura/escrita **sem** quebrar o DB como source of truth.
- **Pré-requisitos:** T07 (DB estável). Messaging pode estar em paralelo, mas cache não depende de outbox.
- **Passos:**
  1. Defina política **conforme Architecture V2** (típico: cache-aside no `getById`; `put` após write; `evict` no delete/update).
  2. Injete `TaskCache` no `TaskServiceImpl` (ou decorator — conforme V2) com implementação no-op para quando Redis estiver off.
  3. Env `CACHE_ENABLED=true|false` + `REDIS_HOST`/`REDIS_PORT`.
  4. Não cacheie listagens no primeiro momento (evita invalidação complexa), a menos que o V2 peça.
- **Critério de aceite:** Com cache on: segundo `GET /tasks/{id}` não precisa bater no DB (prove com log ou contador); update/delete remove/atualiza a chave.
- **Testes sugeridos:** Teste com fake `TaskCache` in-memory; integração opcional com Redis do compose.
- **Estimativa:** M

- [ ] **T13** — Trocar Jedis “solto” por pool thread-safe (P1)

- **Objetivo:** Eliminar o risco de um único `Jedis` compartilhado entre Virtual Threads.
- **Pré-requisitos:** T12.
- **Passos:**
  1. Use `JedisPool` / `JedisPooled` (ou API recomendada na versão do pom — **conforme Architecture V2**).
  2. Em cada operação: borrow → comando → return (try-with-resources se aplicável).
  3. Configure timeouts e tamanho de pool modestos para aprendizado.
  4. Feche o pool no shutdown hook.
- **Critério de aceite:** Código não mantém `Jedis` de longa vida compartilhado; teste paralelo de get/put/evict passa.
- **Testes sugeridos:** Concorrência com dezenas de threads batendo no cache; sem erros de protocolo Redis.
- **Estimativa:** M

- [ ] **T14** — Invalidação e consistência cache ↔ DB

- **Objetivo:** Documentar e implementar a ordem write-DB-then-evict/put para evitar stale reads óbvios.
- **Pré-requisitos:** T13.
- **Passos:**
  1. Escreva a ordem das operações no NOTES (e por que “cache primeiro” é perigoso).
  2. Garanta que falha no Redis **não** falhe o request de negócio (log + segue) — ou siga a política do V2 se for fail-fast.
  3. Adicione TTL opcional se o V2 recomendar.
- **Critério de aceite:** Cenário “update title → GET” devolve valor novo mesmo com cache previamente populado.
- **Testes sugeridos:** Teste de serviço com cache spy verificando `evict`/`put` após update/delete.
- **Estimativa:** S

---

## Fase E — Deploy consistente (imagem, réplicas, health)

> Meta: o que roda no K8s/compose reflete o mesmo artefato e um estado compartilhado (DB), não 3 cérebros InMemory.

- [ ] **T15** — Alinhar nome da imagem (compose ↔ Dockerfile ↔ deployment)

- **Objetivo:** Eliminar o mismatch `task-management:latest` vs `task-management-api`.
- **Pré-requisitos:** T07 (já existe artefato real).
- **Passos:**
  1. Escolha **um** nome de imagem e tag (ex.: `task-management-api:1.0.0`) — **conforme Architecture V2** se houver padrão.
  2. Atualize `compose.yaml`, `compose.debug.yaml` e `deployment.yaml` para o mesmo valor.
  3. Documente no README o comando `build`/`load`/`imagePullPolicy` para cluster local (Minikube/kind/Podman).
- **Critério de aceite:** Um único `podman build -t <nome>:<tag> .` serve tanto para compose quanto para o Deployment.
- **Testes sugeridos:** `compose up --build` e `kubectl apply` (se tiver cluster) puxando a mesma tag.
- **Estimativa:** S

- [ ] **T16** — Réplicas só com estado compartilhado + probes

- **Objetivo:** Tornar `replicas: 3` seguro: DB compartilhado, probes de health, sem depender de memória local.
- **Pré-requisitos:** T07, T15; idealmente T09+ se eventos forem requisito do ambiente.
- **Passos:**
  1. Remova a ideia de escalar horizontalmente com `STORAGE=memory` (documente como proibido em multi-replica).
  2. Adicione endpoint de health/readiness **conforme Architecture V2** (mínimo: process up; ideal: DB ping).
  3. Em `deployment.yaml`: `livenessProbe`/`readinessProbe`, `resources` modestos, env do DB/Redis/Rabbit.
  4. Se ainda não houver Service/Ingress, crie um Service ClusterIP básico apontando para a app.
- **Critério de aceite:** Duas réplicas veem a mesma tarefa criada via uma delas; pod “not ready” se DB estiver down (se você implementou readiness com DB).
- **Testes sugeridos:** Criar task na réplica A, GET na B; derrubar DB e observar readiness.
- **Estimativa:** M

- [ ] **T17** — Dockerfile: testes no CI vs skip no build de imagem

- **Objetivo:** Entender o trade-off de `mvn -DskipTests` no Dockerfile e documentar/ajustar o fluxo.
- **Pré-requisitos:** T15.
- **Passos:**
  1. Decida **conforme Architecture V2**/prática do repo: (a) rodar testes no CI antes do build; (b) stage separado; ou (c) build arg `SKIP_TESTS`.
  2. Ajuste Dockerfile/README para que “imagem de produção” não seja a única linha de defesa dos testes.
  3. Garanta que `mvn test` local continue fácil.
- **Critério de aceite:** README explica claramente onde os testes rodam; imagem ainda builda de forma reproduzível.
- **Testes sugeridos:** Pipeline mental: `mvn test` → `podman build`; falha de teste impede release (mesmo que o Dockerfile skippe).
- **Estimativa:** S

---

## Fase F — Hardening HTTP e testes (P2)

> Meta: polir o contrato HTTP e a suíte de testes sem reescrever a arquitetura.

- [ ] **T18** — Header `Location` no `201 Created`

- **Objetivo:** Em `POST /tasks`, responder `201` com `Location: /tasks/{id}` (URI do recurso criado).
- **Pré-requisitos:** Baseline CRUD (já existe).
- **Passos:**
  1. Em `handleCreate`, sete o header **antes** de `sendResponseHeaders`.
  2. Use path relativo ou absoluto de forma consistente; documente a escolha.
  3. Atualize `http.bash` / teste de integração para assertar o header.
- **Critério de aceite:** Integração verifica status 201 **e** `Location` terminando com o id retornado no body.
- **Testes sugeridos:** `TaskHandlerIntegrationTest` + passo no `http.bash`.
- **Estimativa:** S

- [ ] **T19** — Limite máximo de `title` (validação de domínio)

- **Objetivo:** Rejeitar títulos maiores que um máximo (ex.: 200 chars — confirme **conforme Architecture V2**/README).
- **Pré-requisitos:** Nenhum além do domínio atual.
- **Passos:**
  1. Defina constante única (domínio) e use em `Task` / `TaskServiceImpl`.
  2. Responda `400` com mensagem clara (nunca `500`).
  3. Alinhe coluna DB (`VARCHAR(n)`) na migration se necessário (nova migration, não editar V1 aplicada).
- **Critério de aceite:** Título no limite passa; limite+1 → 400 JSON `{ "error": "..." }`.
- **Testes sugeridos:** Unitário em `Task`/`TaskServiceImpl`; integração HTTP.
- **Estimativa:** S

- [ ] **T20** — URL-decode do `{id}` no path

- **Objetivo:** IDs (ou segmentos) com caracteres percent-encoded sejam decodificados antes do lookup.
- **Pré-requisitos:** T18 recomendável (mesmo arquivo handler).
- **Passos:**
  1. Ao extrair `segments[1]`, aplique decode UTF-8 seguro (`URLDecoder` com charset UTF-8, ou API do `URI`).
  2. Cuidado com `+` vs espaço — prefira a API que respeita path (não query).
  3. Adicione teste com id que exija encode.
- **Critério de aceite:** Request com path encoded encontra a tarefa equivalente ao id decodificado; decode inválido → 400 (não 500).
- **Testes sugeridos:** Integração HTTP com id contendo caractere especial (se o gerador de UUID não bastar, teste o helper de decode isolado + um id fixo no repo fake).
- **Estimativa:** S

- [ ] **T21** — 400 vs 500: JSON de erro previsível

- **Objetivo:** Garantir que erros de cliente nunca virem `500`, e que `500` não vaze stack trace no body.
- **Pré-requisitos:** T19, T20.
- **Passos:**
  1. Revise o `catch` do `TaskHandler`: `ValidationException`, `IllegalArgumentException`, JSON inválido → 400; not found → 404; inesperado → 500 genérico.
  2. Evite mapear excessivamente `Exception` ampla para 400.
  3. Confirme `Content-Type` e formato `ErrorResponse` em todos os erros.
- **Critério de aceite:** Matriz curta no NOTES: exceção → status; testes cobrem pelo menos um 400 e um caminho 500 (mock forçando falha interna, se viável).
- **Testes sugeridos:** JSON quebrado; title blank; id inexistente; (opcional) falha forçada no service.
- **Estimativa:** M

- [ ] **T22** — Testes de concorrência e regressão final

- **Objetivo:** Fechar a fase com testes que protejam race conditions no repositório/serviço e um smoke E2E.
- **Pré-requisitos:** T11–T14 se cache/messaging ligados; senão foque em JDBC/InMemory.
- **Passos:**
  1. Estenda testes de concorrência no repositório (já há base InMemory) para JDBC se estável.
  2. Adicione teste: N patches paralelos no mesmo id não corrompem o estado (lost update detectável ou last-write-wins documentado).
  3. Rode `mvn test` e `./http.bash` com a stack compose (DB ± Redis ± Rabbit conforme o que você ativou).
  4. Atualize o README (contagem de testes / como rodar) **sem** apagar o conteúdo histórico.
- **Critério de aceite:** Suíte verde; script HTTP verde; NOTES com “o que ainda é stub”.
- **Testes sugeridos:** JUnit concorrente + `http.bash` + checklist manual das filas/outbox se C estiver feita.
- **Estimativa:** M

- [ ] **T23** — (Opcional) Paginação `limit`/`offset`

- **Objetivo:** Implementar o desafio extra ainda aberto no README (`limit`/`offset` em `GET /tasks`).
- **Pré-requisitos:** T07 (melhor com DB); validação de query params sólida (T21).
- **Passos:**
  1. Defina defaults e máximos **conforme Architecture V2**.
  2. Propague do handler → service → repository (`LIMIT`/`OFFSET` no SQL; em memória via stream).
  3. Rejeite valores não numéricos/negativos com 400.
- **Critério de aceite:** `GET /tasks?limit=2&offset=0` devolve fatia estável; params inválidos → 400.
- **Testes sugeridos:** Integração com dataset fixo de N tarefas.
- **Estimativa:** M

---

## Mapa rápido por fase

| Fase | IDs | Foco |
| :--- | :--- | :--- |
| **A** Alinhar | T01–T03 | Inventário, stubs, contratos |
| **B** Persistência | T04–T07 | Compose DB, Flyway, JDBC repo, wire |
| **C** Outbox/eventos | T08–T11 | Dual-write, outbox TX, relay, Channel safe |
| **D** Cache | T12–T14 | Cache-aside, Jedis pool, invalidação |
| **E** Deploy | T15–T17 | Imagem única, réplicas+health, skipTests |
| **F** Hardening | T18–T23 | Location, max title, URL-decode, 400/500, concurrency, paginação opcional |

---

## Notas do Pedro

> Use este espaço livremente (checklist pessoal, links do Architecture V2, decisões de nome).

- [ ] Li o baseline P0/P1/P2 acima
- [ ] `docs/ARCHITECTURE-V2.md` existe? ___ Se sim, data da leitura: ___
- [ ] Decisões de naming (DB/env/image): ___

---

## Lembrete final

Você já tem um **núcleo CRUD/HTTP excelente**. Este roadmap existe para evoluir com calma: cada microtarefa é uma sessão de estudo, não uma corrida. Quando o Architecture V2 chegar, atualize os placeholders — o caminho InMemory → DB → outbox → cache → messaging → deploy → hardening continua válido.

Boa construção. Uma tarefa por vez.
