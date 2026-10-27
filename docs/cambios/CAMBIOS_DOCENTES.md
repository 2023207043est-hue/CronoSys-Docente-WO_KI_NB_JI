# Perfil docente y Espacio Docente

La aplicación ofrece ahora dos espacios seleccionables: **Estudiante** y **Docente**.

- En una instalación nueva, la bienvenida permite escoger el perfil junto con el nombre.
- El perfil elegido queda guardado y Compás abre directamente ese espacio en los siguientes inicios.
- El nombre y el perfil se pueden editar en **Configuración → Tu perfil**.
- Al abrir la aplicación aparece un saludo personalizado y un recuerdo tomado de una nota anterior, una tarea completada o un evento pasado.
- El perfil Estudiante mantiene el Espacio de Estudio existente.
- El perfil Docente muestra el **Espacio Docente**, organizado en pestañas: Clases, Evaluación, Tutorías y Enfoque. En Clases se crean cursos propios, cada uno con paneles de Resumen, Planeación y Estudiantes y notas.
- Cada curso puede registrar estudiantes, correo, calificación y observaciones, además de clases, materiales, actividades y evaluaciones con fecha.
- **Importar Excel** y **Exportar Excel** intercambian la lista y las notas del curso como libros `.xlsx`. La importación reconoce encabezados como Estudiante/Nombre, Correo/Email, Calificación/Nota y Observaciones/Comentarios; al repetir una importación se actualiza por nombre en ese curso.
- Las pestañas utilizan la agenda y las tareas actuales. Las acciones de Time Blocking enlazan las técnicas generales con tres opciones docentes: Preparación de clase 50/10, Corrección 40/10 y Planeación profunda 90/20.
- **Configuración → Restablecer Compás** ofrece una acción protegida por confirmación para borrar registros, perfiles y preferencias, y volver a pedir nombre y perfil.

## Alcance del perfil

Compás es una aplicación local de un solo perfil, sin inicio de sesión con contraseña ni cuentas independientes. La selección inicial define el espacio de trabajo; ambos espacios usan la misma agenda y tareas, y el Espacio Docente añade cursos, listas y calificaciones locales. La integración con Excel lee y escribe archivos `.xlsx`; no controla una sesión abierta de Microsoft Excel ni sincroniza en vivo con OneDrive.

## Ejecutar

El proyecto conserva Maven y Java 25. Desde esta carpeta:

```powershell
mvn javafx:run
```

Los métodos docentes solo se incorporan a la lista cuando el perfil activo es Docente; los métodos estudiantiles y los métodos propios guardados continúan disponibles.

## Versión 1.7 — Asistencia visual, semáforo de riesgo y aula virtual

**Mis cursos** muestra cada curso como una tarjeta (con sus alertas) y, dentro, seis pestañas:

- **Panel**: cifras del curso y tres gráficas que se rehacen al marcar asistencia o ingresar notas
  (asistencia acumulada, asistencia por clase y estudiantes al día vs atrasados), más el **semáforo de alertas**.
- **Asistencia**: tabla con casillas *Presente / Falta / Tardanza / Justificada* por estudiante. La fila se pinta
  de verde, rojo, amarillo o azul. Se guarda al instante en la base local (funciona sin internet y no se pierde al
  cerrar). Atajos: P, F, T, J, Supr y flechas. Botones «Marcar todos presentes», «Sin registro → Falta» y «Limpiar día».
  La **vista mensual** es una matriz estudiantes × fechas con celdas clicables.
- **Trabajos y notas**: una columna por tarea/actividad/evaluación con la entrega (✓ / ⏱ / ✗) y la nota.
- **Estudiantes**: búsqueda, doble clic para editar, situación de cada uno, importar/exportar Excel.
- **Planeación**: clases, materiales, tareas y evaluaciones con fecha; «Agendar» crea el evento en el calendario.
- **Aula virtual**: enlace de la clase en vivo (Meet/Zoom/Teams), tablón de anuncios (copiar para WhatsApp o
  enviar por correo a la lista del curso) y biblioteca de enlaces y archivos.

**Exportar a Excel** genera un libro con colores (registro mensual, día elegido, semáforo de riesgo / trabajos).
**Imprimir** manda la tabla tal como se ve, en horizontal y paginada; con «Microsoft Print to PDF» se guarda como PDF.

**Alertas automáticas** (`RiesgoEstudianteService`): sin que el docente busque uno por uno, se cruzan el historial
de asistencia, las entregas y las notas. Rojo (en riesgo): 3+ faltas consecutivas, asistencia < 70 %, 2+ trabajos
no entregados, o nota baja que coincide con inasistencias. Ámbar (atención): 2 faltas seguidas, asistencia < 85 %,
3+ tardanzas, 1 trabajo no entregado, nota bajo la mínima o caída reciente («dejó de asistir»). Junto al nombre
aparece la bandera y el tooltip explica el motivo exacto. La pestaña **Alertas** reúne a todos los cursos.

Cada curso tiene su **nota mínima aprobatoria** (11 por defecto, escala vigesimal) editable en «Editar curso».

**Datos de prueba**: el botón «Cargar datos de prueba» crea dos cursos «(prueba)» con estudiantes, 12 clases de
asistencia, trabajos, notas y aula virtual (incluye casos de riesgo). «Quitar datos de prueba» borra solo esos cursos.

Esquema de BD v7: tablas `entrega` y `recurso_clase`, columna `clase_docente.nota_minima` (migración automática).

### Reporte individual para el apoderado o tutor

En **Estudiantes → Reporte individual** (o clic derecho sobre un estudiante, o «Reporte» en el semáforo del Panel)
se abre una hoja A4 con: nivel de riesgo, asistencia (%, faltas, tardanzas y detalle por fecha a colores), trabajos
con su entrega y nota, promedio, puntos de atención, observación del docente, sugerencias para conversar con la
familia según lo detectado, líneas para notas a mano y firmas del docente y del apoderado. Con «Anterior/Siguiente»
(o las flechas del teclado) se recorre el curso. «Imprimir o guardar PDF» imprime esa hoja y «Reportes de todo el
curso» imprime una hoja por estudiante en un solo trabajo (reunión de padres, entrega de libretas).
