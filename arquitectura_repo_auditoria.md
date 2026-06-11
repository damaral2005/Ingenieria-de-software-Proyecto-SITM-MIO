# Auditoria tecnica de arquitectura del repositorio SITM-MIO

## 1. Resumen ejecutivo

Este informe audita el repositorio del experimento de calculo de velocidades promedio por ruta y mes para el SITM-MIO. La revision se hizo rama por rama, sin modificar codigo, usando la evidencia disponible en Git.

Ramas disponibles observadas con `git branch -a`:

- `main`
- `version-2-threadpool`
- `codex/version-3-distributed-master-worker`
- `remotes/origin/main`
- `remotes/origin/version-2-threadpool`
- `remotes/origin/codex/version-3-distributed-master-worker`
- `remotes/origin/feature/visualization`

Para el analisis de V1, V2 y V3 se usaron las ramas remotas mas completas, porque las ramas locales `main` y `version-2-threadpool` estan atrasadas frente a `origin/main` y `origin/version-2-threadpool`.

| Version | Rama auditada | Commit auditado | Diagnostico corto |
|---|---|---:|---|
| V1 | `origin/main` | `082f88a1e08a5acb084b52bd71c2ab7e9f4890ac` | Monolito batch secuencial. Correcto en MiniPilot, falla por memoria en Pilot. |
| V2 | `origin/version-2-threadpool` | `4f07da1d25a0fe7c50ef080e00b8f54f805c1480` | Thread Pool local. Mejora rendimiento en MiniPilot, pero sigue cargando todos los datagramas y falla en Pilot. |
| V3 | `origin/codex/version-3-distributed-master-worker` | `b0b6d8baadc7c9316b5daf8f8d025f78dce11168` | Master-Worker distribuido. Completa Pilot en modo file-based y con ZeroC Ice. |

Conclusion principal: V3 es la unica version que resuelve la restriccion arquitectonica critica del dataset grande: no cargar el archivo completo en memoria de un solo JVM. V2 mejora performance local, pero no cambia el cuello de botella de memoria. V3 introduce particionamiento por `routeId + busId`, workers y mezcla de resultados parciales, lo cual permite procesar `datagrams4Pilot.csv`.

## 2. Comparacion V1, V2 y V3

### 2.1 Inventario general por version

| Aspecto | V1 | V2 | V3 |
|---|---|---|---|
| Rama exacta | `origin/main` | `origin/version-2-threadpool` | `origin/codex/version-3-distributed-master-worker` |
| Ultimo commit revisado | `082f88a` | `4f07da1` | `b0b6d8b` |
| Lenguaje / runtime | Java 11 CLI | Java 11 CLI | Java 11 CLI + ZeroC Ice |
| Build tool | Gradle wrapper | Gradle wrapper | Gradle wrapper |
| Dependencias relevantes | `commons-csv`, JUnit 5, SpotBugs annotations | `commons-csv`, JUnit 5, SpotBugs annotations | `commons-csv`, ZeroC Ice `3.7.10`, JUnit 5, SpotBugs annotations |
| Comando base | `./gradlew run --args="--lines ... --datagrams ... --output ..."` | Igual a V1, con `--threads N` | Default ThreadPool, o modos `--distributed-master`, `--distributed-worker`, `--distributed-partition`, `--distributed-merge`, `--ice-master`, `--ice-worker-server` |
| Entrada principal | `lines-241-ActiveGT.csv`, `datagrams-MiniPilot.csv`, `datagrams4Pilot.csv` | Igual a V1 | Igual a V1/V2; en Ice los workers reciben particiones por RPC |
| Salida principal | CSV final en `results/` | CSV final en `results/` | CSV final, particiones, manifest, parciales y metricas de workers |
| Carpetas principales | `src/main/java`, `src/test/java`, `docs`, `specs`, `scripts`, `results` | Igual a V1 + evidencia V2 | Igual a V2 + `src/main/java/.../distributed`, `specs/003...`, scripts distribuidos |
| Clases clave | `MonolithicSpeedCalculator`, `GpsDatagramCsvReader`, `DataCleaner`, `SpeedSegmentCalculator`, `RouteMonthAggregator`, `ResultCsvWriter` | `ThreadPoolSpeedCalculator`, `RouteProcessingTask` + clases comunes de V1 | `DistributedSpeedMaster`, `DatagramPartitioner`, `WorkerProcessor`, `WorkerProcessLauncher`, `PartialResultMerger`, `IceDistributedSpeedMaster`, `IceScanWorkerServant` |
| Configuracion importante | Columnas de CSV, `--coordinate-scale`, `--max-gap-minutes`, `--max-speed-kmh` | Lo anterior + `--threads` | Lo anterior + `--workers`, `--partitions`, `--work-dir`, `--ice-workers`, `--ice-host`, `--ice-port`, `JAVA_TOOL_OPTIONS=-Xmx8g` |

