# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy poms
COPY pom.xml .
COPY forge-engine/pom.xml forge-engine/
COPY forge-platform/pom.xml forge-platform/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build executable JAR and skip test compilation to avoid Lombok test issues
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Stage 2: Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the executable jar
COPY --from=build /app/forge-platform/target/forge-platform-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]