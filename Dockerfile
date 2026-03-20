# Use a lightweight OpenJDK image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory inside container
WORKDIR /app

# Copy the built jar from local machine into container
COPY target/caresync-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Cloud Run uses 8080 by default)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
