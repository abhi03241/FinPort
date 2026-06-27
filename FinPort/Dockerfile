# syntax=docker/dockerfile:1.6

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies separately
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -ntp dependency:go-offline

# Build the application
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -ntp -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user for security
RUN groupadd --system artha && useradd --system --gid artha artha
USER artha

COPY --from=build /workspace/target/artha.jar /app/artha.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/artha.jar"]