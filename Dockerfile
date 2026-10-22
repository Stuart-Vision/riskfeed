# Build stage: compile and package with Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 riskfeed
WORKDIR /app
COPY --from=build /src/target/riskfeed-1.0.0.jar app.jar
USER riskfeed
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
