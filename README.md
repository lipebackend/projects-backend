# API REST para Gerenciamento de Tarefas (Task Management)

> **Domínio:** Backend &nbsp;|&nbsp; **Nível:** Iniciante / Intermediário &nbsp;|&nbsp; **Linguagem:** Java 21 LTS &nbsp;|&nbsp; **Padrão:** Clean Architecture & Production-Ready

---

## 📖 Visão Geral

Construção de uma API REST de alta performance e robustez técnica para gerenciar tarefas inteiramente em memória, utilizando **Java 21**, **Virtual Threads (Project Loom)**, **HTTP Server nativo do JDK** e **Jackson Databind**.

A aplicação segue princípios sólidos de engenharia de software (Clean Architecture, Fail-Fast, Imutabilidade, Injeção de Dependências via Construtor e Semântica HTTP estrita de acordo com as RFCs 9110 e 7396).

---

## 🛠️ Pré-requisitos & Tecnologias

- **Java 21 LTS** (Eclipse Temurin, OpenJDK ou via SDKMAN)
- **Apache Maven 3.9+**
- **Docker** ou **Podman** (opcional, para execução conteinerizada)
- Ferramenta de teste HTTP: `curl`, HTTPie ou o script automatizado `./http.bash`

---

## 🎯 Objetivos de Aprendizado & Engenharia

