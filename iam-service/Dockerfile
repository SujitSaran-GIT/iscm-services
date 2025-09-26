# Build stage
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install security updates
RUN apk update && apk upgrade && rm -rf /var/cache/apk/*

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

WORKDIR /app

# Copy JAR from build stage
COPY --from=builder /app/target/iam-service-*.jar app.jar

# Create directory for logs with proper permissions
RUN mkdir -p /app/logs && chown spring:spring /app/logs

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/iam/actuator/health || exit 1

EXPOSE 8081

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]