### 2.2 Evidencia experimental resumida

| Experimento | V1 | V2 | V3 |
|---|---|---|---|
| MiniPilot | 64,081 ms en evidencia limpia; 111 filas; 7,494,051 segmentos validos | 39,201 ms con 4 threads; 38,643 ms con 12 threads; 111 filas | 30,814 ms local con 4 workers/8 particiones; 53,780 ms con Ice/2 workers remotos |
| Pilot 67 GB | Falla `OutOfMemoryError`; build fallido en 7m40s | Falla `OutOfMemoryError`; build fallido en 7m20s | Completa: 73.43 min file-based; 84.99 min con Ice; 1,443 filas |

## 3. Componentes y responsabilidades

### 3.1 V1 - Monolitica secuencial

| Componente / archivo / clase | Responsabilidad | Entrada | Salida | Relacion con otros componentes |
|---|---|---|---|---|
| `Main` | Punto de entrada Java. | Args CLI | Codigo de salida | Llama `CommandLineApp`. |
| `CommandLineApp` | Orquesta parsing, ejecucion y manejo basico de errores. | Args CLI | Consola, exit code | Usa `CliParser` y `MonolithicSpeedCalculator`. |
| `CliParser` / `CliOptions` | Define opciones de rutas, columnas, indices, escala y umbrales. | Texto CLI | `CliOptions` | Alimenta calculadora. |
| `ActiveRoutesCsvReader` | Lee rutas activas desde `lines-241-ActiveGT.csv`. | CSV con header | `Set<String>` de rutas | Usa `ColumnResolver` y `DataCleaner`. |
| `GpsDatagramCsvReader` | Lee datagramas con o sin header y acumula puntos limpios en memoria. | CSV datagramas | `GpsDatagramReadResult` | Usa `DataCleaner`. |
| `DataCleaner` | Filtra campos vacios, rutas inactivas, timestamps invalidos y coordenadas fuera del area. | Valores crudos CSV | `Optional<GpsPoint>` | Usado por lectores y particionador. |
| `HaversineDistanceCalculator` | Calcula distancia GPS en km. | Lat/lon previas y actuales | Distancia km | Usado por `SpeedSegmentCalculator`. |
| `SpeedSegmentCalculator` | Calcula segmentos validos entre puntos consecutivos de la misma ruta y bus. | Lista ordenada de `GpsPoint` | `List<SpeedSegment>` | Aplica max gap y max speed. |
| `RouteMonthAggregator` | Agrupa segmentos por ruta y mes. | `SpeedSegment` | `RouteMonthSpeed` | Calcula distancia, tiempo, velocidad promedio y buses observados. |
| `ResultCsvWriter` | Escribe CSV deterministico. | Lista de resultados | CSV final | Formato locale-independent con 6 decimales. |
| `MonolithicRunSummary` | Reporta metricas de ejecucion. | Conteos y tiempos | Texto consola | Usado por `CommandLineApp`. |

### 3.2 V2 - Concurrente local con Thread Pool

