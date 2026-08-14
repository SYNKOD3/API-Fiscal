FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --home-dir /app fiscalapi
COPY --from=build /workspace/target/fiscal-api-0.0.1-SNAPSHOT.jar /app/fiscal-api.jar
USER fiscalapi
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/fiscal-api.jar"]
