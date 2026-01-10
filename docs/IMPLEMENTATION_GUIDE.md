# 🚀 Guia de Implementação - IA369

## 📋 Checklist de Implementação

Use este guia para implementar todas as melhorias de segurança e DevOps.

---

## ✅ FASE 1: Gestão de Secrets (1-2 horas)

### **1.1 Criar estrutura de arquivos**

```bash
cd ~/IA369

# Criar diretórios
mkdir -p scripts k8s .github/workflows

# Criar arquivos de ambiente
touch .env.example .env.dev .env.prod

# Atualizar .gitignore
nano .gitignore
```

**Copie o conteúdo dos artifacts:**
- `.env.example`
- `.env.dev`
- `.env.prod`
- `.gitignore`

### **1.2 Preencher variáveis de ambiente**

```bash
# Editar .env.dev com valores reais
nano .env.dev

# Gerar JWT Secret seguro
openssl rand -base64 64
```

### **1.3 Atualizar docker-compose.yml**

```bash
# Backup do atual
cp docker-compose.yml docker-compose.yml.backup

# Substituir pelo novo (com variáveis de ambiente)
nano docker-compose.yml
```

**Copie o conteúdo do artifact: `docker-compose.yml (com .env)`**

### **1.4 Testar localmente**

```bash
# Parar containers atuais
sudo docker compose down

# Criar .env para teste
cp .env.dev .env

# Subir com novas configurações
sudo docker compose up -d

# Verificar logs
sudo docker compose logs -f

# Testar endpoints
curl http://localhost:8761
curl http://localhost:8080/actuator/health
```

### **✅ Checklist Fase 1**

- [ ] Arquivos `.env.*` criados
- [ ] `.gitignore` atualizado
- [ ] `docker-compose.yml` usando variáveis
- [ ] Testado localmente
- [ ] Nenhum `.env` commitado no Git

---

## ✅ FASE 2: Docker Secrets (2-3 horas)

### **2.1 Preparar scripts**

```bash
# Criar script de secrets
nano scripts/create-secrets.sh

# Dar permissão
chmod +x scripts/create-secrets.sh
```

**Copie o conteúdo do artifact: `scripts/create-secrets.sh`**

### **2.2 Criar docker-compose.secrets.yml**

```bash
nano docker-compose.secrets.yml
```

**Copie o conteúdo do artifact: `docker-compose.secrets.yml`**

### **2.3 Testar Docker Swarm (opcional)**

```bash
# Inicializar Swarm
docker swarm init

# Criar secrets
./scripts/create-secrets.sh

# Deploy
docker stack deploy -c docker-compose.secrets.yml ia369

# Verificar
docker stack services ia369
docker service logs ia369_eureka-server

# Remover (depois do teste)
docker stack rm ia369
docker swarm leave --force
```

### **✅ Checklist Fase 2**

- [ ] Script `create-secrets.sh` criado
- [ ] `docker-compose.secrets.yml` criado
- [ ] (Opcional) Testado com Docker Swarm
- [ ] Documentado no README

---

## ✅ FASE 3: GitHub Actions (3-4 horas)

### **3.1 Criar workflows**

```bash
# Criar diretório
mkdir -p .github/workflows

# CI
nano .github/workflows/ci.yml

# CD
nano .github/workflows/cd.yml
```

**Copie o conteúdo dos artifacts:**
- `.github/workflows/ci.yml`
- `.github/workflows/cd.yml`

### **3.2 Configurar Secrets no GitHub**

1. Acesse: `https://github.com/audalio-devops/IA369/settings/secrets/actions`

2. Adicione os secrets:

```
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Para adicionar mais tarde (quando tiver servidores):
```
PROD_SSH_KEY
PROD_HOST
PROD_USER
STAGING_SSH_KEY
STAGING_HOST
STAGING_USER
```

### **3.3 Criar Docker Hub Token**

```bash
# Acesse: https://hub.docker.com/settings/security
# Crie um novo Access Token
# Copie o token e adicione como DOCKERHUB_TOKEN no GitHub
```

### **3.4 Testar pipeline**

```bash
# Commit e push
git add .github/
git commit -m "feat: add CI/CD pipelines"
git push origin master

