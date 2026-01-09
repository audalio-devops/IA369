#!/bin/bash

# Script para remover/renomear classes SecurityConfig
# Uso: chmod +x remove-security-configs.sh && ./remove-security-configs.sh

PROJECT_ROOT="$HOME/IA369"
BACKUP_SUFFIX=".disabled"

echo "🔍 Procurando classes SecurityConfig..."

# Encontrar todas as classes SecurityConfig
SECURITY_FILES=$(find "$PROJECT_ROOT" -name "SecurityConfig.java" -type f)

if [ -z "$SECURITY_FILES" ]; then
    echo "✅ Nenhuma classe SecurityConfig encontrada."
    exit 0
fi

echo "📋 Classes encontradas:"
echo "$SECURITY_FILES"
echo ""

# Perguntar confirmação
read -p "⚠️  Deseja renomear estas classes? (s/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Ss]$ ]]; then
    while IFS= read -r file; do
        if [ -f "$file" ]; then
            mv "$file" "${file}${BACKUP_SUFFIX}"
            echo "✅ Renomeado: $file → ${file}${BACKUP_SUFFIX}"
        fi
    done <<< "$SECURITY_FILES"

    echo ""
    echo "✅ Concluído! Classes SecurityConfig foram desabilitadas."
    echo "💡 Para reverter, remova o sufixo '$BACKUP_SUFFIX' dos arquivos."
else
    echo "❌ Operação cancelada."
    exit 1
fi
