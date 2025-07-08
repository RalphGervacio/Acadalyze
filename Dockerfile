# Use official Java 21 base image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the JAR file (adjust name if needed)
COPY target/acadalyze-0.0.1-SNAPSHOT.jar app.jar

# Expose port (for documentation; not required by Cloud Run)
EXPOSE 8080

# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
