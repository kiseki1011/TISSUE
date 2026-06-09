# syntax=docker/dockerfile:1.7

FROM bellsoft/liberica-openjdk-alpine:21 AS builder

WORKDIR /app

# Gradle wrapper + root build files (lombok.config: copyableAnnotations for @Qualifier;
# gradle.properties: project version baked into application.yml)
COPY gradle gradle
COPY gradlew settings.gradle build.gradle gradle.properties lombok.config ./
COPY config config
RUN chmod +x gradlew

# Module build files (all modules the bootstrap depends on)
COPY tissue-core/build.gradle tissue-core/
COPY tissue-security/build.gradle tissue-security/
COPY tissue-notification/build.gradle tissue-notification/
COPY tissue-admin/build.gradle tissue-admin/
COPY tissue-mcp/build.gradle tissue-mcp/
COPY tissue-bootstrap/build.gradle tissue-bootstrap/

# Sources
COPY tissue-core/src tissue-core/src
COPY tissue-security/src tissue-security/src
COPY tissue-notification/src tissue-notification/src
COPY tissue-admin/src tissue-admin/src
COPY tissue-mcp/src tissue-mcp/src
COPY tissue-bootstrap/src tissue-bootstrap/src

# Build
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :tissue-bootstrap:bootJar \
      -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck -x spotlessJavaCheck

FROM bellsoft/liberica-openjre-alpine:21

WORKDIR /app

# curl for HEALTHCHECK; tzdata for accurate timestamps
RUN apk add --no-cache curl tzdata \
    && addgroup -S tissue \
    && adduser -S tissue -G tissue \
    && mkdir -p /var/tissue/storage \
    && chown -R tissue:tissue /var/tissue

COPY --from=builder --chown=tissue:tissue /app/tissue-bootstrap/build/libs/*.jar /app/app.jar

USER tissue

EXPOSE 8080 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
