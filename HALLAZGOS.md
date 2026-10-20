# Hallazgos candidatos

> Revisión de descubrimiento de solo lectura. Todos los puntos están pendientes de revisión humana; no se crearon issues.

### DA-01: SQL de autenticación construido con entradas concatenadas
- Estado: CANDIDATO
- Evidencia: `LegacyDatabase.findByCredentials` interpola `username` y `password` dentro del SQL: [LegacyDatabase.java](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java#L31) y [LegacyDatabase.java](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java#L32). La prueba existente cubre el login válido, no entradas con caracteres SQL: [AuthResourceTest.java](src/test/java/com/bcp/workshop/auth/AuthResourceTest.java#L13).
- Impacto: una entrada manipulada podría cambiar la consulta prevista y comprometer el límite de autenticación.
- Prioridad propuesta: P1 si el servicio se expone fuera del laboratorio aislado.
- Contrato potencialmente afectado: `POST /auth/login` y su respuesta de éxito/error.
- Cómo validarlo: parametrizar la consulta y agregar pruebas de login con entradas que contengan comillas, verificando que se traten como valores literales y que no cambie el resultado de autenticación.

### DA-02: Token compartido sin firma criptográfica
- Estado: CANDIDATO
- Evidencia: el backend incluye un secreto constante en el contenido y solo aplica Base64: [LegacyTokenUtil.java](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java#L8), [LegacyTokenUtil.java](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java#L14) y [LegacyTokenUtil.java](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java#L15). La API consumidora decodifica ese contenido y compara la constante compartida, sin verificar una firma, en [main.py](../workshop-requirements-api/app/main.py#L20) y [main.py](../workshop-requirements-api/app/main.py#L22).
- Impacto: la identidad y el rol incluidos en el token no tienen una integridad criptográfica demostrada; el contrato entre emisor y consumidor puede aceptar claims alterados.
- Prioridad propuesta: P1 si los servicios se exponen fuera del laboratorio aislado.
- Contrato potencialmente afectado: token emitido por `POST /auth/login` y consumido como `Authorization: Bearer` por la API de requerimientos.
- Cómo validarlo: agregar pruebas de integración que modifiquen claims de un token emitido y exijan rechazo; validar además firma y expiración con el mecanismo acordado.