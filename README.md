# Volk REST

The reactive REST API of the **Volk eDiscovery platform**. Serves the users and file
metadata that [volk-sniffer](https://github.com/armper/volk-sniffer) ingests into MongoDB,
and is consumed by the [volk-ui](https://github.com/armper/volk-ui) Angular front end.

```
directories ──> volk-sniffer ──> MongoDB <── volk-rest <── volk-ui (Angular)
                (Camel + POI)     (volk db)   (WebFlux, :8091)   (:4200)
```

## Tech stack

- Java 17, Spring Boot 3.5
- Spring WebFlux (Reactor) + Spring Data MongoDB Reactive
- Spring Boot Actuator

## API

Base URL: `http://localhost:8091`

### Users — `/searchuser`

| Method | Path | Description |
| --- | --- | --- |
| GET | `/searchuser/?name={prefix}` | Search users whose name starts with `prefix` (case-insensitive) |
| GET | `/searchuser/findall` | List all users |
| GET | `/searchuser/{id}` | Fetch one user (includes embedded `searchFiles`) |
| POST | `/searchuser` | Create a user, or append the posted files to an existing user with the same name + domain |
| PUT | `/searchuser` | Save the posted user's files and update the user |

### Files — `/searchfile`

| Method | Path | Description |
| --- | --- | --- |
| GET | `/searchfile/{id}` | Fetch one file |
| GET | `/searchfile/findall` | List all files |
| POST | `/searchfile` | Save a file |

Health check: `GET /actuator/health`.

CORS is allowed for `http://localhost:4200` by default
(override with `volk.cors.allowed-origins`).

### Example

```bash
curl "http://localhost:8091/searchuser/?name=alp"
```

```json
[{
  "id": "6a2b0985e258e165cb1a6b5e",
  "name": "alperea",
  "domainName": "",
  "searchFiles": [{
    "fileName": "test-document.docx",
    "path": "/tmp/volk-inbox/test-document.docx",
    "extension": "docx",
    "size": 2304,
    "server": "/tmp/volk-inbox"
  }]
}]
```

## Running

Requires Java 17+ and MongoDB on `localhost:27017`:

```bash
docker compose up -d          # starts MongoDB 7
./mvnw spring-boot:run        # API on :8091
```

## Tests

```bash
docker compose up -d          # tests hit a real MongoDB
./mvnw test
```

## History

Originally written in 2018 against Spring Boot 2.0. Modernized in 2026: Spring Boot 3.5
(Java 17), fully non-blocking handlers (the old create/update endpoints called `block()`
inside the event loop), JUnit 5 + datafaker tests, and a docker-compose for MongoDB.
