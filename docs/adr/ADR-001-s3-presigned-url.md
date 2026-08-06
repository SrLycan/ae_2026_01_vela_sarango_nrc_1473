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

## Lectura (GET): bucket policy de solo-lectura pública sobre `spots/*`

La subida (PUT) va firmada porque escribir sí debe estar restringido al dueño del spot. Mostrar
la foto después (GET), en cambio, no necesita ese control — cualquiera puede ver un spot público
en la app, con o sin sesión (`GET /spots` es `permitAll`), así que sus fotos también deberían
poder cargarse sin autenticación. `MediaService.presign()` ya devuelve `publicUrl` como una URL
directa de S3 (`https://bucket.s3.region.amazonaws.com/key`), sin firmar — para que esa URL
funcione, el bucket necesita una **bucket policy de solo lectura** acotada al prefijo `spots/*`
(el mismo que usa `MediaService` al construir el `key`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadSpotMedia",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::<BUCKET>/spots/*"
    }
  ]
}
```

Esto exige desactivar 2 de las 4 casillas de "Block public access" del bucket (las de
**bucket policies**; las 2 de ACLs se dejan activadas, no se usan ACLs en este proyecto — el
Object Ownership es "Bucket owner enforced"). El resto de operaciones (`PutObject`,
`DeleteObject`, `ListBucket`) siguen exigiendo credenciales válidas (el Role de la instancia o
un presigned PUT) — esta policy solo abre lectura, y solo bajo `spots/*`.

Alternativa descartada: generar también un presigned GET por cada foto en cada respuesta de la
API (en vez de una bucket policy). Es más "cerrado" pero agrega una llamada a S3 por cada media
en cada `GET /spots/{id}/media`, y las fotos de un spot aprobado no son información sensible —
no se justifica esa complejidad para este caso.
