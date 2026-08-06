# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system usersvc && useradd --system --gid usersvc usersvc

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown usersvc:usersvc app.jar
USER usersvc

EXPOSE 8686

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
  CMD bash -c 'echo > /dev/tcp/127.0.0.1/8686' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
