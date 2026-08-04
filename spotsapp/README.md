# Spots App — Backend (microservicio `spotsapp`)

Spring Boot 4 (Kotlin) + PostgreSQL + Flyway + Cognito (JWT) + S3.

> Este es el microservicio de dominio dentro del monorepo `ae_2026_01_vela_sarango_nrc_1473`
> (Arquitectura Empresarial). El otro microservicio es [`../users`](../users) (perfiles de
> usuario), y `../nginx` es el gateway que reparte tráfico entre los dos — ver
> [ADR-005](../docs/adr/ADR-005-repos-separados.md) (por qué mobile va en un repo aparte) y
> [ADR-006](../docs/adr/ADR-006-microservicios-users-spotsapp.md) (por qué el backend se
> partió en dos microservicios). El resto de docs (ADRs, requisitos, arquitectura de nube,
> plan de despliegue, Emprendimiento) están en [`../docs`](../docs).

## Correr todo el stack (los dos microservicios + nginx + BDs + pgAdmin)

Desde la **raíz del monorepo** (un nivel arriba de esta carpeta), no desde acá:

```bash
cd ..
cp .env.example .env
# edita .env con tus valores reales (COGNITO_ISSUER_URI, contraseñas de BD, AWS_S3_BUCKET, etc.)
docker compose up -d --build
```

Con nginx delante, este servicio deja de tener un puerto propio expuesto al host — todo pasa
por `http://localhost/` (puerto 80). Para desplegarlo en EC2 (plan gratuito, sin dominio),
sigue [`../docs/despliegue-ec2.md`](../docs/despliegue-ec2.md).

## Correr SOLO este microservicio (desarrollo local, sin Docker)

## Requisitos locales (sin Docker, para desarrollar)

- JDK 21
- Gradle (o usar el wrapper una vez generado, ver abajo)
- PostgreSQL corriendo localmente (o desde la raíz del monorepo, `docker compose up spotsapp-db` para solo la base de datos de este servicio)

## Gradle Wrapper

El wrapper está completo (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` y
`gradle-wrapper.properties`, generados con Spring Initializr apuntando a **Gradle 9.5.1**, la
versión compatible con Spring Boot 4.1.0). No requiere ningún paso adicional: basta con tener
JDK 21 y ejecutar `./gradlew` directamente (la primera vez descarga la distribución de Gradle
9.5.1 automáticamente).

## Levantar una base de datos local de prueba

```bash
docker run --name spotsapp-db -e POSTGRES_DB=spotsapp -e POSTGRES_USER=spotsapp \
  -e POSTGRES_PASSWORD=spotsapp -p 5432:5432 -d postgres:16
```

## Compilar y correr

```bash
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Al arrancar, Flyway ejecuta automáticamente `V1__init.sql` y crea las 5 tablas
(`categories`, `spots`, `media`, `reviews`, `follows`) en la base `spotsapp`.

## Verificar la migración manualmente

```bash
docker exec -it spotsapp-db psql -U spotsapp -d spotsapp -c '\dt'
```

Debe listar las 5 tablas más `flyway_schema_history`.

## Estructura de paquetes (`com.spotsapp`)

```
controllers/   → Fase 7
services/      → Fase 5
repositories/  → Fase 3
entities/      → Fase 2 (actual)
  entities/enums/  → SpotStatus, Rarity, MediaType
dto/           → Fase 3
mappers/       → Fase 3
exceptions/    → Fase 4
config/        → Fase 6 (SecurityConfig, JwtAuthenticationConverter)
```

## Nota de verificación (Fase 2)

Las entidades y la migración fueron escritas manualmente y revisadas por consistencia
(nombres de columnas, tipos, constraints) entre el código Kotlin y el SQL. El `gradle-wrapper.jar`
se probó en este entorno (`./gradlew --version`) y arranca correctamente — solo falla el paso de
descarga de la distribución de Gradle porque este entorno no tiene acceso a red, lo cual **no
ocurrirá en tu máquina con internet**. Aun así, **no se pudo ejecutar `./gradlew build` completo
ni `flyway migrate` contra una base real** dentro de este entorno por la misma razón (no hay red
para bajar las dependencias de Maven Central). Corre `./gradlew build` localmente como primer
paso antes de continuar a la Fase 3, y repórtame cualquier error de compilación para corregirlo.
