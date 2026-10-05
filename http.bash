#!/usr/bin/env bash
# ==============================================================================
# Script de teste interativo e demonstrativo dos endpoints da Task Management API
# ==============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080/tasks}"

# Cores para saída no terminal
GREEN="\033[0;32m"
BLUE="\033[0;34m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
NC="\033[0m"

echo -e "${CYAN}======================================================================"
echo -e "       TESTANDO ENDPOINTS DA TASK MANAGEMENT REST API"
echo -e "       Base URL: ${BASE_URL}"
echo -e "======================================================================${NC}\n"

# 1. GET (Listar todas as tarefas - lista vazia inicial)
echo -e "${YELLOW}=== 1. GET /tasks (Listar todas - esperado []) ===${NC}"
curl -s -i -X GET "${BASE_URL}"
echo -e "\n"

# 2. POST (Criar tarefa 1)
echo -e "${YELLOW}=== 2. POST /tasks (Criar primeira tarefa) ===${NC}"
CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}" \
    -H "Content-Type: application/json" \
    -d '{"title": "Estudar Concorrência e Virtual Threads"}')
echo -e "${GREEN}Resposta:${NC} ${CREATE_RESPONSE}\n"

# Extrair ID da tarefa criada (tenta jq ou fallback regex)
if command -v jq >/dev/null 2>&1; then
    TASK_ID=$(echo "${CREATE_RESPONSE}" | jq -r '.id')
else
    TASK_ID=$(echo "${CREATE_RESPONSE}" | sed -n 's/.*"id"[ ]*:[ ]*"\([^"]*\)".*/\1/p')
fi
echo -e "${BLUE}ID capturado para a Tarefa 1:${NC} ${TASK_ID}\n"

# 3. POST (Criar tarefa 2)
echo -e "${YELLOW}=== 3. POST /tasks (Criar segunda tarefa) ===${NC}"
curl -s -i -X POST "${BASE_URL}" \
    -H "Content-Type: application/json" \
    -d '{"title": "Implementar Clean Architecture no Backend"}'
echo -e "\n"

# 4. GET (Listar todas as tarefas - agora com 2 tarefas)
echo -e "${YELLOW}=== 4. GET /tasks (Listar todas novamente) ===${NC}"
curl -s -i -X GET "${BASE_URL}"
echo -e "\n"

# 5. GET /tasks/{id} (Consultar por ID)
echo -e "${YELLOW}=== 5. GET /tasks/${TASK_ID} (Buscar Tarefa 1 por ID) ===${NC}"
curl -s -i -X GET "${BASE_URL}/${TASK_ID}"
echo -e "\n"

# 6. PUT /tasks/{id} (Substituição completa - requer title e completed)
echo -e "${YELLOW}=== 6. PUT /tasks/${TASK_ID} (Substituição completa: título e completed=true) ===${NC}"
curl -s -i -X PUT "${BASE_URL}/${TASK_ID}" \
    -H "Content-Type: application/json" \
    -d '{"title": "Estudar Concorrência e Virtual Threads (Concluído)", "completed": true}'
echo -e "\n"

# 7. PATCH /tasks/{id} (Atualização parcial - apenas título)
echo -e "${YELLOW}=== 7. PATCH /tasks/${TASK_ID} (Atualização parcial: apenas título) ===${NC}"
curl -s -i -X PATCH "${BASE_URL}/${TASK_ID}" \
    -H "Content-Type: application/json" \
    -d '{"title": "Dominar Concorrência em Java 21"}'
echo -e "\n"

# 8. GET /tasks?completed=true (Filtro de tarefas concluídas)
echo -e "${YELLOW}=== 8. GET /tasks?completed=true (Filtro por concluídas) ===${NC}"
curl -s -i -X GET "${BASE_URL}?completed=true"
echo -e "\n"

# 9. GET /tasks?completed=false (Filtro de tarefas pendentes)
echo -e "${YELLOW}=== 9. GET /tasks?completed=false (Filtro por pendentes) ===${NC}"
curl -s -i -X GET "${BASE_URL}?completed=false"
echo -e "\n"

# 10. Erro: POST sem campo obrigatório 'title' (esperado 400 Bad Request)
echo -e "${YELLOW}=== 10. POST /tasks (Erro: sem título - esperado 400) ===${NC}"
curl -s -i -X POST "${BASE_URL}" \
    -H "Content-Type: application/json" \
    -d '{}'
echo -e "\n"

# 11. Erro: Content-Type inválido (esperado 415 Unsupported Media Type)
echo -e "${YELLOW}=== 11. POST /tasks (Erro: Content-Type texto - esperado 415) ===${NC}"
curl -s -i -X POST "${BASE_URL}" \
    -H "Content-Type: text/plain" \
    -d '{"title": "Teste"}'
echo -e "\n"

# 12. Erro: PUT sem campo obrigatório 'completed' (esperado 400 Bad Request)
echo -e "${YELLOW}=== 12. PUT /tasks/${TASK_ID} (Erro PUT sem completed - esperado 400) ===${NC}"
curl -s -i -X PUT "${BASE_URL}/${TASK_ID}" \
    -H "Content-Type: application/json" \
    -d '{"title": "Tentativa de PUT parcial"}'
echo -e "\n"

# 13. Erro: GET com ID inexistente (esperado 404 Not Found)
echo -e "${YELLOW}=== 13. GET /tasks/id-inexistente (esperado 404) ===${NC}"
curl -s -i -X GET "${BASE_URL}/id-inexistente"
echo -e "\n"

# 14. DELETE /tasks/{id} (Remover tarefa - esperado 204 No Content sem corpo)
echo -e "${YELLOW}=== 14. DELETE /tasks/${TASK_ID} (esperado 204 No Content) ===${NC}"
curl -s -i -X DELETE "${BASE_URL}/${TASK_ID}"
echo -e "\n"

# 15. GET /tasks/{id} (Verificar remoção - esperado 404 Not Found)
echo -e "${YELLOW}=== 15. GET /tasks/${TASK_ID} pós-exclusão (esperado 404) ===${NC}"
curl -s -i -X GET "${BASE_URL}/${TASK_ID}"
echo -e "\n"

echo -e "${CYAN}======================================================================"
echo -e "       TESTES CONCLUÍDOS COM SUCESSO!"
echo -e "======================================================================${NC}"