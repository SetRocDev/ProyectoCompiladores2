# Analizador de PHP en Java: léxico, sintáctico y semántico

**Proyecto 2 · Compiladores (03307) · UNED · III Cuatrimestre 2026**
Estudiante: Pablo Cortés

El programa revisa un archivo de código PHP como lo haría la primera parte de un compilador, en seis etapas:
- **Análisis léxico** (Proyecto 1): identificadores, etiquetas `<?php`/`?>` y comentarios.
- **Análisis sintáctico** (Proyecto 2): estructuras `while`, `if`/`else` y `echo`, con paréntesis, llaves, `;`, comillas, bloques vacíos e indentación.
- **Análisis semántico** (Proyecto 2): variables usadas sin haber recibido un valor y divisiones entre cero.

Al terminar crea `nombreArchivo-errores.log`: una copia exacta del `.php`, con cada línea numerada y los errores debajo de la línea donde ocurren, cada uno con su tipo: `[Léxico]`, `[Sintáctico]` o `[Semántico]`.

El alcance de cada validación se consultó con la profesora; sus aclaraciones están en la sección 11 del INFORME.

## Cómo probarlo
El programa ya viene compilado. Solo se necesita Java 8 o más nuevo.

1. Descomprimir el proyecto y entrar a la carpeta **`dist`**. Ahí están el programa (`ProyectoCompiladores2.jar`) y el código de ejemplo (`CodigoProyecto.php`).
2. Abrir el Símbolo del sistema en esa carpeta: clic en la barra de direcciones del Explorador, escribir `cmd` y presionar Enter.
3. Ejecutar:
   ```
   java -jar ProyectoCompiladores2.jar
   ```
4. Abrir el archivo nuevo **`CodigoProyecto-errores.log`** con el Bloc de notas.

Opciones: `-tokens` muestra los tokens y la tabla de símbolos; `-arbol` muestra el árbol sintáctico.

Las instrucciones completas, en lenguaje sencillo, están en **[INFORME.txt](INFORME.txt)**.

## Qué hay en cada carpeta
| Carpeta o archivo | Qué contiene |
|---|---|
| [`INFORME.txt`](INFORME.txt) | Instrucciones, correcciones del Proyecto 1, lista de errores, etapas, gramática, decisiones, pruebas y uso de IA |
| [`dist/`](dist) | El programa listo para usar (`.jar`) y el archivo PHP de ejemplo |
| [`src/`](src/proyectocompiladores2) | El código fuente en Java |
| [`pruebas/`](pruebas) | 127 casos (`casos/`), el `.log` exacto que debe salir de cada uno (`esperados/`), la tabla [`esperado.txt`](pruebas/esperado.txt), los scripts `ejecutar-pruebas.bat` / `.sh` que los comparan automáticamente y las [herramientas](pruebas/herramientas/LEEME.txt) de las pruebas extensas (opcionales, Python 3) |
| [`pruebas/catedra/`](pruebas/catedra/LEEME.txt) | 616 copias del código de la cátedra con errores introducidos (uno, dos o tres), el `.log` exacto de cada una, la tabla [`esperado.txt`](pruebas/catedra/esperado.txt) con lo que se cambió y los scripts `ejecutar-catedra.bat` / `.sh` |
| [`recursos/`](recursos) | Copia del código PHP original de la cátedra |
| `nbproject/`, `build.xml`, `manifest.mf`, `test/` | Archivos del proyecto NetBeans (*Java with Ant*) |
| [`CLAUDE.md`](CLAUDE.md) | Reglas del proyecto para el asistente de IA (Claude Code): restricciones, comandos y decisiones vigentes |

