# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

```bash
mvn quarkus:dev
```

Servicio: `http://localhost:8080`

El servicio requiere `AUTH_TOKEN_SECRET` con al menos 32 bytes UTF-8. Configure el mismo secreto en el consumidor de la API de requerimientos. El backend emite tokens JWT HS256 con duración de 3600 segundos; el formato legacy Base64 ya no se acepta como credencial.

Para ejecución local, configure un secreto de laboratorio mediante la variable de entorno antes de iniciar Quarkus. No reutilice el secreto de pruebas ni publique el secreto en el repositorio.

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Propósito del laboratorio

El sistema conserva deuda técnica fuera del alcance de este cambio, incluidas contraseñas en texto plano, CORS permisivo y manejo genérico de excepciones. Mantenerlo en ambientes aislados y consultar `RETO.md` antes de cualquier despliegue.
