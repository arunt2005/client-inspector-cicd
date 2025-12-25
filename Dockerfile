# Stage 1: Build the application using Gradle 9.2.1 and JDK 21
FROM gradle:9.2.1-jdk21 AS build
WORKDIR /app

# Copy configuration files first for better layer caching
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the fat JAR
RUN gradle clean bootJar -x test

# Stage 2: Java 21 Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the JAR from the build stage (already named app.jar via build.gradle)
COPY --from=build /app/build/libs/app.jar .

# Create log directory
RUN mkdir logs

EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]