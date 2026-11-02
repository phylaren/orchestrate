# Етап збірки: JDK 25 + Gradle Wrapper збирають автономний Fat JAR.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Спершу лише файли збірки, щоб шар із залежностями кешувався між змінами коду.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY orchestrate-notification-starter/build.gradle.kts orchestrate-notification-starter/
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY orchestrate-notification-starter/src orchestrate-notification-starter/src
COPY src src
RUN ./gradlew bootJar --no-daemon \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

# Етап виконання: лише JRE 25, без вихідного коду та Gradle.
FROM eclipse-temurin:25-jre

# curl потрібен лише для healthcheck у docker-compose.yml.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Принцип найменших привілеїв: окремий системний користувач без shell та домашньої теки.
RUN groupadd --system orchestrate \
    && useradd --system --gid orchestrate --no-create-home --shell /usr/sbin/nologin orchestrate

WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
# Логи застосунку та access-логи Tomcat пишуться у відносну теку logs.
RUN mkdir logs && chown orchestrate:orchestrate logs

USER orchestrate
EXPOSE 8080

# Exec-форма: java стає PID 1 і сама отримує SIGTERM для graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]