# Acompanhar em:
# https://github.com/audalio-devops/IA369/actions
```

### **✅ Checklist Fase 3**

- [ ] Workflows criados
- [ ] Secrets configurados no GitHub
- [ ] Docker Hub configurado
- [ ] Pipeline executado com sucesso
- [ ] Imagens publicadas no Docker Hub

---

## ✅ FASE 4: Kubernetes (4-6 horas)

### **4.1 Criar manifestos Kubernetes**

```bash
# Namespace
nano k8s/00-namespace.yaml

# Secrets (template)
nano k8s/01-secrets.yaml

# ConfigMaps
nano k8s/02-configmaps.yaml

# Deployments (criar para cada serviço)
nano k8s/03-eureka-deployment.yaml
nano k8s/04-gateway-deployment.yaml
# ... etc
```

### **4.2 Instalar ferramentas (se necessário)**

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verificar
kubectl version --client

# Minikube (para testes locais)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

### **4.3 Testar localmente com Minikube**

```bash
# Iniciar Minikube
minikube start

# Criar namespace
kubectl apply -f k8s/00-namespace.yaml

# Criar secrets
kubectl apply -f k8s/01-secrets.yaml

# Deploy serviços
kubectl apply -f k8s/

# Verificar
kubectl get all -n ia369

# Port-forward para testar
kubectl port-forward -n ia369 svc/eureka-server 8761:8761
```

### **✅ Checklist Fase 4**

- [ ] Manifestos K8s criados
- [ ] Testado com Minikube
- [ ] Documentado processo de deploy
- [ ] Preparado para cloud (EKS/GKE/AKS)

---

## ✅ FASE 5: Documentação (1-2 horas)

### **5.1 Criar documentação**

```bash
# Segurança
nano SECURITY.md

# README principal
nano README.md

# Changelog
nano CHANGELOG.md
```

### **5.2 Atualizar README principal**

Adicione seções:
- ✅ Como rodar localmente
- ✅ Variáveis de ambiente
- ✅ Deploy com Docker Compose
- ✅ Deploy com Kubernetes
- ✅ CI/CD
- ✅ Contribuindo
- ✅ Segurança

### **✅ Checklist Fase 5**

- [ ] `SECURITY.md` criado
- [ ] `README.md` atualizado
- [ ] `CHANGELOG.md` criado
- [ ] Diagramas de arquitetura (opcional)

---

## 🎯 Próximos Passos (Futuro)

### **Monitoramento**

- [ ] Prometheus + Grafana
- [ ] Alertmanager
- [ ] Loki para logs
- [ ] Jaeger para tracing

### **Segurança Adicional**

- [ ] HTTPS com Let's Encrypt
- [ ] OAuth2/OIDC
- [ ] Rate limiting
- [ ] WAF (Web Application Firewall)

### **Performance**

- [ ] Redis cache
- [ ] CDN
- [ ] Load balancer
- [ ] Auto-scaling

### **Backup**

- [ ] Backup automático do PostgreSQL
- [ ] Backup dos secrets
- [ ] Disaster recovery plan

---

## 📊 Cronograma Sugerido

| Fase | Duração | Prioridade |
|------|---------|------------|
| Fase 1: Secrets | 1-2h | 🔴 Alta |
| Fase 2: Docker Secrets | 2-3h | 🟡 Média |
| Fase 3: GitHub Actions | 3-4h | 🔴 Alta |
| Fase 4: Kubernetes | 4-6h | 🟡 Média |
| Fase 5: Documentação | 1-2h | 🟢 Baixa |

**Total: 11-17 horas**

---

## 🆘 Troubleshooting

### **Problema: Pipeline falha no GitHub Actions**

```bash
# Verificar logs
# Actions → Workflow → Job → Step

# Testar localmente
act -l  # Lista workflows
act push  # Simula push event
```

### **Problema: Secrets não funcionam no Docker Compose**

```bash
# Verificar se .env existe
ls -la .env

# Verificar se docker-compose lê o arquivo
docker compose config

# Ver valores das variáveis (CUIDADO em produção!)
docker compose config | grep -A 5 environment
```

### **Problema: Kubernetes não encontra secrets**

```bash
# Verificar namespace
kubectl get secrets -n ia369

# Verificar se o pod consegue ler
kubectl exec -it POD_NAME -n ia369 -- env | grep POSTGRES
```

---

## 📞 Suporte

Dúvidas durante implementação:
- Issues: https://github.com/audalio-devops/IA369/issues
- Email: devops@ia369.com