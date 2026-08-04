# Business Model Canvas — SpotsApp

## Propuesta de Valor
- Descubrimiento gamificado de spots turísticos mediante un mapa interactivo con pines por rareza.
- Usuarios contribuyen con contenido (spots, reseñas, fotos) y ganan puntos/medallas.
- Moderación colaborativa: admins revisan y aprueban spots, asignando rareza y puntos.

## Segmentos de Cliente
- **Viajeros/exploradores**: buscan lugares únicos fuera de las rutas turísticas tradicionales.
- **Creadores de contenido**: ganan reconocimiento y puntos por compartir sus descubrimientos.
- **Comunidades locales**: promocionan su ciudad/región a través de spots verificados.

## Canales
- App móvil Android (Kotlin/Jetpack Compose) — canal principal de interacción.
- API REST pública — permite integraciones de terceros.
- Web/app futura — expansión planificada.

## Relación con Clientes
- **Autoservicio**: registro/login con Cognito, exploración guiada por gamificación.
- **Comunidad**: sistema de follows, feed de actividad, reseñas entre pares.
- **Moderación**: admins curan la calidad del contenido (spots aprobados/rechazados).

## Fuentes de Ingreso
- **Freemium**: app gratuita con funcionalidad base; suscripción premium (stats avanzadas, insignias exclusivas).
- **Spots patrocinados**: negocios locales pueden promocionar spots destacados.
- **API de terceros**: cobro por consultas a la API para integraciones comerciales.

## Recursos Clave
- Backend: Spring Boot 4, PostgreSQL, S3 (media), Cognito (auth).
- Infraestructura: Docker, AWS (ALB, ECS/Fargate).
- Talento: desarrolladores Kotlin/Spring Boot, Android, DevOps.

## Actividades Clave
- Desarrollo y mantenimiento de backend y app móvil.
- Moderación de contenido (aprobación/rechazo de spots).
- Marketing y crecimiento de comunidad de usuarios.

## Socios Clave
- AWS (Cognito, S3, hosting cloud).
- Google (Maps SDK, Places API).
- Operadores turísticos locales (contenido y promoción).

## Estructura de Costos
- Infraestructura cloud (AWS: EC2/ECS, RDS, S3, Cognito).
- APIs externas (Google Maps/Places).
- Desarrollo y mantenimiento del equipo técnico.

## Métricas Clave
- Usuarios activos (MAU/DAU).
- Spots creados y aprobados.
- Reseñas y puntos acumulados por usuarios.
- Retención de usuarios a 30/90 días.
