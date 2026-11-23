# syntax=docker/dockerfile:1
FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
COPY src/main/ src/main/
RUN --mount=type=cache,target=/root/.m2 mvn -B -U package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /build/target/quarkus-app/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
