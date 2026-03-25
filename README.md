# Property Evaluation Search

`property-evaluation-search` is a Spring Boot service for managing property/evaluation data in MariaDB and searching indexed property content with Apache Lucene.  
The project is designed to compare search quality between Lucene and MariaDB Full-Text Search, especially for advanced features like synonyms, fuzzy matching, and field boosting.

## Prerequisites

- Java `21`
- Maven `3.9+` (optional, for local non-Docker runs; Docker build handles Maven internally)
- Docker + Docker Compose
- MariaDB `11.0.6` (provided by `docker-compose.yml`)

## Configuration (`.env`)

1. Copy `.env.example` to `.env` (if not already present).
2. Update values as needed.

Example:

```env
DB_URL=jdbc:mariadb://localhost:3306/property_search
DB_USER=admin
DB_PASSWORD=admin123
LUCENE_INDEX_PATH=C:/tmp/property-search/lucene-index
```

Notes:
- Spring picks these values from `application.yml`.
- Lucene index path defaults to `./index/lucene/${spring.application.name}` unless `LUCENE_INDEX_PATH` is overridden (for example via `.env`).

## Start MariaDB with Docker Compose

From project root:

```bash
docker compose up -d
```

Stop and remove containers + volumes:

```bash
docker compose down -v
```

## Run the Application

Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.config.import=optional:file:.env[.properties]"
```

macOS/Linux:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.config.import=optional:file:.env[.properties]"
```

Liquibase migrations run automatically on startup.

## Run App in Docker

Use Docker Compose only for MariaDB, and run the app as a separate container.

No separate `mvn install` step is required here. The Docker multi-stage build runs Maven during image build.

1. Start MariaDB:
```bash
docker compose up -d
```

2. Build the app image:
```bash
docker build -t property-evaluation-search-api:local .
```
Side note: if you suspect Docker layer cache is serving stale app artifacts (for example Swagger/OpenAPI changes not reflected), rebuild once with:
```bash
docker build --no-cache -t property-evaluation-search-api:local .
```

3. Run the app container (Bash):
```bash
docker run --name property-search-app --rm \
  --env-file .env \
  --add-host=host.docker.internal:host-gateway \
  -e DB_URL=jdbc:mariadb://host.docker.internal:3306/property_search \
  -e LUCENE_INDEX_PATH=/var/lib/property-search/lucene-indexes/property-evaluation \
  -p 8080:8080 \
  -v property_search_lucene_index:/var/lib/property-search/lucene-indexes \
  property-evaluation-search-api:local
```

Windows PowerShell:
```powershell
docker run --name property-search-app --rm `
  --env-file .env `
  --add-host=host.docker.internal:host-gateway `
  -e DB_URL=jdbc:mariadb://host.docker.internal:3306/property_search `
  -e LUCENE_INDEX_PATH=/var/lib/property-search/lucene-indexes/property-evaluation `
  -p 8080:8080 `
  -v property_search_lucene_index:/var/lib/property-search/lucene-indexes `
  property-evaluation-search-api:local
```

Notes:
- `LUCENE_INDEX_PATH` is overridden in Docker to a container path backed by a Docker volume.
- If you run the app as a local JAR instead, it uses `./index/lucene/...` by default.

## Quick Start (Required)

After the app starts, run these two endpoints before search demos:

1. `POST /api/lucene/test-data/generate?propertyCount=20000&batchSize=200&clearExistingData=true&reindexAfterGeneration=false`
2. `POST /api/lucene/reindex`

This loads benchmark data first and then builds the Lucene index with lower overall load.

## Core Tech Stack

- Java: `21`
- Spring Boot: `4.0.3`
- Apache Lucene: `9.11.1`
- MariaDB: `11.0.6`
- Maven: `3.9+`
- Docker / Docker Compose: latest compatible versions

## Database Migration Summary

At startup, the application runs database migrations that:
- Create core tables for properties and property evaluations
- Apply primary/foreign key constraints
- Apply data validation check constraints (for example numeric and text length guards)
- Create query-friendly indexes on common filter and join columns

## Search Indexes (Short Note)

Lucene index fields used by this project:
- Text search fields: `title`, `description` (analyzed with `StandardTokenizer -> LowerCaseFilter -> SynonymGraphFilter`)
- Exact/filter fields: `propertyType`, `cityFilter`, `postalCodeFilter`
- Numeric range fields: `areaInSquareMeter`, `evaluationMarketValue`

MariaDB search indexes used by this project:
- Full-text index: `ftx_properties_title_description` on `properties(title, description)`

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Lucene Setup Notes

Analyzer and index/storage behavior used by this project:

- Index location: `app.lucene.index-path` (defaults to `target/lucene-index`)
- Directory implementation: Lucene `FSDirectory`
- Analyzer pipeline: `StandardTokenizer` -> `LowerCaseFilter` -> `SynonymGraphFilter`
- Synonym pairs (bidirectional): `flat <-> apartment`, `garage <-> parking`, `metro <-> subway`, `loft <-> studio`

## Test Data Generator

Use `POST /api/lucene/test-data/generate` to generate benchmark data for search comparisons.

What it does:
- Optionally clears existing data first
- Inserts synthetic properties and evaluations in batches
- Adds deterministic comparison fixtures (synonyms/typos/proximity/field-boost scenarios) as part of normal generated data
- Optionally triggers Lucene reindex after generation so search is immediately ready

Useful query params:
- `propertyCount`
- `batchSize`
- `clearExistingData`
- `reindexAfterGeneration`

Recommendation:
- For large data loads, run `POST /api/lucene/test-data/generate` with `reindexAfterGeneration=false`, then call `POST /api/lucene/reindex` separately.
- This reduces indexing overhead during inserts and keeps overall system load lower.

## Postman Collections

Use the all-in-one collection:

- [property-evaluation-search-all-in-one.postman_collection.json](postman-collection/property-evaluation-search-all-in-one.postman_collection.json)
  Includes top-level folders for setup/data, Lucene demo, MariaDB FTS demo, and side-by-side comparison.

## Benchmark (p50/p95)

Use the benchmark script to compare Lucene vs MariaDB FTS latency by scenario.

Script path:
- `benchmarking/run-benchmark.ps1`
- `benchmarking/run-benchmark.sh`

For more details, refer to `benchmarking/benchmarking.docx`.

Default behavior:
- Assumes the app is already running on `http://localhost:8080`
- Assumes data is already loaded and Lucene is already indexed
- Runs warmup + measured iterations and outputs `p50` / `p95` per scenario
- Writes result files in the same folder as the script (`benchmarking/`)
- PowerShell and Bash scripts are equivalent versions of the same benchmark flow

Run benchmark on existing loaded/indexed data:

```powershell
powershell -ExecutionPolicy Bypass -File benchmarking/run-benchmark.ps1 -Iterations 30 -Warmup 5
```

```bash
chmod +x benchmarking/run-benchmark.sh
./benchmarking/run-benchmark.sh --iterations 30 --warmup 5 --base-url http://localhost:8080
```

Output:
- `benchmarking/benchmark-<timestamp>.csv`
- `benchmarking/benchmark-<timestamp>.json`

Scenarios measured:
- `baseline_natural_light - Keyword + City + Prperty Type search`
- `modern_area_berlin - Keyword+ City + Peoperty type + Areaa Range search`
