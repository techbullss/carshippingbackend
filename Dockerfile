FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY carshippingbackend/pom.xml .
RUN mvn -f pom.xml dependency:go-offline
COPY StainlesSteel/src ./src
RUN mvn -f pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]