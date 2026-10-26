# Stage 1: Build the Spring Boot fat JAR with Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy project definition and source code
COPY pom.xml settings.xml* ./
COPY src ./src

# Package the application skipping unit tests for fast builds
RUN mvn clean package -DskipTests

# Stage 2: Create a secure, lightweight runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the compiled JAR from the build stage
COPY --from=build /app/target/quickbite-backend-1.0.0.jar app.jar

# Render routes HTTP traffic to port 8080 by default
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
