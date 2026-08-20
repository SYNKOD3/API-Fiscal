FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --home-dir /app fiscalapi
# A pasta de schemas nasce vazia de proposito: os XSD da SEFAZ nao vem na
# java-nfe nem sao redistribuidos aqui. Vazia, a emissao segue sem validacao
# local e o log avisa; monte um volume com os XSD para liga-la.
RUN mkdir -p /var/lib/fiscal-api/certificates /var/lib/fiscal-api/schemas \
    && chown -R fiscalapi:fiscalapi /var/lib/fiscal-api
COPY --from=build /workspace/target/fiscal-api-0.0.1-SNAPSHOT.jar /app/fiscal-api.jar
USER fiscalapi
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/fiscal-api.jar"]
