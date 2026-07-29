FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S oficina && adduser -S oficina -G oficina
COPY --from=build /app/target/oficina-mecanica-api-*.jar app.jar
USER oficina
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
