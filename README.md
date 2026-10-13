# CarePlus Maven Starter

Multi-module Spring Boot starter with UI forms and placeholder APIs. Business logic and persistence are intentionally left for Tamal.

## Modules
- `careplus-facade` — port 8080; serves UI and future service routing; no database.
- `patient-service` — port 8081; patient API; configure its own database later.
- `doctor-service` — port 8082; doctor API; configure its own database later.
- `appointment-service` — port 8083; appointment API; configure its own database later.

The databases are not configured yet. When added, each service should own its database/schema. The facade should call service APIs, not access their databases directly.

## Build/run
Requires Java 17+ and Maven 3.9+.
`mvn clean package`

Run each in separate terminals:
`mvn -pl careplus-facade spring-boot:run`
`mvn -pl patient-service spring-boot:run`
`mvn -pl doctor-service spring-boot:run`
`mvn -pl appointment-service spring-boot:run`

Open http://localhost:8080/. The form buttons are not wired to APIs yet. Placeholder APIs return text only; they do not save data.
"# careplus-healthcare-api" 
"# careplus-healthcare-api" 
