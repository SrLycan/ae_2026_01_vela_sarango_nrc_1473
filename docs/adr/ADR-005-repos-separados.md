# ADR-005: Backend y app móvil en repositorios separados (no monorepo)

## Contexto

El proyecto arrancó como un monorepo (`spots-app/backend` + `spots-app/mobile` + `docs/` en un
solo repositorio Git) durante el prototipado inicial. El ingeniero de Arquitectura de Software
indicó explícitamente que backend y app móvil **no** deben vivir en el mismo repositorio.

Además de cumplir esa indicación, un monorepo entre estos dos componentes no encaja bien con
cómo cambian en la práctica:

- **Ciclos de vida distintos**: el backend se despliega (a EC2) de forma independiente de cuándo
  se genera un nuevo APK; un commit que solo toca Kotlin de Android no debería disparar ni
  mezclarse con el historial de despliegue del backend, y viceversa.
- **Distinto stack de build**: backend es un proyecto Gradle/JVM puro (Spring Boot); mobile es un
  proyecto Gradle/Android (con Android Gradle Plugin, SDKs de Android, y firmas de release) — son
  toolchains diferentes aunque ambos usen Gradle.
- **GitFlow por equipo/materia**: la rúbrica de Análisis de Diseño evalúa el manejo de GitFlow del
  equipo; un monorepo con dos frentes de trabajo (backend y mobile) tiende a mezclar ramas
  `feature/*` de ambos mundos en el mismo historial, dificultando revisar el flujo de trabajo de
  cada lado por separado.

## Decisión

Separar el monorepo original en dos repositorios independientes:

- **`spots-app-backend`**: código Kotlin/Spring Boot, `docker-compose.yml`, `Dockerfile`, y toda
  la documentación transversal del proyecto (`docs/`: ADRs, requisitos, arquitectura de nube, plan
  de despliegue, Business Model Canvas, plan financiero) — vive aquí porque describe al sistema
  como un todo (RF/RNF, infraestructura, negocio), no solo al backend.
- **`spots-app-mobile`**: código Kotlin/Jetpack Compose de la app Android, con su propio
  `README.md` (setup de Cognito, Google Maps/Places, variables de build).

Cada repo tiene su propio historial de commits, su propio flujo de GitFlow, y se versiona/despliega
de forma independiente. El backend expone un contrato HTTP estable (`docs/matriz-endpoints.md`)
que la app móvil consume — ese contrato, no el código fuente, es lo que los acopla.

## Consecuencias

**Positivas**
- Cumple la restricción explícita del curso de Arquitectura de Software.
- Cada equipo/persona puede trabajar y hacer GitFlow en su propio repo sin ruido del otro lado.
- El backend puede desplegarse (EC2) sin que un cambio de UI en mobile aparezca en ese historial.
- Facilita que, a futuro, cada repo tenga su propio pipeline de CI si el proyecto crece.

**Negativas**
- Un cambio que afecta a ambos lados a la vez (p. ej. agregar un campo a un DTO que el backend
  expone y que mobile debe empezar a consumir) ahora requiere coordinar dos PRs en dos repos en
  vez de uno solo — se mitiga documentando el contrato en `docs/matriz-endpoints.md` como fuente
  de verdad compartida.
- No hay un solo `git log` que muestre la evolución completa del proyecto en una sola línea de
  tiempo; hay que mirar los dos repos para reconstruir la historia completa de una fecha dada.
