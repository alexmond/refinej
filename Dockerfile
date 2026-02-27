FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="RefineJ" \
      org.opencontainers.image.description="Java refactoring CLI powered by static analysis" \
      org.opencontainers.image.source="https://github.com/alexmond/refinej" \
      org.opencontainers.image.licenses="MIT"

RUN addgroup -S refinej && adduser -S refinej -G refinej

COPY refinej-cli/target/refinej-cli-*.jar /app/refinej.jar

USER refinej
WORKDIR /workspace

ENTRYPOINT ["java", "-jar", "/app/refinej.jar"]