## Dónde está cada etapa
| Etapa | Archivo | Qué hace |
|---|---|---|
| 1. Obtención de tokens | [AnalizadorLexico.java](src/proyectocompiladores2/lexico/AnalizadorLexico.java) | Corta cada línea en lexemas |
| 2. Clasificación | [ClasificadorTokens.java](src/proyectocompiladores2/lexico/ClasificadorTokens.java) | Dice de qué tipo es cada token |
| 3. Patrón | [Patrones.java](src/proyectocompiladores2/lexico/Patrones.java) | Reglas de cada tipo y palabras reservadas |
| 4. Validación del lexema | [ValidadorLexema.java](src/proyectocompiladores2/lexico/ValidadorLexema.java) | Identificadores, etiquetas y comentarios ([validacion/](src/proyectocompiladores2/validacion)) |
| 5. Análisis sintáctico | [AnalizadorSintactico.java](src/proyectocompiladores2/sintactico/AnalizadorSintactico.java) | Descenso recursivo y árbol sintáctico; [ValidadorWhile](src/proyectocompiladores2/sintactico/ValidadorWhile.java), [ValidadorIf](src/proyectocompiladores2/sintactico/ValidadorIf.java), [ValidadorEcho](src/proyectocompiladores2/sintactico/ValidadorEcho.java) |
| 6. Análisis semántico | [AnalizadorSemantico.java](src/proyectocompiladores2/semantico/AnalizadorSemantico.java) | Tabla de símbolos, uso de variables y división entre cero |

Lista de errores: [CodigoError.java](src/proyectocompiladores2/errores/CodigoError.java) · Punto de inicio: [Main.java](src/proyectocompiladores2/Main.java)

## Cómo se desarrolló
Cada tarea tiene una tarjeta en Trello y un *Pull Request* en GitHub, donde se explica qué cambió, cómo se probó y qué decir en la defensa.

| Tarea | Contenido | Pull Request |
|---|---|---|
| T01–T13 | Proyecto 1: analizador léxico (versión entregada: etiqueta `v1.0`) | [#1](../../pull/1) – [#15](../../pull/15) |
| T14 | Nombre ProyectoCompiladores2 y pruebas automáticas | [#17](../../pull/17) |
| T15 | Corrección P1: salto de línea de cada línea en el `.log` | [#18](../../pull/18) |
| T16 | Corrección P1: errores 303 y 305 con su línea | [#19](../../pull/19) |
| T17–T19 | Correcciones P1: varias líneas, solo lado izquierdo del `=`, división y texto suelto | [#20](../../pull/20) |
| T20 | Etapa 5: base del análisis sintáctico | [#21](../../pull/21) |
| T21 | Comando while | [#22](../../pull/22) |
| T22 | Comando if / else | [#23](../../pull/23) |
| T23 | Comando echo | [#24](../../pull/24) |
| T24 | Etapa 6: análisis semántico | [#25](../../pull/25) |
| T25 | Integración, resumen por etapa y opción `-arbol` | [#26](../../pull/26) |
| T26 | Control de calidad final | [#27](../../pull/27) |
| T27 | INFORME y documentación | [#28](../../pull/28) |
| T29 | Revisión final: comando for, semántico 902/903, codificación del `.log` y simulación en otro equipo | [#29](../../pull/29) |
| T30 | Pulido tras el code review: expresiones válidas en echo, `%=`, mensajes del 902 y recuperación con llaves faltantes | [#30](../../pull/30) |
| T31 | Ajustes según la respuesta de la profesora: llaves y else en su propia línea, paréntesis en el echo, sin validar el for, semántico 901/902 y tipo de error en cada mensaje | [#31](../../pull/31) |
| T32 | Revisión integral final: cadenas muy largas, números de PHP, echo y else mal escritos, herramientas de verificación en el repositorio | [#32](../../pull/32) |
| T33 | Pruebas con el código de la cátedra (carpeta `pruebas/catedra`, más de 100 000 pruebas): palabras clave mal escritas junto con otros errores, texto antes de `<?php`, funciones anónimas | [#33](../../pull/33) |

- Todos los Pull Requests: [lista completa](../../pulls?q=is%3Apr)
- Historial de cambios: [commits](../../commits/main)

## Uso de inteligencia artificial
Se usó Claude Code (Anthropic) como asistente supervisado. El detalle y los prompts principales están en la sección 15 de [INFORME.txt](INFORME.txt).
