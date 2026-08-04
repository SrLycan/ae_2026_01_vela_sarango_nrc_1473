# Plan Financiero — SpotsApp

## Estimación de Costos Mensuales (MVP)

| Recurso | Proveedor | Costo Estimado (USD/mes) |
|---------|-----------|--------------------------|
| Backend (ECS t3.medium × 2) | AWS | ~$60 |
| Base de datos (RDS db.t3.small) | AWS | ~$30 |
| Almacenamiento S3 (media) | AWS | ~$10 |
| Cognito (hasta 50k usuarios) | AWS | ~$0 |
| Google Maps API (hasta 28k cargas/día) | Google | ~$20 |
| Balanceador (ALB) | AWS | ~$20 |
| **Total mensual estimado** | | **~$140** |

## Proyección de Usuarios (12 meses)

| Mes | Usuarios Registrados | Usuarios Activos (MAU) | Spots Aprobados |
|-----|---------------------|----------------------|-----------------|
| Mes 1 | 200 | 80 | 50 |
| Mes 3 | 1,000 | 350 | 300 |
| Mes 6 | 5,000 | 1,500 | 1,200 |
| Mes 12 | 20,000 | 5,000 | 5,000 |

## Monetización

### Plan Freemium

| Plan | Precio | Características |
|------|--------|-----------------|
| Gratuito | $0 | Explorar, crear spots, reseñar, seguir usuarios |
| Premium | $3.99/mes | Estadísticas avanzadas, insignias exclusivas, sin anuncios |

### Spots Patrocinados (a partir del Mes 6)
- Spot destacado por 7 días: $10
- Paquete mensual (4 spots rotativos): $30

### Proyección de Ingresos

| Mes | Usuarios Premium (5%) | Spots Patrocinados | Ingreso Estimado |
|-----|----------------------|-------------------|------------------|
| Mes 1 | 4 | 0 | ~$16 |
| Mes 3 | 17 | 0 | ~$68 |
| Mes 6 | 75 | 5 | ~$449 |
| Mes 12 | 250 | 20 | ~$1,597 |

## Punto de Equilibrio (Break-even)

Con costos fijos de ~$140/mes y un ingreso promedio por usuario premium de $3.99/mes:
- Se necesitan ~35 suscriptores premium para cubrir costos fijos.
- Proyectado: alcanzable en el Mes 4-5 con ~1,500 usuarios registrados (5% → 75 premium).

## Inversión Inicial

| Concepto | Costo Estimado |
|----------|---------------|
| Desarrollo (3 meses, equipo 2 personas) | $0 (proyecto académico) |
| Setup infraestructura cloud | ~$50 |
| Google Cloud Platform (API keys) | ~$20 |
| **Total inversión inicial** | **~$70** |

## KPIs Financieros

- **CAC (Costo de Adquisición de Cliente)**: ~$0.50 (orgánico, redes sociales)
- **LTV (Lifetime Value)**: ~$35.91 (9 meses promedio × $3.99)
- **Margen bruto**: ~85% (costos variables muy bajos)
- **Payback period**: ~4 meses por usuario adquirido
