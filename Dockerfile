# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (layer caching optimization)
COPY pom.xml .
RUN mvn dependency:resolve

# Copy entire source code
COPY . .

# Copy static frontend files to Spring Boot's static resources directory
# (HTML, CSS, JS files at root level must go into src/main/resources/static/ to be served by Spring Boot)
RUN mkdir -p src/main/resources/static && \
    cp index.html checkout.html chemistry.html collections.html findFragrance.html src/main/resources/static/ && \
    cp javas.js style.css src/main/resources/static/ && \
    cp -r assets src/main/resources/static/

# Build the JAR
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/aether-beauty-0.0.1-SNAPSHOT.jar app.jar

# Expose the default port (actual port comes from PORT env variable)
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
