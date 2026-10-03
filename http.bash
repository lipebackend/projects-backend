#!/usr/bin/env bash
# Script de teste dos endpoints da Task Management REST API

BASE_URL="http://localhost:8080/tasks"

# 1. GET (Listar todas as tarefas)
echo "=== 1. GET /tasks (listar todas) ==="
curl -i -X GET "${BASE_URL}"
echo ""


# echo "=== 2. POST /tasks (criar nova tarefa) ==="
# curl -i -X POST "${BASE_URL}" -H "Content-Type: application/json" -d '{"title": "Nova Tarefa", "description": "Descrição da nova tarefa"}'
# curl -i -X POST "${BASE_URL}" -H "Content-Type: application/json" -d '{"title": "Nova Tarefa", "description": "Descrição da nova tarefa"}'
# curl -i -X POST "${BASE_URL}" -H "Content-Type: application/json" -d '{"title": "Nova Tarefa", "description": "Descrição da nova tarefa"}'
# curl -i -X POST "${BASE_URL}" -H "Content-Type: application/json" -d '{"title": "Nova Tarefa", "description": "Descrição da nova tarefa"}'
# curl -i -X POST "${BASE_URL}" -H "Content-Type: application/json" -d '{"title": "Nova Tarefa", "description": "Descrição da nova tarefa"}'
# echo "======================================================================"


# echo "=== 3. GET /tasks (listar todas novamente) ==="
# curl -i -X GET "${BASE_URL}"
# echo ""