| Componente / archivo / clase | Responsabilidad | Entrada | Salida | Relacion con otros componentes |
|---|---|---|---|---|
| `CommandLineApp` | Ejecuta calculadora ThreadPool por defecto. | Args CLI | Consola, exit code | Usa `ThreadPoolSpeedCalculator`. |
| `CliParser` / `CliOptions` | Agrega `--threads` y `DEFAULT_THREAD_COUNT`. | Args CLI | `CliOptions` | Configura tamaño del pool. |
| `ThreadPoolSpeedCalculator` | Orquesta lectura, agrupacion por ruta, ejecucion concurrente y escritura final. | Rutas, datagramas, threads | CSV final + resumen | Usa lectores comunes, executor y writer. |
| `RouteProcessingTask` | Procesa una ruta en un `Callable`. | `routeId`, puntos de esa ruta | Resultados ruta/mes parciales | Ordena por bus/timestamp y usa `SpeedSegmentCalculator`. |
| `ExecutorService` | Ejecuta tareas por ruta en paralelo local. | `Callable<List<RouteMonthSpeed>>` | `Future` | Coordinado por `ThreadPoolSpeedCalculator`. |
| Clases comunes V1 | Lectura, limpieza, distancia, velocidad, agregacion y escritura. | CSV y objetos de dominio | Resultados | Reutilizadas sin cambio arquitectonico fuerte. |
| `ThreadPoolRunSummary` | Imprime metricas, threads y tareas. | Conteos y tiempos | Texto consola | Evidencia para experimentos V2. |

Observacion critica: V2 paraleliza despues de leer todos los datagramas limpios en memoria. Por eso mejora MiniPilot, pero no resuelve el `OutOfMemoryError` del dataset Pilot.

### 3.3 V3 - Distribuida Master-Worker

| Componente / archivo / clase | Responsabilidad | Entrada | Salida | Relacion con otros componentes |
|---|---|---|---|---|
| `ExecutionMode` | Enumera modos CLI. | Flags CLI | Modo de ejecucion | Permite seleccionar master, worker, merge, Ice. |
| `CommandLineApp` | Despacha segun modo: ThreadPool, particion, master, worker, scan-worker, merge, Ice. | Args CLI | Exit code / consola | Capa de entrada comun. |
| `DistributedSpeedMaster` | Master file-based: particiona, lanza workers JVM, lee parciales y mezcla. | CSV rutas/datagramas, work-dir | CSV final, particiones, parciales | Usa `DatagramPartitioner`, `WorkerProcessLauncher`, `PartialResultMerger`. |
| `DatagramPartitioner` | Lee el CSV crudo una vez, limpia y escribe particiones por hash `routeId + busId`. | Datagrams, rutas activas | `partition-xxxxx.csv`, `PartitioningSummary` | Evita partir una misma ruta/bus en distintos workers. |
| `PartitionKey` | Calcula id de particion estable. | `routeId`, `busId`, `partitionCount` | Entero de particion | Usado por particionador y scan-worker. |
| `PartitionManifestCsv` | Escribe/lee manifest de particiones. | Work items | `manifest.csv` | Evidencia de work units. |
| `WorkerProcessLauncher` | Lanza JVMs worker por proceso local. | `PartitionWorkItem` | Parcial CSV o error | Ejecuta `Main --distributed-worker`. |
| `WorkerProcessor` | Procesa una particion limpia. | `partition-xxxxx.csv` | `partial-xxxxx.csv` | Reusa `SpeedSegmentCalculator` y `PartialRouteMonthAggregator`. |
| `PartialRouteMonthAggregator` | Crea agregados parciales mergeables. | Segmentos | `PartialRouteMonthAggregate` | Conserva totales para mezcla posterior. |
| `PartialResultCsv` | Lee/escribe parciales. | Agregados parciales | CSV parcial | Interfaz entre worker y master. |
| `PartialResultMerger` | Mezcla parciales por ruta/mes y completa rutas/meses faltantes. | Parciales + rutas + meses | `RouteMonthSpeed` final | Alimenta `ResultCsvWriter`. |
| `DistributedPartialMerger` | Modo de merge para parciales remotos copiados al master. | Carpeta de parciales | CSV final | Apoya flujo multi-PC manual. |
| `IceDistributedSpeedMaster` | Master Ice: particiona y envia contenido de particion a workers remotos. | CSV crudo, endpoints Ice | Parciales, metricas, CSV final | Usa ZeroC Ice `ObjectPrx.ice_invoke`. |
| `IceScanWorkerServer` / `IceScanWorkerServant` | Servidor worker Ice persistente. | RPC Ice | Respuesta con parcial CSV | Implementa operaciones `processPartitionCsv` y `processScanPartition`. |
| `IceInvocationCodec` / request-response Ice | Codifica/decodifica payloads RPC. | Strings serializados | Requests/responses | Interfaz interna de comunicacion Ice. |

