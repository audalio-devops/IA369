#!/bin/bash

# =============================================================================
# Script de Setup Automatizado - IA369
# =============================================================================
# Este script facilita a configuração inicial do projeto
# =============================================================================

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funções auxiliares
print_header() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}======================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Verificar se está no diretório correto
check_directory() {
    if [ ! -f "docker-compose.yml" ]; then
        print_error "Execute este script no diretório raiz do projeto IA369"
        exit 1
    fi
}

# Criar estrutura de diretórios
create_directories() {
    print_header "Criando estrutura de diretórios"

    mkdir -p scripts
    mkdir -p k8s
    mkdir -p .github/workflows
    mkdir -p logs

    print_success "Diretórios criados"
}

# Criar arquivos de ambiente
setup_env_files() {
    print_header "Configurando arquivos de ambiente"

    if [ ! -f ".env.dev" ]; then
        print_info "Criando .env.dev..."
        cat > .env.dev << 'EOF'
# Development Environment
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres_dev_2024
POSTGRES_DB=nfe_db
DB_NFE_URL=jdbc:postgresql://postgres:5432/nfe_db
DB_CLIENT_URL=jdbc:postgresql://postgres:5432/client_db
DB_BORDERO_URL=jdbc:postgresql://postgres:5432/bordero_db
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KAFKA_CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk
EUREKA_DEFAULT_ZONE=http://eureka-server:8761/eureka/
SPRING_PROFILES_ACTIVE=docker
JWT_SECRET=dev-secret-key-not-for-production
JWT_EXPIRATION=86400000
JAVA_OPTS_EUREKA=-Xmx512m -Xms256m
JAVA_OPTS_GATEWAY=-Xmx512m -Xms256m
JAVA_OPTS_NFE=-Xmx768m -Xms384m
JAVA_OPTS_CLIENT=-Xmx512m -Xms256m
JAVA_OPTS_BORDERO=-Xmx768m -Xms384m
LOG_LEVEL=DEBUG
EOF
        print_success ".env.dev criado"
    else
        print_warning ".env.dev já existe (pulando)"
    fi

    # Criar .env para desenvolvimento local
    if [ ! -f ".env" ]; then
        print_info "Criando .env a partir de .env.dev..."
        cp .env.dev .env
        print_success ".env criado (copiado de .env.dev)"

        # Verificar se copiou corretamente
        if [ -s ".env" ]; then
            VAR_COUNT=$(grep -c "=" .env)
            print_success "✅ $VAR_COUNT variáveis carregadas no .env"
        else
            print_error ".env foi criado mas está vazio!"
            exit 1
        fi
    else
        print_warning ".env já existe"
        print_info "Verificando variáveis..."
        VAR_COUNT=$(grep -c "=" .env)
        print_info "📊 $VAR_COUNT variáveis encontradas no .env"
    fi
}

# Verificar .gitignore
check_gitignore() {
    print_header "Verificando .gitignore"

    if ! grep -q ".env" .gitignore 2>/dev/null; then
        print_warning "Adicionando regras ao .gitignore..."
        cat >> .gitignore << 'EOF'

# Environment Variables
.env
.env.local
.env.dev
.env.prod
.env.*.local
*.secret
EOF
        print_success ".gitignore atualizado"
    else
        print_success ".gitignore já está configurado"
    fi
}

# Gerar JWT Secret
generate_jwt_secret() {
    print_header "Gerando JWT Secret"

    if command -v openssl &> /dev/null; then
        JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
        print_success "JWT Secret gerado"
        print_info "Adicione ao seu .env.prod:"
        echo -e "${GREEN}JWT_SECRET=$JWT_SECRET${NC}"
        echo ""
    else
        print_warning "OpenSSL não encontrado. Gere manualmente: openssl rand -base64 64"
    fi
}

