# One recipe for all three services (auth, dates, notification).
# Step 1 builds the jar with Maven; step 2 runs it on a small Java-only image.
# Works on Oracle's Ampere (ARM) servers and on normal PCs.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Download libraries first, so code changes don't re-download them
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/*.jar /app.jar

FROM eclipse-temurin:21-jre
# Run as a normal user, not root
RUN useradd --system --uid 10001 app
COPY --from=build /app.jar /app/app.jar
USER app
# Use at most 60% of the container's memory; restart cleanly if it ever runs out
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
