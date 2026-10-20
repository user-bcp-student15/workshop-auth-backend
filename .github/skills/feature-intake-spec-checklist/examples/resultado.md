# Summary
- Permitir que el usuario revise un resumen y confirme una transferencia antes de enviarla, para reducir las quejas por transferencias duplicadas en un 20% durante un mes.

# Scope
- **Dentro del alcance**
  - Confirmación previa al envío.
  - Presentación de un resumen del mensaje de transferencia.
  - Validación de datos.
- **Fuera del alcance**
  - Cambios de infraestructura.
  - Rediseño completo de la interfaz.

# Supuestos
- No se declaran supuestos adicionales; los datos del resumen y las reglas de validación están pendientes de definición.

# Decisiones técnicas por tomar (pendientes o resueltas)
- Definir los campos que mostrará el resumen previo a la confirmación.
- Acordar cómo se integrará la confirmación con el API REST existente y los servicios de cuentas y transferencias.
- Definir las reglas de validación de importes y los mensajes para datos inválidos.
- **Resueltas:** respetar el API REST existente y el registro estandarizado; no se realizarán cambios de infraestructura ni un rediseño completo.

# Riesgos y mitigaciones
- **Exposición de datos sensibles en logs:** revisar los registros y evitar que incluyan datos sensibles de la transferencia.
- **Errores en la validación de importes:** definir las reglas antes de implementar y cubrir los casos válidos e inválidos con pruebas.

# Criterios de aceptación (medibles)
- El 100% de las transferencias se presenta para confirmación antes de enviarse.
- El 100% de las transferencias requiere una confirmación explícita para continuar; cancelar o no confirmar no envía la transferencia.
- La validación rechaza todos los importes que incumplan las reglas acordadas y permite los que las cumplen.
- En el mes posterior al lanzamiento, las quejas por transferencias duplicadas disminuyen al menos un 20% respecto del mes de referencia acordado.

# Plan de validación
- **Pruebas unitarias**
  - Verificar que la transferencia no se envíe sin confirmación explícita.
  - Verificar que cancelar la confirmación no envíe la transferencia.
  - Cubrir importes válidos e inválidos según las reglas acordadas.
- **Pruebas funcionales**
  - Verificar el flujo de resumen, confirmación y envío mediante los servicios de cuentas y transferencias.
  - Comprobar que la confirmación no duplica el envío ante interacciones repetidas.
- **Seguridad (comprobaciones mínimas)**
  - Revisar que los registros estandarizados no expongan datos sensibles.
  - Comprobar que los datos de transferencia se validen antes del envío.
- **Evidencia requerida (qué adjuntar)**
  - Salida de pruebas unitarias y funcionales.
  - Checklist de revisión de registros sin datos sensibles.
  - Documentación breve actualizada y PR revisado.

# Tareas sugeridas por rol
- **Chapter/PO**
  - Confirmar la métrica base y el método para contar quejas por duplicados.
  - Definir los campos del resumen previo al envío.
  - Acordar los mensajes de confirmación y cancelación.
- **Tech Lead**
  - Definir el contrato entre el frontend, el API REST y los servicios dependientes.
  - Acordar las reglas de validación de importes y el manejo de errores.
  - Revisar el diseño frente a duplicados y exposición de datos en registros.
- **Backend**
  - Implementar la validación de datos acordada antes de enviar la transferencia.
  - Integrar el flujo con los servicios de cuentas y transferencias.
  - Añadir pruebas unitarias y verificar que los registros no expongan datos sensibles.
- **Frontend**
  - Implementar la presentación del resumen y las acciones de confirmar o cancelar.
  - Evitar enviar la transferencia antes de la confirmación explícita.
- **QA**
  - Preparar pruebas funcionales para confirmar, cancelar y enviar.
  - Verificar los casos válidos e inválidos de importe según las reglas acordadas.
  - Adjuntar resultados de pruebas y evidencia de la revisión de registros.
- **Cyber**
  - Revisar los registros para detectar exposición de datos sensibles.
  - Revisar las comprobaciones de validación antes del envío.

# Preguntas abiertas
- ¿Qué campos debe incluir el resumen previo a la confirmación?
- ¿Cuáles son las reglas exactas para validar importes?
- ¿Cuál será el período y la fuente de referencia para medir la reducción del 20% en quejas?
- ¿Qué comportamiento se espera ante una respuesta fallida o tardía de los servicios de cuentas o transferencias?
