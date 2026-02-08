# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom first for dependency caching
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar
COPY --from=builder /build/target/valuation-service-0.0.1-SNAPSHOT.jar app.jar

# JVM + container-friendly defaults
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
