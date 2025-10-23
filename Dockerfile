# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Verify build output (optional, for debugging)
RUN ls -la /app/target/

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /

ENV PORT=8080
EXPOSE 8080

# Copy the JAR (using wildcard to avoid version issues)
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]