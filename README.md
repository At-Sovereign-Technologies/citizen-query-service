# citizen-query-service

## 1. Descripción

El Citizen Query Service es un microservicio de solo lectura encargado
de exponer información pública del ciudadano, como el puesto de
votación, estado del voto y si posee multas.

Forma parte del lado de consulta bajo el enfoque CQRS e implementa una
capa de cache con Redis, incluyendo mecanismos de resiliencia para
tolerar fallos del servicio de cache.

------------------------------------------------------------------------

## 2. Tecnologías

-   Java 21
-   Spring Boot 3.x
-   Spring Web
-   Spring Data JPA
-   PostgreSQL
-   Redis
-   Resilience4j (Circuit Breaker)
-   Flyway
-   Springdoc OpenAPI (Swagger)
-   Maven

------------------------------------------------------------------------

## 3. Arquitectura

Arquitectura por capas:

-   Controller: Exposición de endpoints REST
-   Service: Lógica de negocio y orquestación
-   Repository: Acceso a datos con JPA
-   Cache Adapter: Integración con Redis
-   Circuit Breaker: Manejo de fallos en cache
-   Mapper: Transformación de entidades a DTOs
-   Exception Layer: Manejo global de errores

------------------------------------------------------------------------

## 4. Estrategia de Cache

Se implementa el patrón cache-aside con resiliencia:

1.  Se intenta obtener la información desde Redis
2.  Si falla o no existe, se consulta la base de datos
3.  Se intenta almacenar en cache
4.  En caso de fallo de Redis, el sistema continúa funcionando usando DB

------------------------------------------------------------------------

## 5. Resiliencia (Circuit Breaker)

Se implementa Circuit Breaker con Resilience4j:

-   Detecta fallos en Redis
-   Evita llamadas repetidas a un servicio caído
-   Permite fallback automático hacia base de datos
-   Mejora la latencia en escenarios de fallo

Estados:

-   CLOSED → funcionamiento normal
-   OPEN → Redis deshabilitado temporalmente
-   HALF-OPEN → prueba de recuperación

------------------------------------------------------------------------

## 6. Versionamiento de API

/api/v1/\*

------------------------------------------------------------------------

## 7. Variables de entorno

DB_URL=jdbc:postgresql://localhost:5432/citizen_db\
DB_USER=citizen_user\
DB_PASSWORD=123456

REDIS_HOST=localhost\
REDIS_PORT=6379

PORT=8081

------------------------------------------------------------------------

## 8. Base de datos

CREATE DATABASE citizen_db;\
CREATE USER citizen_user WITH PASSWORD '123456';\
GRANT ALL PRIVILEGES ON DATABASE citizen_db TO citizen_user;

`\c c`{=tex}itizen_db\
GRANT ALL ON SCHEMA public TO citizen_user;

------------------------------------------------------------------------

## 9. Migraciones (Flyway)

Ubicación:

src/main/resources/db/migration

Ejemplo:

V1\_\_init.sql\
V2\_\_seed.sql\
V3\_\_add_has_fines.sql

------------------------------------------------------------------------

## 10. Redis

sudo systemctl start redis-server\
redis-cli ping

Respuesta esperada: PONG

------------------------------------------------------------------------

## 11. Ejecución

export \$(grep -v '\^#' .env \| xargs)\
mvn spring-boot:run

------------------------------------------------------------------------

## 12. Swagger

http://localhost:8081/swagger-ui.html

------------------------------------------------------------------------

## 13. Endpoint

GET /api/v1/citizen/polling-station?document=1001

------------------------------------------------------------------------

## 14. Respuesta

{ "document": "1001", "pollingStation": "Mesa 01 - Bogotá", "status":
"NOT_VOTED", "hasFines": true }

------------------------------------------------------------------------

## 15. Observabilidad

Logging estructurado:

-   CACHE HIT
-   CACHE MISS
-   CACHE STORE
-   CACHE FALLBACK
-   Circuit Breaker events (OPEN, CLOSED, HALF-OPEN)

------------------------------------------------------------------------

## 16. Estado

Microservicio funcional, resiliente y listo para integración:

-   API REST operativa
-   PostgreSQL integrado
-   Redis con tolerancia a fallos
-   Circuit Breaker activo
-   Migraciones controladas con Flyway
-   Campo adicional hasFines implementado
-   Documentación Swagger
