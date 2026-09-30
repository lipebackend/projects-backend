#!/usr/bin/env bash
# Script de teste dos endpoints da Task Management REST API

BASE_URL="http://localhost:8080/tasks"

# 1. GET (Listar todas as tarefas)
echo "=== 1. GET /tasks (listar todas) ==="
curl -i -X GET "${BASE_URL}"
echo ""

# 2. POST (Criar tarefa - sem ID na URL, gerado no servidor)
echo "=== 2. POST /tasks (criar) ==="
curl -i -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -d '{"title":"Task 1"}'
echo ""

# 3. GET (Buscar por ID específico - substitua pelo UUID real gerado)
echo "=== 3. GET /tasks/{id} (buscar por id) ==="
curl -i -X GET "${BASE_URL}/sample-uuid"
echo ""

# 4. PUT (Atualizar tarefa existente)
echo "=== 4. PUT /tasks/{id} (atualizar) ==="
curl -i -X PUT "${BASE_URL}/sample-uuid" \
  -H "Content-Type: application/json" \
  -d '{"title":"Task 1 Atualizada","completed":true}'
echo ""

# 5. PATCH (Nota: TaskHandler não implementa PATCH -> retorna 405 Method Not Allowed)
echo "=== 5. PATCH /tasks/{id} (tentativa de patch - esperado: 405) ==="
curl -i -X PATCH "${BASE_URL}/sample-uuid" \
  -H "Content-Type: application/json" \
  -d '{"title":"Task 1 Parcial"}'
echo ""

# 6. DELETE (Remover tarefa)
echo "=== 6. DELETE /tasks/{id} (apagar) ==="
curl -i -X DELETE "${BASE_URL}/sample-uuid"
echo ""
