# Etapa 1: Build (Compila o projeto usando Maven)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
LABEL authors="lucas"
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Execução (Roda o projeto com uma imagem Java leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copia o .jar gerado na Etapa 1
COPY --from=build /app/target/*.jar app.jar
# Pasta para os uploads
RUN mkdir uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
