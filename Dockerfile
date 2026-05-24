# Single Dockerfile reused by configserver, eurekaserver and player.
# Build with: docker build --build-arg SERVICE=<configserver|eurekaserver|player> .
FROM eclipse-temurin:17-jre-alpine

ARG SERVICE
WORKDIR /app

# Copia el fat jar del módulo correspondiente. spring-boot-maven-plugin
# deja también un .jar.original con las clases sin repackagear; el glob *.jar
# no lo selecciona (extension distinta).
COPY ${SERVICE}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