## 4. Interfaces internas y externas

### 4.1 Interfaces externas comunes

Entradas CSV:

- Rutas activas: `lines-241-ActiveGT.csv`.
- Datagramas GPS: `datagrams-MiniPilot.csv` o `datagrams4Pilot.csv`.
- Mapping real usado en scripts: `LINEID`, `route-index 7`, `bus-index 11`, `timestamp-index 10`, `latitude-index 4`, `longitude-index 5`, `coordinate-scale 10000000`.

Salida final:

```text
route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,avg_segment_speed_kmh,valid_segments,buses_observed
```

El writer usa orden estable y formato decimal con `Locale.ROOT`.

### 4.2 Interfaces internas V1

Diagrama textual:

```text
Args CLI -> CliParser -> CommandLineApp -> MonolithicSpeedCalculator
lines CSV -> ActiveRoutesCsvReader -> activeRoutes
datagrams CSV -> GpsDatagramCsvReader -> DataCleaner -> List<GpsPoint>
List<GpsPoint> -> sort(route,bus,timestamp) -> SpeedSegmentCalculator
SpeedSegment -> RouteMonthAggregator -> ResultCsvWriter -> CSV final
```

Datos pasados entre componentes:

- `Set<String>` de rutas activas.
- `GpsDatagramReadResult` con conteos y `List<GpsPoint>`.
- `List<SpeedSegment>`.
- `List<RouteMonthSpeed>`.

### 4.3 Interfaces internas V2

Diagrama textual:

```text
CSV rutas/datagramas -> Readers/Cleaner -> List<GpsPoint> completa
List<GpsPoint> -> groupingBy(routeId)
ThreadPoolSpeedCalculator -> ExecutorService fixed pool
RouteProcessingTask(route) -> SpeedSegmentCalculator -> RouteMonthAggregator
Futures -> lista parcial RouteMonthSpeed -> completeActiveRouteMonths -> ResultCsvWriter
```

Division del trabajo:

- La unidad de trabajo es una ruta activa.
- Cada `RouteProcessingTask` recibe los puntos de una ruta.
- El pool se configura con `--threads`.
- No hay procesos, sockets ni red; todo ocurre dentro del mismo JVM.

### 4.4 Interfaces internas y externas V3

Diagrama file-based:

```text
Coordinator/Master -> ActiveRoutesCsvReader
CSV datagrams -> DatagramPartitioner -> results/.../partitions/partition-xxxxx.csv
manifest.csv -> WorkerProcessLauncher -> JVM worker --distributed-worker
WorkerProcessor -> partial-results/partial-xxxxx.csv
PartialResultMerger -> ResultCsvWriter -> CSV resultado
```

Diagrama Ice:

```text
Master Ice -> DatagramPartitioner -> particiones locales
Master Ice -> Ice endpoint worker 1 / worker 2 / worker N
processPartitionCsv(partitionCsv, thresholds) -> Ice worker
Ice worker -> WorkerProcessor -> partialCsv
Master Ice -> partial-results -> PartialResultMerger -> CSV resultado
```

Mecanismos de comunicacion:

- V3 local/file-based: archivos de particion, archivos parciales y procesos JVM lanzados con `ProcessBuilder`.
- V3 Ice: RPC ZeroC Ice; el master envia el contenido CSV de cada particion como payload y recibe un CSV parcial como respuesta.
- V3 scan-worker: modo alternativo donde cada worker escanea el CSV crudo y selecciona una particion por hash; util para ejecuciones multi-PC manuales, pero el resultado mas fuerte documentado es el Ice full-pilot.

## 5. Flujo de datos por version

### Flujo V1

