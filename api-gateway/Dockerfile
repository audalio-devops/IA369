# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar apenas pom.xml primeiro (cache de dependências)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build do projeto
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instalar ferramentas para health check
RUN apk add --no-cache wget netcat-openbsd

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR da stage de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta do API Gateway
EXPOSE 8080

# Variáveis de ambiente
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]