# Orders API – Java + Spring Boot (Taller Benchmarking con JMeter)

## 1. Levantar el entorno

```bash
docker compose up -d --build
docker ps
```

La API queda en `http://localhost:5001/api/orders` y Postgres en `localhost:5433`.
Las tablas `orders` y `order_items` se crean solas con `init.sql`.

## 2. Probar el endpoint

```bash
curl -i -X POST http://localhost:5001/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C123","items":[{"productId":"P1","quantity":2,"price":10.5},{"productId":"P2","quantity":1,"price":5.0}]}'
```

Respuesta esperada (201):

```json
{"orderId":"...","customerId":"C123","totalAmount":26.00,"itemsCount":3,"processedAt":"2026-11-17T20:40:00.123"}
```

Nota: `totalAmount = sum(price * quantity)` = 10.5*2 + 5.0*1 = 26.00 y `itemsCount = sum(quantity)` = 3.
Si tu profesor espera `itemsCount` como el número de líneas (2), cambia en `OrderService.java` `itemsCount += it.quantity()` por `itemsCount++`.

Verificar en BD:

```bash
docker exec -it db-orders psql -U postgres -d orders_db -c "SELECT count(*) FROM orders;" -c "SELECT count(*) FROM order_items;"
```

## 3. Ejecutar JMeter (modo CLI, recomendado para medir)

El plan `orders-test-plan.jmx` recibe parámetros `users`, `ramp` y `duration` (segundos).

```bash
mkdir -p results

# Baja (10 usuarios)
jmeter -n -t orders-test-plan.jmx -Jusers=10 -Jramp=5 -Jduration=60 -l results/baja.jtl -e -o results/report-baja

# Media (50)
jmeter -n -t orders-test-plan.jmx -Jusers=50 -Jramp=10 -Jduration=60 -l results/media.jtl -e -o results/report-media

# Alta (100)
jmeter -n -t orders-test-plan.jmx -Jusers=100 -Jramp=20 -Jduration=60 -l results/alta.jtl -e -o results/report-alta

# Estrés: de 20 en 20 hasta ver degradación
for u in 20 40 60 80 100 120 140 160 180 200; do
  jmeter -n -t orders-test-plan.jmx -Jusers=$u -Jramp=15 -Jduration=60 -l results/estres-$u.jtl -e -o results/report-estres-$u
done
```

Para las capturas del enunciado (Summary Report, Aggregate Report, pantallazo durante la carga) abre el `.jmx` en la GUI,
ejecútalo desde ahí con el mismo número de usuarios (cambia el valor por defecto de `users` en User Defined Variables) y toma las capturas.
El enunciado menciona 20/70/150 en un punto y 10/50/100 en otro; confirma con tu profesor cuáles usar.

Entre escenarios limpia los datos para que sean comparables:

```bash
docker exec -it db-orders psql -U postgres -d orders_db -c "TRUNCATE orders, order_items RESTART IDENTITY;"
```

## 4. Monitoreo durante cada prueba (en terminales separadas)

```bash
htop
docker stats api-orders db-orders
# para guardar en archivo:
docker stats api-orders db-orders --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}}" > results/stats-baja.csv
```

Con esto llenas la tabla de CPU/RAM de API y DB.

## 5. Dónde sacar las métricas

| Métrica pedida | Dónde |
|---|---|
| Average response time | Summary/Aggregate Report: columna Average |
| P95 | Aggregate Report: columna 95% Line |
| Throughput | Columna Throughput (req/s) |
| % Error | Columna Error % |

## 6. Parámetros para experimentar (opcional, útil para el análisis)

Variables de entorno del servicio `api` en el compose: `DB_POOL_SIZE` (default 10) y `TOMCAT_THREADS` (default 200).
Con 100+ usuarios el pool de 10 conexiones suele ser el cuello de botella; puedes mencionarlo en tus observaciones.
