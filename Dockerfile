# -------- Build stage --------
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy only what Gradle needs first (better caching)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (best-effort for cache)
RUN ./gradlew dependencies --no-daemon || true

# Copy source
COPY src src

# Build the application
RUN ./gradlew bootJar --no-daemon

# -------- Runtime stage --------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
