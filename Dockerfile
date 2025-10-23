FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
ENV PORT=8080
EXPOSE 8080
COPY --from=builder /app/target/learning-backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
