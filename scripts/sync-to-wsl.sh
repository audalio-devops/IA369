#!/bin/bash

WINDOWS_PATH="//mnt/c/Projetos/IA369/nfe-bordero-system"
WSL_PATH="$HOME/IA369"

echo "🔄 Sincronizando arquivos..."

# Copiar apenas arquivos modificados
rsync -av --delete \
    --exclude='.git/' \
    --exclude='.idea/' \
    --exclude='target/' \
    --exclude='build/' \
    --exclude='*.class' \
    --exclude='*.jar' \
    "$WINDOWS_PATH/" "$WSL_PATH/"

echo "✅ Sincronização concluída!"