# Verificar Docker
check_docker() {
    print_header "Verificando Docker"

    if ! command -v docker &> /dev/null; then
        print_error "Docker não está instalado"
        print_info "Instale: https://docs.docker.com/get-docker/"
        exit 1
    fi

    print_success "Docker está instalado: $(docker --version)"

    if ! command -v docker compose &> /dev/null; then
        print_error "Docker Compose não está instalado"
        exit 1
    fi

    print_success "Docker Compose está instalado"
}

# Test Docker Compose configuration
test_docker_compose() {
    print_header "Testando Docker Compose"

    if docker compose config > /dev/null 2>&1; then
        print_success "docker-compose.yml é válido"
    else
        print_error "docker-compose.yml tem erros"
        print_info "Execute: docker compose config"
        exit 1
    fi
}

# Criar script de backup
create_backup_script() {
    print_header "Criando script de backup"

    cat > scripts/backup-database.sh << 'EOF'
#!/bin/bash
# Backup do banco de dados PostgreSQL

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups"
mkdir -p $BACKUP_DIR

docker exec postgres pg_dumpall -U postgres | gzip > "$BACKUP_DIR/backup_$DATE.sql.gz"
echo "Backup criado: $BACKUP_DIR/backup_$DATE.sql.gz"

# Manter apenas últimos 7 dias
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
EOF

    chmod +x scripts/backup-database.sh
    print_success "Script de backup criado: scripts/backup-database.sh"
}

# Criar Makefile
create_makefile() {
    print_header "Criando Makefile"

    cat > Makefile << 'EOF'
.PHONY: help up down restart logs build clean test backup

help:
	@echo "IA369 - Comandos disponíveis:"
	@echo "  make up        - Subir todos os containers"
	@echo "  make down      - Parar todos os containers"
	@echo "  make restart   - Reiniciar containers"
	@echo "  make logs      - Ver logs"
	@echo "  make build     - Rebuildar imagens"
	@echo "  make clean     - Limpar volumes"
	@echo "  make test      - Testar endpoints"
	@echo "  make backup    - Fazer backup do banco"

up:
	docker compose up -d

down:
	docker compose down

restart:
	docker compose restart

logs:
	docker compose logs -f

build:
	docker compose build --no-cache

clean:
	docker compose down -v
	docker system prune -f

test:
	@echo "Testando endpoints..."
	@curl -f http://localhost:8761 || echo "Eureka: FAIL"
	@curl -f http://localhost:8080/actuator/health || echo "Gateway: FAIL"

backup:
	./scripts/backup-database.sh
EOF

    print_success "Makefile criado"
}

# Menu interativo
show_menu() {
    print_header "Setup IA369"
    echo ""
    echo "Escolha uma opção:"
    echo "1) Setup completo (recomendado)"
    echo "2) Apenas criar estrutura de diretórios"
    echo "3) Apenas configurar variáveis de ambiente"
    echo "4) Gerar JWT Secret"
    echo "5) Testar Docker Compose"
    echo "6) Sair"
    echo ""
    read -p "Opção: " option

    case $option in
        1)
            full_setup
            ;;
        2)
            create_directories
            ;;
        3)
            setup_env_files
            ;;
        4)
            generate_jwt_secret
            ;;
        5)
            test_docker_compose
            ;;
        6)
            print_info "Saindo..."
            exit 0
            ;;
        *)
            print_error "Opção inválida"
            exit 1
            ;;
    esac
}

# Setup completo
full_setup() {
    check_directory
    create_directories
    setup_env_files
    check_gitignore
    check_docker
    test_docker_compose
    generate_jwt_secret
    create_backup_script
    create_makefile

    print_header "Setup Concluído! 🎉"
    echo ""
    print_success "Próximos passos:"
    echo "1. Revise o arquivo .env com suas configurações"
    echo "2. Execute: make up"
    echo "3. Acesse: http://localhost:8761 (Eureka)"
    echo "4. Acesse: http://localhost:8080 (Gateway)"
    echo ""
    print_info "Para ver comandos disponíveis: make help"
}

# Executar
if [ "$1" == "--auto" ]; then
    full_setup
else
    show_menu
fi