FROM eclipse-temurin:17-jre

# Create app directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Expose port (adjust per service)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

