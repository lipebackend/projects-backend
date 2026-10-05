#!/usr/bin/env bash
# ==============================================================================
# Script de teste interativo, funcional e de estresse para a Task Management API
# ==============================================================================

set -euo pipefail

# Cores para saída no terminal
GREEN="\033[0;32m"
BLUE="\033[0;34m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
RED="\033[0;31m"
BOLD="\033[1m"
NC="\033[0m"

# Configurações padrão
DEFAULT_URL="http://localhost:8080/tasks"
BASE_URL="${DEFAULT_URL}"
MODE="all" # "functional", "stress", "all"
FORCE_NATIVE_BASH=false
TOTAL_REQUESTS="${REQUESTS:-10000}"
CONCURRENCY="${CONCURRENCY:-100}"

# Processamento de argumentos
for arg in "$@"; do
    case "${arg}" in
        http://*|https://*)
            BASE_URL="${arg}"
            ;;
        --stress|stress)
            MODE="stress"
            ;;
        --functional|functional)
            MODE="functional"
            ;;
        --all|all)
            MODE="all"
            ;;
        --native)
            FORCE_NATIVE_BASH=true
            ;;
        -n=*|--requests=*)
            TOTAL_REQUESTS="${arg#*=}"
            ;;
        -c=*|--concurrency=*)
            CONCURRENCY="${arg#*=}"
            ;;
        --help|-h)
            echo "Uso: $0 [BASE_URL] [OPÇÕES]"
            echo ""
            echo "Opções:"
            echo "  --stress            Executa apenas o teste de estresse de alta concorrência"
            echo "  --functional        Executa apenas a suíte funcional de 15 passos"
            echo "  --all               Executa a suíte funcional seguida do teste de estresse (padrão)"
            echo "  --native            Força uso do motor nativo Bash (xargs -P + curl) em vez de ferramentas Go"
            echo "  -n=N, --requests=N  Número total de requisições no estresse (padrão: 10000)"
            echo "  -c=C, --concurrency=C Concorrência simultânea (padrão: 100)"
            echo ""
            echo "Exemplos:"
            echo "  $0"
            echo "  $0 http://localhost:8080/tasks --stress"
            echo "  $0 --stress -n=10000 -c=200"
            echo "  $0 --stress --native"
            exit 0
            ;;
    esac
done

# ==============================================================================
# 1. SUÍTE DE TESTES FUNCIONAIS
# ==============================================================================
run_functional_tests() {
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

    echo -e "${GREEN}✓ Suíte funcional concluída com sucesso!${NC}\n"
}

# ==============================================================================
# 2. TESTE DE ESTRESSE: 10.000 REQUISIÇÕES EM PARALELO
# ==============================================================================
run_stress_test() {
    echo -e "${CYAN}======================================================================"
    echo -e "       TESTE DE ESTRESSE: ${TOTAL_REQUESTS} REQUISIÇÕES EM PARALELO"
    echo -e "       Alvo: ${BASE_URL}"
    echo -e "       Concorrência: ${CONCURRENCY} conexões simultâneas"
    echo -e "======================================================================${NC}\n"

    local HEY_BIN
    HEY_BIN=$(command -v hey || echo "/home/wroc/go/bin/hey")

    if [ "${FORCE_NATIVE_BASH}" = false ] && [ -x "${HEY_BIN}" ]; then
        echo -e "${BLUE}▶ Motor: 'hey' (Conexões HTTP Keep-Alive & Medição Percentil)${NC}"
        echo -e "${YELLOW}Disparando ${TOTAL_REQUESTS} requisições GET com concorrência de ${CONCURRENCY}...${NC}\n"
        "${HEY_BIN}" -n "${TOTAL_REQUESTS}" -c "${CONCURRENCY}" "${BASE_URL}"
    else
        echo -e "${BLUE}▶ Motor: Bash Nativo (xargs -P ${CONCURRENCY} + curl)${NC}"
        echo -e "${YELLOW}Iniciando disparo paralelo de ${TOTAL_REQUESTS} requisições no kernel Linux...${NC}"
        
        local start_ts
        start_ts=$(date +%s%N 2>/dev/null || date +%s)

        # Dispara as requisições em paralelo via xargs e contabiliza códigos de status HTTP
        local summary
        summary=$(seq 1 "${TOTAL_REQUESTS}" | xargs -P "${CONCURRENCY}" -I {} curl -s -o /dev/null -w "%{http_code}\n" "${BASE_URL}" 2>/dev/null | sort | uniq -c)

        local end_ts
        end_ts=$(date +%s%N 2>/dev/null || date +%s)

        # Cálculo do tempo decorrido
        local elapsed_sec
        if [ ${#start_ts} -gt 10 ]; then
            # Nanosegundos disponíveis
            local diff_ns=$((end_ts - start_ts))
            elapsed_sec=$(awk "BEGIN {printf \"%.3f\", ${diff_ns} / 1000000000}")
        else
            elapsed_sec=$((end_ts - start_ts))
            [ "${elapsed_sec}" -eq 0 ] && elapsed_sec=1
        fi

        local rps
        rps=$(awk "BEGIN {printf \"%.1f\", ${TOTAL_REQUESTS} / ${elapsed_sec}}")

        echo -e "\n${BOLD}${GREEN}======================================================================${NC}"
        echo -e "${BOLD}${GREEN}                     RELATÓRIO DO TESTE DE ESTRESSE                   ${NC}"
        echo -e "${BOLD}${GREEN}======================================================================${NC}"
        echo -e "${CYAN}Total de Requisições:${NC}  ${TOTAL_REQUESTS}"
        echo -e "${CYAN}Concorrência:${NC}          ${CONCURRENCY} processos paralelos"
        echo -e "${CYAN}Tempo Total:${NC}           ${elapsed_sec} segundos"
        echo -e "${CYAN}Vazão Média:${NC}           ${rps} requisições/segundo"
        echo -e "\n${YELLOW}Distribuição de Códigos HTTP Retornados:${NC}"
        echo "${summary}" | while read -r count code; do
            if [[ "${code}" =~ ^2 ]]; then
                echo -e "  ${GREEN}[${code}] ${count} respostas com sucesso (OK)${NC}"
            elif [[ "${code}" =~ ^4 ]]; then
                echo -e "  ${YELLOW}[${code}] ${count} respostas de cliente (4xx)${NC}"
            else
                echo -e "  ${RED}[${code}] ${count} erros de servidor (5xx)${NC}"
            fi
        done
        echo -e "${BOLD}${GREEN}======================================================================${NC}\n"
    fi
}

# ==============================================================================
# EXECUÇÃO DO FLUXO SELECIONADO
# ==============================================================================
case "${MODE}" in
    functional)
        run_functional_tests
        ;;
    stress)
        run_stress_test
        ;;
    all)
        run_functional_tests
        run_stress_test
        ;;
esac

echo -e "${CYAN}======================================================================"
echo -e "       TESTES CONCLUÍDOS COM SUCESSO!"
echo -e "======================================================================${NC}"