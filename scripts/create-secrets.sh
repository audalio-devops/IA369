#!/bin/bash

# =============================================================================
# Script para criar Docker Secrets
# =============================================================================
# ATENÇÃO: Execute apenas em ambiente de produção com Docker Swarm
# =============================================================================

set -e

echo "🔐 Criando Docker Secrets para IA369..."
echo ""

# Verificar se está rodando em modo Swarm
if ! docker info 2>/dev/null | grep -q "Swarm: active"; then
    echo "❌ Docker Swarm não está ativo!"
    echo "💡 Execute: docker swarm init"
    exit 1
fi

# Função para criar secret
create_secret() {
    local secret_name=$1
    local secret_prompt=$2

    if docker secret inspect "$secret_name" >/dev/null 2>&1; then
        echo "⚠️  Secret '$secret_name' já existe (pulando)"
    else
        echo -n "$secret_prompt: "
        read -s secret_value
        echo ""
        echo -n "$secret_value" | docker secret create "$secret_name" -
        echo "✅ Secret '$secret_name' criado"
    fi
}

# Criar secrets
create_secret "postgres_user" "PostgreSQL User"
create_secret "postgres_password" "PostgreSQL Password"
create_secret "db_password" "Database Password"
create_secret "jwt_secret" "JWT Secret (min 256 bits)"
create_secret "kafka_password" "Kafka Password (se aplicável)"

echo ""
echo "✅ Todos os secrets foram criados!"
echo ""
echo "📋 Listar secrets:"
docker secret ls
echo ""
echo "🚀 Para deploy:"
echo "   docker stack deploy -c docker-compose.secrets.yml ia369"