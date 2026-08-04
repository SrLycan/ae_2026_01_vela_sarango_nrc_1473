# ---------- Etapa 1: build ----------
# Compila el jar ejecutable con el JDK completo. Esta etapa no viaja a producción.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar primero solo los archivos de configuración de Gradle (no el código fuente).
# Así, mientras no cambien build.gradle.kts/settings.gradle.kts/el wrapper, Docker
# reutiliza la capa con las dependencias ya descargadas en vez de bajarlas de nuevo
# cada vez que cambia una línea de código Kotlin.
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# Ahora sí, el código fuente y la compilación del jar.
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- Etapa 2: runtime ----------
# Imagen liviana, solo con el JRE — no incluye Gradle ni el JDK completo ni el código fuente.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# No correr como root dentro del contenedor.
RUN groupadd --system spotsapp && useradd --system --gid spotsapp spotsapp

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown spotsapp:spotsapp app.jar
USER spotsapp

EXPOSE 8080

# Chequeo de salud simple (sin curl/wget en la imagen base): confirma que algo
# está escuchando en el puerto 8080 usando el redirector /dev/tcp de bash.
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
  CMD bash -c 'echo > /dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
