# CLAUDE.md – Proyecto 2 de Compiladores (UNED 03307, III-2026)

Analizador de PHP en Java: etapas léxicas del Proyecto 1 + análisis sintáctico (while, if/else, echo) y semántico del Proyecto 2. Estudiante: Pablo Cortés. Claude Code trabaja como asistente supervisado: Pablo revisa y fusiona cada PR.

## Restricciones del enunciado (si se rompen, el proyecto no se acepta)
- Java, proyecto NetBeans **Java with Ant** (sin Maven ni Gradle), 100% consola.
- **Sin librerías externas** (nada de JFlex, JavaCC, ANTLR...). Solo el JDK (`java.util.regex` está permitido).
- El `.jar` debe correr con **Java 8 o superior**: `javac.source/target=1.8`; no usar APIs de Java 9+ (`List.of`, `Set.of`, `var`, `String.repeat`, `isBlank`, `Files.readString`...). Para conjuntos usar `Patrones.conjunto(...)`.
- El `.php` se busca en la carpeta del `.jar`, sin nombre ni ruta fijos. El `.log` es `nombre-errores.log`.
- Formato del `.log`: copia exacta del `.php` (incluidos los saltos de línea originales) con `0001 ` al inicio de cada línea y los errores **debajo de su línea** (Opción 1), de forma consistente en todo el archivo: `Error NNN. Descripción específica`.
- El `.log` se escribe con la codificación del `.php` (UTF-8 con o sin BOM, Windows-1252 o ISO-8859-1): se verifica byte por byte.

## Estructura
- `src/proyectocompiladores2/`: `io/` (lectura y log), `errores/` (catálogo `CodigoError` 1xx–9xx), `lexico/` (etapas 1–4), `validacion/` (reglas léxicas R3–R5 del P1), `sintactico/` (etapa 5) y `semantico/` (etapa 6).
- Cada clase indica en su comentario inicial que se hizo con apoyo de IA (lo exige el enunciado).
- `pruebas/casos/`: entradas `.php` (byte por byte, no editarlas a mano con un editor que cambie los saltos de línea).
- `pruebas/esperados/`: el `-errores.log` exacto que debe generar cada caso.
- `dist/`: el `.jar` compilado + `CodigoProyecto.php` de ejemplo (se entrega así). `recursos/`: copia del PHP de la cátedra.

## Comandos
- Compilar como NetBeans: `ant clean jar` (el `-post-jar` de `build.xml` copia `recursos/CodigoProyecto.php` a `dist/`, que el clean borra).
- Pruebas: `pruebas/ejecutar-pruebas.sh` (`-v` muestra diferencias; `JAVA=/ruta/java` para probar con Java 8). En Windows: `pruebas\ejecutar-pruebas.bat`.
- Actualizar esperados (solo si el cambio es intencional y se explica en el PR): `pruebas/ejecutar-pruebas.sh --actualizar`.
- Contraste con PHP real: `php -l archivo.php`.

## Flujo de trabajo
- Planificación en Trello (tablero "Proyecto Compiladores 2"): una tarjeta por tarea (T14–T30), decisiones Q1–Q13 en la lista Questions/Done.
- Una rama por tarea (`fix/T15-...`, `feature/T20-...`) y un PR por tarea. Las ramas van encadenadas: cada PR apunta a la rama anterior y se fusionan en orden.
- Si una revisión pide un cambio sobre una tarea ya fusionada, se corrige hacia adelante en la rama actual.
- Plantilla del PR: Tarjeta, Qué cambia, Requerimiento cubierto, Cómo se probó (resultado de la batería n/n), Para la defensa, Uso de IA.
- Ningún PR sin la batería en verde. Cada error nuevo lleva su caso en `pruebas/casos` y su `.log` esperado.
- Commits en español con prefijos `feat`, `fix`, `test`, `docs`, `chore`.

## Decisiones vigentes (detalle en Trello)
- Q5: palabras reservadas sin distinguir mayúsculas; identificadores sí las distinguen.
- Q7: `{` en la misma línea del while/if/else y `} else {` en una línea.
- Q8: un bloque con solo comentarios o líneas en blanco está vacío.
- Q9: indentación de 4 espacios × nivel de anidamiento (cuenta también el for), sin tabulaciones; se revisa la primera línea de cada sentencia y la `}`; comentarios y nivel 0 no se revisan.
- Q10: echo sin paréntesis de agrupación (las llamadas a función sí se permiten).
- Q11: semántico = 901 variable usada sin asignar, 902 condición que asigna un valor fijo (`if ($x = 5)`) y 903 división entre el número 0. La regla de "posible ciclo infinito" se retiró: el PDF muestra como permitido un while que no cambia su variable.
- Q12: el for se valida como el while (611–620, T29); foreach/function son bloques genéricos; elseif/else if se validan como if; 504 para palabras de control mal escritas.
- Q13: 201–205 solo a la izquierda del `=`; 303 en la línea 0001 y 305 en la última línea.