1. `CommandLineApp` parsea argumentos obligatorios `--lines`, `--datagrams`, `--output`.
2. `ActiveRoutesCsvReader` abre `lines-241-ActiveGT.csv`, resuelve la columna activa y produce un set de rutas.
3. `GpsDatagramCsvReader` abre el CSV de datagramas, con header o indices configurados.
4. `DataCleaner` descarta registros sin ruta, bus, timestamp, latitud o longitud; descarta rutas inactivas; valida escala de coordenadas; elimina coordenadas `-1`; y valida area de operacion.
5. El resultado queda como `List<GpsPoint>` completa en memoria.
6. `MonolithicSpeedCalculator` ordena por `routeId`, `busId` y `timestamp`.
7. `SpeedSegmentCalculator` compara puntos consecutivos del mismo bus/ruta, calcula delta temporal, distancia Haversine y velocidad.
8. Se descartan segmentos con delta cero/negativo, gap superior al umbral o velocidad implausible.
9. `RouteMonthAggregator` agrupa por ruta y mes.
10. `completeActiveRouteMonths` completa combinaciones ruta/mes sin datos con ceros.
11. `ResultCsvWriter` escribe CSV final.

Puntos de falla:

- Cargar todos los datagramas limpios en memoria.
- CSV sin indices correctos.
- Columnas faltantes.
- Dataset grande provoca `OutOfMemoryError`.

### Flujo V2

1. El CLI agrega `--threads`.
2. La lectura de rutas y datagramas ocurre igual que en V1.
3. `ThreadPoolSpeedCalculator` conserva todos los datagramas limpios en memoria.
4. Los puntos se agrupan por `routeId`.
5. Se crea un `ExecutorService` fijo.
6. Por cada ruta activa se crea un `RouteProcessingTask`.
7. Cada task ordena puntos de esa ruta por bus y timestamp.
8. Cada task calcula segmentos validos y agrega por mes.
9. El master local espera `Future.get()`.
10. Se consolidan resultados y se completa la matriz ruta/mes.
11. `ResultCsvWriter` escribe el CSV final.

Puntos de falla:

- Si un worker falla, `ExecutionException` se envuelve en `IllegalStateException`.
- El modelo sigue cargando todo el CSV antes de paralelizar.
- En Pilot grande falla por heap igual que V1.

### Flujo V3

1. El CLI selecciona un modo distribuido.
2. El master lee rutas activas.
3. `DatagramPartitioner` lee el CSV crudo una vez, limpia cada fila y asigna particion con hash de `routeId + busId`.
4. Se escriben archivos `partition-xxxxx.csv` bajo `work-dir/partitions`.
5. En modo local/file-based, `DistributedSpeedMaster` lanza workers JVM con `WorkerProcessLauncher`.
6. Cada `WorkerProcessor` lee una particion limpia, ordena por ruta/bus/timestamp, calcula segmentos y escribe `partial-xxxxx.csv`.
7. El master valida que existan parciales.
8. `PartialResultMerger` suma agregados parciales por ruta/mes.
9. `ResultCsvWriter` produce el CSV final.
10. En modo Ice, `IceDistributedSpeedMaster` tambien particiona, pero envia el contenido de cada particion a un worker remoto por `processPartitionCsv`.
11. El worker Ice escribe temporalmente la particion recibida, procesa y retorna el CSV parcial.

Diferencias clave:

- V1/V2 cargan todos los datagramas limpios en memoria.
- V2 cambia el calculo por ruta a threads locales, no el modelo de memoria.
- V3 cambia el modelo de datos a particiones acotadas y merge de parciales.
- Ice prueba despliegue multi-PC real, con overhead de red y serializacion.

## 6. Patrones y estilos arquitectonicos

