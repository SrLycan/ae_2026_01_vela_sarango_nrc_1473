# ADR-001: Subida de multimedia mediante URLs prefirmadas de S3

## Contexto

Los usuarios necesitan adjuntar imágenes (y potencialmente video) a un `Spot` a través de la
entidad `Media`. El backend podría (a) recibir el archivo binario en el propio endpoint REST y
subirlo él mismo a S3, o (b) generar una URL prefirmada (presigned URL) para que el cliente
móvil suba el archivo directamente a S3.

## Decisión

Se opta por **URLs prefirmadas de S3**: el endpoint `POST /spots/{id}/media` no recibe el
binario; genera y devuelve una presigned URL (PUT) con expiración corta. El cliente Android sube
el archivo directamente a S3 usando esa URL, y luego confirma al backend la URL final para
persistir el registro `Media`.

## Consecuencias

**Positivas**
- El backend nunca maneja binarios pesados, reduciendo carga de CPU/memoria y ancho de banda.
- Escala mejor: la subida va directo a S3, no pasa por el balanceador ni los contenedores del backend.
- Menor superficie de ataque en el backend (no hay parsing de multipart/form-data de archivos grandes).

**Negativas**
- Se requiere un paso adicional de confirmación (`registrar` la URL final tras la subida), lo que
  introduce un estado intermedio donde el backend "espera" que el cliente confirme.
- Se debe validar en el paso de confirmación que el spot pertenezca al usuario autenticado, para
  evitar que alguien registre media en un spot ajeno.
- Requiere configurar correctamente CORS y política de bucket en S3 para permitir el PUT directo
  desde el cliente.
