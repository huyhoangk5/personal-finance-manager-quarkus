# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/quarkus-app/ ./

# Use the PORT environment variable provided by Render
ENV QUARKUS_HTTP_PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]