| Version | Patron / estilo | Problema que resuelve | Driver satisfecho | Trade-off | Evidencia en el codigo | Alternativa no escogida y razon |
|---|---|---|---|---|---|---|
| V1 | Monolito batch secuencial / pipeline simple por capas | Tener una linea base funcional reproducible | Correctness, Modifiability inicial | Simple, pero no escala en memoria | `MonolithicSpeedCalculator` coordina reader, cleaner, calculator, aggregator y writer en un solo JVM | Streaming externo o particionamiento; no correspondia a V1 por alcance monolitico |
| V2 | Thread Pool local / tareas por ruta | Reducir tiempo de calculo en MiniPilot usando CPU local | Performance | Mayor complejidad concurrente, sin resolver memoria | `Executors.newFixedThreadPool`, `RouteProcessingTask implements Callable` | ForkJoin podria usarse, pero las tareas por ruta son explicitas y suficientes |
| V3 local | Master-Worker file-based / distributed batch processing local | Procesar work units acotados y mergear resultados | Scalability, Performance | Costo alto de particionamiento e I/O | `DistributedSpeedMaster`, `DatagramPartitioner`, `WorkerProcessLauncher`, `PartialResultMerger` | Producer-consumer puro no resolveria despliegue distribuido ni parciales persistentes |
| V3 Ice | Master-Worker distribuido con RPC / Scatter-Gather | Coordinar workers remotos reales | Scalability, deployment distribuido | Overhead de red, serializacion y configuracion de endpoints | `IceDistributedSpeedMaster`, `IceScanWorkerServant`, `--ice-master`, `--ice-worker-server` | REST o colas; agregarian infraestructura no requerida y mas dependencias |

No hay evidencia de brokers, microservicios, REST, base de datos ni colas. La documentacion debe evitar presentar V3 como microservicios; el patron soportado por codigo es Master-Worker.

## 7. Relacion con drivers de arquitectura

| Driver | Evidencia en V1 | Evidencia en V2 | Evidencia en V3 | Version que mejor lo satisface | Limitaciones |
|---|---|---|---|---|---|
| Performance | MiniPilot limpio en 64,081 ms | 39,201 ms con 4 threads; 38,643 ms con 12 threads | 30,814 ms local; Ice MiniPilot 53,780 ms | V3 local en MiniPilot; V2 como opcion simple local | Mediciones no son benchmark controlado; hosts y condiciones pueden variar |
| Scalability | Falla en 67 GB por `OutOfMemoryError` | Falla en 67 GB por `OutOfMemoryError` | Completa 67 GB con 64 particiones local y 1024 particiones Ice | V3 | Particionamiento domina runtime; requiere disco y configuracion |
| Correctness | Tests de parser, cleaner, Haversine, agregacion, writer; salida MiniPilot valida | Reutiliza dominio; mismos conteos limpios en evidencia V2 | Tests distribuidos y validacion sin NaN/Infinity/null | Compartido; V3 conserva logica comun | Falta registrar diff bit a bit formal entre outputs |
| Modifiability / Maintainability | Separacion por paquetes `cli`, `csv`, `geo`, `service`, `output` | Reutiliza componentes y encapsula concurrencia en `service` | Infraestructura distribuida aislada en `distributed` | V3 por separacion dominio/infraestructura | Hay duplicacion de completado ruta/mes entre calculadoras |
| Availability / Reliability | Manejo basico de errores CLI | Captura interrupciones y fallas de futures | Detecta parciales faltantes y errores de worker/Ice | V3 parcialmente | No hay reintentos, health checks ni recuperacion automatica de workers |

Experimentos recomendados para validar mejor:

- Diff formal V1/V2/V3 sobre MiniPilot.
- Medicion de memoria por fase.
- Prueba de caida de worker Ice y comportamiento del master.
- Repeticiones estadisticas por configuracion de threads/workers.
- Comparacion de particiones 64, 128, 512, 1024 con dataset grande.

## 8. Relacion deployment-codigo

### Deployment V1

- Nodo: una maquina / un JVM.
- Componentes desplegados: `Main`, `CommandLineApp`, `MonolithicSpeedCalculator`, readers, cleaner, calculators, aggregator, writer.
- Entradas: `lines-241-ActiveGT.csv`, `datagrams-MiniPilot.csv` o `datagrams4Pilot.csv`.
- Salidas: CSV final en `results/`.
- Responsabilidades: leer todo, limpiar, ordenar, calcular, agrupar y escribir.
- Riesgos: heap insuficiente para Pilot; no hay distribucion ni paralelismo.

