# Single-service image (what Render runs): the React app is built and bundled into the Spring Boot jar, so the UI and
# /api share one origin — the HttpOnly refresh cookie stays first-party (Safari-safe) and no CORS is involved.
# backend/Dockerfile is still there for an API-only deployment behind a separate frontend host.

FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /src
COPY backend/pom.xml .
COPY backend/.mvn .mvn
COPY backend/mvnw .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist src/main/resources/static
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 app
COPY --from=api /src/target/*.jar /app/app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
