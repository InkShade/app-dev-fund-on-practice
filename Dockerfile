# Build stage: tests are skipped here because the CI pipelines already run them
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

# Runtime stage: JRE only, running as a non-root user
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home fridgechef
COPY --from=build /app/target/fridgechef-*.jar app.jar
USER fridgechef
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
