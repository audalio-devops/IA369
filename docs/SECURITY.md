# 🔐 Guia de Segurança - IA369

## 📋 Índice

1. [Gestão de Secrets](#gestão-de-secrets)
2. [Ambientes](#ambientes)
3. [Docker Secrets](#docker-secrets)
4. [Kubernetes Secrets](#kubernetes-secrets)
5. [GitHub Actions](#github-actions)
6. [Boas Práticas](#boas-práticas)

---

## 🔑 Gestão de Secrets

### **Setup Inicial**

```bash
# 1. Copiar template de ambiente
cp .env.example .env.dev

# 2. Editar e preencher com valores reais
nano .env.dev

# 3. NUNCA commite arquivos .env
git status  # Verificar se .env está no .gitignore
```

### **Estrutura de Arquivos**

```
IA369/
├── .env.example          # Template (commitável)
├── .env.dev              # Development (NÃO commitar)
├── .env.prod             # Production (NÃO commitar)
├── .gitignore            # Garante que .env não seja commitado
└── docker-compose.yml    # Usa variáveis do .env
```

---

## 🌍 Ambientes

### **Development (Local)**

```bash
# Usar .env.dev
cp .env.dev .env
docker compose up -d
```

**Características:**
- ✅ Senhas simples (OK para desenvolvimento)
- ✅ Debug logging habilitado
- ✅ Volumes persistentes
- ❌ NÃO usar em produção

### **Staging**

```bash
# Usar .env.staging
cp .env.staging .env
docker compose -f docker-compose.yml up -d
```

**Características:**
- ⚠️ Senhas intermediárias
- ⚠️ Logs moderados
- ✅ Ambiente similar à produção

### **Production**

```bash
# Usar Docker Secrets (Swarm) ou Kubernetes Secrets
docker stack deploy -c docker-compose.secrets.yml ia369

# OU

kubectl apply -f k8s/
```

**Características:**
- 🔒 Secrets gerenciados por Docker/Kubernetes
- 🔒 Senhas fortes e rotacionadas
- 🔒 Logs mínimos
- 🔒 Monitoramento ativo

---

## 🐳 Docker Secrets (Swarm Mode)

### **1. Inicializar Swarm**

```bash
docker swarm init
```

### **2. Criar Secrets**

```bash
# Método 1: Interativo
./scripts/create-secrets.sh

# Método 2: Manual
echo "seu_usuario_postgres" | docker secret create postgres_user -
echo "senha_super_secreta" | docker secret create postgres_password -
echo "jwt_secret_min_256_bits" | docker secret create jwt_secret -
```

### **3. Listar Secrets**

```bash
docker secret ls
```

### **4. Deploy com Secrets**

```bash
docker stack deploy -c docker-compose.secrets.yml ia369
```

### **5. Ver serviços**

```bash
docker stack services ia369
docker service logs ia369_nfe-processor-service
```

---

## ☸️ Kubernetes Secrets

### **1. Criar Namespace**

```bash
kubectl apply -f k8s/00-namespace.yaml
```

### **2. Criar Secrets (via CLI)**

```bash
# Database
kubectl create secret generic ia369-database \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD='SuaSenhaAqui' \
  --from-literal=POSTGRES_DB=nfe_db \
  --namespace=ia369

# JWT
kubectl create secret generic ia369-jwt \
  --from-literal=JWT_SECRET='SeuJWTSecretAqui' \
  --from-literal=JWT_EXPIRATION=3600000 \
  --namespace=ia369
```

### **3. Criar Secrets (via arquivo YAML criptografado)**

```bash
# Usar Sealed Secrets ou SOPS
kubeseal --format=yaml < k8s/01-secrets.yaml > k8s/01-sealed-secrets.yaml
kubectl apply -f k8s/01-sealed-secrets.yaml
```

### **4. Verificar Secrets**

```bash
kubectl get secrets -n ia369
kubectl describe secret ia369-database -n ia369
```

---

## 🚀 GitHub Actions

### **1. Configurar Secrets no GitHub**

Acesse: `Settings → Secrets and variables → Actions`

**Secrets Necessários:**

```
DOCKERHUB_USERNAME        # Seu usuário Docker Hub
DOCKERHUB_TOKEN           # Token de acesso Docker Hub
PROD_SSH_KEY              # Chave SSH privada (produção)
PROD_HOST                 # IP/hostname do servidor
PROD_USER                 # Usuário SSH
PROD_ENV_FILE             # Conteúdo do .env.prod
STAGING_SSH_KEY           # Chave SSH (staging)
STAGING_HOST              # IP/hostname staging
STAGING_USER              # Usuário SSH staging
```

### **2. Como adicionar secrets**

```bash
# Via GitHub CLI
gh secret set DOCKERHUB_TOKEN
# Cole o token quando solicitado

# Via interface web
# Settings → Secrets → New repository secret
```

### **3. Workflows disponíveis**

- **CI (ci.yml)**: Build, Test, Security Scan
- **CD (cd.yml)**: Deploy automatizado

---

## 🛡️ Boas Práticas

### ✅ **FAÇA**

1. **Use variáveis de ambiente** para TODAS as credenciais
2. **Rotacione secrets** regularmente (trimestral mínimo)
3. **Use secrets managers** em produção (AWS Secrets Manager, HashiCorp Vault)
4. **Habilite 2FA** no GitHub e Docker Hub
5. **Limite acesso** aos secrets (princípio do menor privilégio)
6. **Monitore acessos** aos secrets
7. **Use HTTPS/TLS** para toda comunicação
8. **Faça backup** dos secrets de forma segura

### ❌ **NÃO FAÇA**

1. ❌ Commitar arquivos `.env` no Git
2. ❌ Usar senhas fracas (`admin`, `123456`, `postgres`)
3. ❌ Compartilhar secrets via email/chat
4. ❌ Reutilizar senhas entre ambientes
5. ❌ Deixar secrets em logs
6. ❌ Hardcode credentials no código
7. ❌ Usar `latest` tag em produção
8. ❌ Expor portas desnecessárias

---

## 🔍 Auditoria de Segurança

### **Verificar se há secrets expostos**

```bash
# Scan no repositório
git secrets --scan

# Verificar histórico
git log --all --full-history -- '*.env'

# Usar ferramentas
truffleHog --regex --entropy=True .
gitleaks detect
```

### **Se um secret foi exposto:**

1. **Revogue imediatamente**
2. **Rotacione todos os secrets relacionados**
3. **Audite logs de acesso**
4. **Notifique equipe de segurança**
5. **Limpe o histórico do Git** (use BFG Repo-Cleaner)

---

## 📚 Recursos

- [12-Factor App](https://12factor.net/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [GitHub Actions Security](https://docs.github.com/en/actions/security-guides)

---

## 🆘 Suporte

Para questões de segurança: security@ia369.com

**Em caso de vulnerabilidade encontrada:**
1. NÃO crie issue público
2. Envie email para security@ia369.com
3. Inclua detalhes da vulnerabilidade
4. Aguarde resposta em até 48h