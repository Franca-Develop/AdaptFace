FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

RUN useradd --uid 10001 --create-home adaptface
WORKDIR /app
COPY --from=build --chown=adaptface:adaptface /workspace/target/adaptFace-API-0.0.1-SNAPSHOT.jar app.jar

USER adaptface
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