### Deployment V2

- Nodo: una maquina / un JVM.
- Componentes desplegados: `ThreadPoolSpeedCalculator`, `RouteProcessingTask`, executor local y componentes comunes.
- Entradas: mismas de V1.
- Salidas: CSV final en `results/`.
- Responsabilidades: leer todo, agrupar por ruta, ejecutar rutas en paralelo local, consolidar y escribir.
- Riesgos: contencion por memoria; el dataset completo sigue en heap; velocidad se satura a partir de varios threads.

### Deployment V3

- Nodo coordinador: PC master que ejecuta `DistributedSpeedMaster` o `IceDistributedSpeedMaster`.
- Nodos worker: procesos JVM locales en modo file-based, o PCs remotos con `IceScanWorkerServer`.
- Componentes desplegados en master: `DatagramPartitioner`, `PartitionManifestCsv`, `WorkerProcessLauncher` o cliente Ice, `PartialResultMerger`, `ResultCsvWriter`.
- Componentes desplegados en worker: `WorkerProcessor`; en Ice tambien `IceScanWorkerServant`.
- Entradas: master usa rutas activas y datagramas completos; workers file-based usan particiones; workers Ice reciben particion por RPC.
- Salidas: particiones, parciales, metricas, CSV final.
- Comunicacion: archivos/procesos en V3 local; RPC ZeroC Ice en V3 multi-PC.
- Responsabilidades: master divide y mezcla; workers calculan parciales.
- Riesgos: disco alto para particiones, overhead RPC en Ice, endpoint/puerto incorrecto, falta de reintentos.

## 9. Matriz de trazabilidad contra rubrica

| Criterio de rubrica | Evidencia encontrada en el repo | Archivo / carpeta / rama | Que falta o que se debe explicar mejor en el PDF |
|---|---|---|---|
| Drivers de arquitectura | Resultados de MiniPilot/Pilot, OOM en V1/V2, exito V3 | `docs/experiment-results.md`, `docs/v*-evidence`, `docs/version-3-*.md` | Conectar cada driver con una decision concreta de codigo y no solo con tiempos |
| Aplicacion y justificacion de patrones | V1 pipeline monolitico, V2 Thread Pool, V3 Master-Worker/Ice | `service/*`, `distributed/*`, `CliParser` | Evitar llamar a V3 microservicios; justificar Master-Worker con particiones y merge |
| Diseno global e integracion | Paquetes separados por CLI, CSV, geo, service, output, distributed | `src/main/java/edu/icesi/sitmmio` | Incluir diagrama de componentes con clases reales |
| Implementacion, despliegue y validacion | Scripts remotos, Ice deployment, Gradle tests | `scripts/*.sh`, `docs/version-3-ice-deployment.md`, `src/test` | Explicar diferencia entre V3 local file-based y V3 Ice multi-PC |
| Documento de resultados | Analisis V3 MiniPilot/Pilot e Ice | `docs/version-3-experiment-analysis.md`, `docs/version-3-minipilot-experiment-analysis.md` | Corregir inconsistencias de numeros y agregar scripts/graficas reproducibles |

## 10. Problemas, inconsistencias o huecos detectados

