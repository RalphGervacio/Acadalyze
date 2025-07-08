# 🧱 Stage 1: Build the JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy everything (your full source code)
COPY . .

# Run Maven build (skip tests for faster build)
RUN mvn clean package -DskipTests

# 🚀 Stage 2: Run the built app
FROM eclipse-temurin:21-jdk

# Working directory for the runtime container
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /app/target/acadalyze-0.0.1-SNAPSHOT.jar app.jar

# Expose the port used by Spring Boot
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
