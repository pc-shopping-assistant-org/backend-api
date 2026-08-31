FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace/server

COPY server/pom.xml ./
COPY server/.mvn ./.mvn

COPY server/src ./src

RUN mvn -B -ntp -DskipTests package \
    && jar_file="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' -print -quit)" \
    && test -n "$jar_file" \
    && cp "$jar_file" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --create-home app

COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