- Mapeamento correto de operações CRUD para métodos HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- Semântica precisa de códigos de status (`200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `404 Not Found`, `413 Payload Too Large`, `415 Unsupported Media Type`, `405 Method Not Allowed`).
- Diferenciação estrita entre **`PUT`** (substituição integral) e **`PATCH`** (atualização parcial).
- Segurança de memória e prevenção de DoS impondo limite no streaming do corpo da requisição (Buffer Bounded em 64 KB).
- Manipulação e serialização JSON estruturada com tratamento de erros robusto.
- Proteção contra condições de corrida (*race conditions*) em memória através de entidades imutáveis e operações atômicas sob lock de bucket (`ConcurrentHashMap.computeIfPresent`).
- Arquitetura limpa com isolamento de responsabilidades (HTTP Handler, Serviço de Domínio e Repositório).
- Testes automatizados cobrindo testes unitários, concorrência e integração HTTP ponta a ponta.

---

## ⚙️ Requisitos Funcionais

- [x] O sistema deve expor um endpoint para listar todas as tarefas com suporte a filtro `?completed=true|false`.
- [x] O sistema deve expor um endpoint para buscar uma tarefa pelo ID e retornar `404` quando ela não existir.
- [x] O sistema deve criar uma tarefa a partir de um corpo JSON, atribuir um ID único (UUID) e retornar `201 Created` com o recurso criado.
- [x] O sistema deve rejeitar requisições de criação sem campo obrigatório com `400 Bad Request` e mensagem explicativa.
- [x] O sistema deve atualizar uma tarefa existente via `PUT` (substituição total) ou `PATCH` (atualização parcial) e retornar `404` se o ID for desconhecido.
- [x] O sistema deve remover uma tarefa pelo ID e retornar `204 No Content` sem corpo (ou `404` se ausente).
- [x] O sistema deve retornar JSON válido com o cabeçalho `Content-Type: application/json; charset=UTF-8` em toda resposta que possuir corpo (respostas `204 No Content` não retornam cabeçalho de conteúdo, conforme RFC 9110).

---

## 📐 Contrato da API e Estrutura de Dados

### Modelo `Task` (Entidade Imutável)

| Campo | Tipo | Obrigatório | Descrição |
| :--- | :--- | :---: | :--- |
| `id` | `String` (UUID) | Sim | Atribuído exclusivamente pelo servidor |
| `title` | `String` | Sim | Título descritivo da tarefa (não-nulo, não-vazio) |
| `completed` | `boolean` | Sim | Status da tarefa (padrão: `false` na criação) |
| `createdAt` | `String` (ISO-8601) | Sim | Timestamp UTC gerado na criação |

---

### Endpoints da API

| Método | Endpoint | Status HTTP | Headers | Payload / Query / Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/tasks` | `200 OK` | `Content-Type: application/json` | Suporta filtro opcional: `?completed=true` ou `?completed=false` |
| `GET` | `/tasks/{id}` | `200 OK`<br>`404 Not Found` | `Content-Type: application/json` | Busca uma tarefa específica pelo ID |
| `POST` | `/tasks` | `201 Created`<br>`400 Bad Request`<br>`413 Payload Too Large`<br>`415 Unsupported Media Type` | `Content-Type: application/json` | Cria nova tarefa.<br>Body: `{"title": "Nome da tarefa"}` |
| `PUT` | `/tasks/{id}` | `200 OK`<br>`400 Bad Request`<br>`404 Not Found`<br>`413 Payload Too Large`<br>`415 Unsupported Media Type` | `Content-Type: application/json` | **Substituição completa** do recurso.<br>Exige obrigatoriamente `title` e `completed`.<br>Body: `{"title": "Novo título", "completed": true}` |
| `PATCH` | `/tasks/{id}` | `200 OK`<br>`400 Bad Request`<br>`404 Not Found`<br>`413 Payload Too Large`<br>`415 Unsupported Media Type` | `Content-Type: application/json` | **Atualização parcial**.<br>Aceita `title`, `completed`, ou ambos.<br>Body: `{"title": "..."}` ou `{"completed": true}` |
| `DELETE` | `/tasks/{id}` | `204 No Content`<br>`404 Not Found` | *(Sem headers de corpo)* | Remove a tarefa. Não retorna corpo na resposta de sucesso. |

---

### Formato Padrão de Erro

Todas as respostas de erro retornam `Content-Type: application/json; charset=UTF-8` no seguinte padrão:

```json
{
  "error": "Field 'title' is required and cannot be blank"
}
```

---

## 🚀 Desafios Extras

- [x] Adicionar filtro `?completed=true|false` no endpoint de listagem.
- [x] Suportar `PATCH` para atualizações seletivas além do `PUT` completo.
- [ ] Adicionar paginação com parâmetros de query `limit` e `offset`.
- [x] Escrever testes automatizados que exercitem cada endpoint, concorrência e código de status.

---

## ✅ Definição de Pronto (DoD)

- [x] Todos os endpoints CRUD funcionam e retornam os códigos de status documentados.
- [x] Buscar, atualizar ou remover um ID inexistente retorna `404 Not Found`.
- [x] Entradas inválidas (JSON malformado, campos ausentes, tipos errados) retornam `400 Bad Request` com diagnóstico claro, nunca `500`.
- [x] IDs são UUIDs únicos e estáveis gerados pelo servidor.
- [x] Respostas com conteúdo definem `Content-Type: application/json; charset=UTF-8`.
- [x] Respostas `204 No Content` não retornam corpo nem cabeçalho `Content-Type`.

---

## 🔎 Revisão Técnica: Tarefas de Melhoria (Code Review Concluído)

### P1 — Comportamento e robustez da API

- [x] **Substituir a leitura de JSON por expressões regulares por um parser JSON.**
  - *Resolução:* Integrado o Jackson Databind 2.18 com `JavaTimeModule`. Deserialização tipada via DTOs imutáveis (`CreateTaskRequest`, `UpdateTaskRequest`, `PatchTaskRequest`). Erros de parsing JSON agora retornam `400 Bad Request`.
- [x] **Usar serialização JSON para todas as respostas.**
  - *Resolução:* Jackson `ObjectMapper` centralizado no `JsonMapper` para serializar todos os payloads de sucesso e erros (`TaskResponse`, `ErrorResponse`). Eliminadas todas as concatenações e rotinas de escape manuais.
- [x] **Validar explicitamente os tipos e valores permitidos nos campos.**
  - *Resolução:* Validação Fail-Fast em todas as camadas. Campos ausentes, em branco ou de formato inesperado são rejeitados de imediato com `400 Bad Request`.
- [x] **Definir contratos diferentes para `PUT` e `PATCH`.**
  - *Resolução:* `PUT` implementa substituição completa (exigindo `title` e `completed`); `PATCH` implementa atualização seletiva (aceitando `title`, `completed` ou ambos).
- [x] **Impor um limite ao tamanho do corpo recebido.**
  - *Resolução:* Leitura limitada a 64 KB (`MAX_BODY_SIZE_BYTES = 64 * 1024`). Requisições acima desse limite são interrompidas e respondidas com `413 Payload Too Large`.
- [x] **Validar o tipo de conteúdo das requisições.**
  - *Resolução:* Métodos com corpo (`POST`, `PUT`, `PATCH`) exigem `Content-Type: application/json`. Requisições sem esse cabeçalho ou com outro tipo de mídia retornam `415 Unsupported Media Type`.
- [x] **Proteger leituras e atualizações concorrentes das tarefas.**
  - *Resolução:* Entidade `Task` imutável com métodos *wither*. O `InMemoryTaskRepository` usa `ConcurrentHashMap` com `computeIfPresent` para atualizações atômicas, garantindo ausência de *lost updates* e *zero lock contention* nas leituras.
- [x] **Fazer o servidor escutar na interface correta dentro do contêiner.**
  - *Resolução:* `Main` escuta na interface `0.0.0.0` por padrão, configurável via variável de ambiente `HOST`.
- [x] **Rejeitar configurações de porta inválidas com diagnóstico claro.**
  - *Resolução:* O método `resolvePort` valida argumentos CLI e a variável `PORT` com Fail-Fast (retornando erro explícito e saindo com status 1 caso inválido), mantendo 8080 apenas como padrão.
- [x] **Conectar o encerramento da aplicação ao ciclo de vida do servidor.**
  - *Resolução:* Registrado Shutdown Hook na JVM que invoca `server.close()`, finalizando ordenadamente as requisições em andamento e o executor de Virtual Threads.

### P2 — Arquitetura e capacidade de evolução

- [x] **Separar o tratamento HTTP das regras de negócio e do armazenamento.**
  - *Resolução:* Clean Architecture implementada: `TaskHandler` (Web/HTTP) ➔ `TaskService` / `TaskServiceImpl` (Regras de negócio) ➔ `TaskRepository` / `InMemoryTaskRepository` (Persistência em memória). Todas as dependências injetadas via construtor.
- [x] **Alinhar os contratos de serviço e repositório ao modelo usado pela API.**
  - *Resolução:* `TaskRepository` e `TaskService` padronizados com IDs `String` e métodos coesos (`findAll`, `findById`, `save`, `update`, `deleteById`).
- [x] **Adicionar testes automatizados para domínio e endpoints.**
  - *Resolução:* 33 testes automatizados com JUnit 5 e AssertJ cobrindo regras de negócio, testes de invariantes, concorrência multithread no repositório e testes de integração HTTP reais via `HttpClient`.
- [x] **Completar o roteiro manual de chamadas HTTP.**
  - *Resolução:* `http.bash` implementado com 15 cenários de teste automatizados e coloridos, cobrindo o ciclo de vida completo de uma tarefa e todos os fluxos de erro.
- [x] **Corrigir o registro da mensagem de inicialização do servidor.**
  - *Resolução:* `Server.java` registra os logs formatando adequadamente `{host}:{port}`.
- [x] **Alinhar as coordenadas Maven ao nome real do projeto.**
  - *Resolução:* Coordenadas configuradas como `com.taskmanagement:task-management-api:1.0.0`, com `maven-shade-plugin` gerando o jar executável `task-management-api.jar`, compatível com o Dockerfile.

### P3 — Contrato e clareza da documentação

- [x] **Documentar a implementação Java e seu contrato real.**
  - *Resolução:* Documentação atualizada com todos os comandos, especificações de endpoints, tabelas de payloads e requisitos técnicos.
- [x] **Especificar a resposta de exclusão `204 No Content`.**
  - *Resolução:* Documentado e implementado: `DELETE /tasks/{id}` responde `204 No Content` sem corpo e sem cabeçalho `Content-Type`.

---

## 🏃 Como Executar a Aplicação

### 1. Execução Local via Maven & Java

```bash
# Compilar e rodar a suíte de testes (33 testes automatizados)
mvn clean test

# Gerar o pacote executável (shaded fat jar)
mvn clean package -DskipTests

# Executar a API na porta padrão 8080 (ou informe outra porta como argumento)
java -jar target/task-management-api.jar
```

A API estará disponível em `http://localhost:8080/tasks`.

---

### 2. Executando os Testes Manuais com o `http.bash`

Com o servidor rodando em outro terminal:

```bash
./http.bash http://localhost:8080/tasks
```

O script executará 15 etapas verificando listagem, criação, atualização com `PUT`, atualização com `PATCH`, filtros e testes de erro (400, 404, 415).

---

### 3. Executando com Docker ou Podman

```bash
# Construir a imagem conteinerizada
podman build -t task-management-api .
# ou: docker build -t task-management-api .

# Subir o contêiner mapeando a porta 8080
podman run --rm -it -p 8080:8080 task-management-api
# ou: docker compose up --build
```

---

## 🧰 Executando no VS Code com Dev Containers

1. Abra o diretório do projeto no VS Code com a extensão **Dev Containers**.
2. Selecione **Dev Containers: Reopen in Container** (`Ctrl+Shift+P`).
3. O ambiente configurará Java 21, Maven e abrirá a porta `8080`.
4. Pressione `F5` para iniciar o debug ou `Shift+F5` para parar.