| Prioridad | Problema | Evidencia | Impacto en la rubrica | Recomendacion |
|---:|---|---|---|---|
| Alta | Ramas locales desactualizadas frente a remotas | `main` local en `122cbe7`, `origin/main` en `082f88a`; `version-2-threadpool` local en `20278b0`, `origin/version-2-threadpool` en `4f07da1` | Puede citarse una version equivocada | En el PDF indicar rama y commit exactos usados |
| Alta | V1/V2 fallan en Pilot por el mismo motivo: memoria | Evidencia `v1_70gb_attempt.txt`, `v2_70gb_threads8_attempt.txt` | Clave para justificar V3 | Explicar que V2 mejora CPU pero no cambia estrategia de memoria |
| Alta | No hay diff formal registrado de salidas entre versiones | Existe `scripts/compare-v2-v3-minipilot.sh`, pero no se ve resultado consolidado en docs | Correctness queda parcialmente demostrado | Guardar salida del diff o checksum de CSVs equivalentes |
| Media | `docs/experiment-results.md` conserva una metrica V1 antigua de 29,716 ms | El archivo menciona limpieza con 0 rechazados; evidencia limpia posterior tiene 64,081 ms y 248,727 rechazados | Puede confundir graficas y conclusiones | Usar la evidencia limpia comun para V1/V2/V3 |
| Media | Documentacion Ice tiene una frase contradictoria sobre workers leyendo copia local del datagrama | `docs/version-3-ice-deployment.md` dice inicialmente que worker lee copia local, pero luego aclara que solo master necesita datos | Deployment puede quedar mal explicado | Corregir: en flujo Ice final, workers reciben particiones por RPC |
| Media | V3 local y V3 Ice se mezclan facilmente bajo el nombre "distribuida" | Docs reportan 4 workers/8 particiones, 2 workers/2 particiones, 64 y 1024 particiones | Riesgo de comparar tiempos injustamente | Separar siempre V3 file-based y V3 Ice |
| Media | `run-ice-master-remote.sh` default `PARTITIONS=2`, pero Pilot exitoso usa 1024 | Script vs docs full pilot | Puede fallar si se ejecuta Pilot con defaults | Documentar presets por dataset o crear scripts MiniPilot/Pilot separados |
| Media | Evidencias V1/V2 estan en `.txt` que Git reporta como binarios | `git show --stat` muestra `Bin` | Dificulta revision y trazabilidad | Reexportar evidencia en UTF-8 Markdown o CSV |
| Baja | Duplicacion de logica para completar rutas/meses | `MonolithicSpeedCalculator`, `ThreadPoolSpeedCalculator`, `PartialResultMerger` | Mantenibilidad | Extraer helper comun si se modifica codigo en una fase futura |
| Baja | Falta medicion de tolerancia a fallos | No hay test tumbando workers | Availability/Reliability debil | Agregar experimento de worker caido y registrar comportamiento |

## 11. Recomendaciones concretas para mejorar los PDFs

1. Incluir una tabla inicial con rama y commit exacto de cada version.
2. Separar V3 en dos mediciones: V3 local/file-based y V3 Ice multi-PC.
3. Explicar que Ice no necesariamente acelera MiniPilot; su valor es demostrar distribucion real.
4. Usar la metrica V1 limpia de 64,081 ms cuando se compare contra V2/V3 con limpieza de coordenadas.
5. Mostrar que V2 falla en Pilot porque el cuello de botella es memoria, no falta de threads.
6. Incluir diagramas de flujo basados en clases reales, no diagramas genericos.
7. Agregar matriz de drivers con evidencia de codigo y evidencia experimental.
8. Documentar el particionamiento `routeId + busId` como decision central de correctness.
9. Agregar una tabla de riesgos: heap, disco, red Ice, particiones, endpoints y falta de reintentos.
10. Registrar un diff/checksum de salidas para reforzar correctitud.
11. Convertir evidencias binarias a Markdown/CSV UTF-8.
12. Corregir cualquier frase que sugiera brokers, microservicios o REST si el codigo no los implementa.

## 12. Sintesis final

La arquitectura evoluciona de forma coherente:

```text
V1: CSV -> Reader/Cleaner -> Calculator -> Aggregator -> Writer
V2: CSV -> Reader/Cleaner -> groupBy(route) -> ThreadPool tasks -> Writer
V3: CSV -> Partitioner(routeId+busId) -> Workers -> Partial CSVs -> Merger -> Writer
V3 Ice: Master -> partition CSV payloads over Ice -> Remote workers -> partial CSV payloads -> Merger
```

La narrativa academica mas defendible es:

- V1 valida el dominio y la salida deterministica.
- V2 valida mejora de performance local con Thread Pool, pero tambien demuestra que concurrencia no equivale a escalabilidad de memoria.
- V3 introduce el cambio arquitectonico decisivo: particionamiento y Master-Worker.
- V3 Ice demuestra despliegue distribuido real, con el trade-off esperado de comunicacion y serializacion.

