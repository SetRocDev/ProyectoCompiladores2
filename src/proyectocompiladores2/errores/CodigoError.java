package proyectocompiladores2.errores;

/**
 * Catálogo de errores del analizador (todas las etapas: archivo, léxico,
 * sintáctico y semántico).
 *
 * Cada constante define un número de error y una plantilla de descripción
 * (formato de {@link String#format}) que se completa con el lexema o dato
 * concreto que provocó el error, para que el mensaje sea específico y no
 * genérico, como exige el enunciado.
 *
 * Numeración por centenas, una por requerimiento:
 *   1xx  Archivo (se reportan en consola, no hay .log sin archivo)
 *   2xx  Identificadores (R3)
 *   3xx  Etiquetas de apertura y cierre (R4)
 *   4xx  Comentarios (R5)
 *   5xx  Errores generales: léxicos (501, 502) y de estructura del código (503-505)
 *   6xx  Ciclos (Proyecto 2): while (601-610) y for (611-620)
 *   7xx  Comando if / else (Proyecto 2)
 *   8xx  Comando echo (Proyecto 2)
 *   9xx  Análisis semántico (Proyecto 2)
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public enum CodigoError {

    // ===== 1xx: Archivo =====
    ARCHIVO_NO_ENCONTRADO(101, "No se encontró ningún archivo con extensión .php en la carpeta '%s'"),
    EXTENSION_INVALIDA(102, "El archivo '%s' no tiene la extensión exacta .php"),
    ERROR_LECTURA(103, "No se pudo leer el archivo '%s': %s"),
    ERROR_ESCRITURA(104, "No se pudo escribir el archivo de errores de '%s' (revise que no esté abierto en otro programa): %s"),

    // ===== 2xx: Identificadores (R3) =====
    ID_INICIA_CON_DIGITO(201, "El identificador '%s' no puede iniciar con un dígito después de '$'"),
    ID_SIMBOLO_INICIAL_INVALIDO(202, "El identificador '%s' tiene el carácter inválido '%s' después de '$'; debe iniciar con una letra o guion bajo"),
    ID_CARACTER_INVALIDO(203, "El identificador '%s' contiene el carácter inválido '%s'; solo se permiten letras, dígitos y guion bajo"),
    ID_PALABRA_RESERVADA(204, "El identificador '%s' usa la palabra reservada '%s' y no puede ser asignado"),
    ID_SIN_NOMBRE(205, "Se encontró el símbolo '$' sin un nombre de identificador"),
    ID_SIN_SIMBOLO_DOLAR(206, "El identificador '%s' no inicia con el símbolo '$'; todo identificador debe iniciar con '$' seguido de una letra o guion bajo"),

    // ===== 3xx: Etiquetas de apertura y cierre (R4) =====
    APERTURA_CONTENIDO_PREVIO(301, "Antes de la etiqueta de apertura '<?php' hay %s; la etiqueta debe ser lo primero del archivo"),
    APERTURA_MAL_ESCRITA(302, "Etiqueta de apertura mal escrita '%s'; se esperaba exactamente '<?php'"),
    APERTURA_FALTANTE(303, "No se encontró la etiqueta de apertura '<?php'; debe ser lo primero del archivo, al inicio de la línea 0001"),
    CIERRE_MAL_ESCRITO(304, "Etiqueta de cierre mal escrita '%s'; se esperaba exactamente '?>'"),
    CIERRE_FALTANTE(305, "No se encontró la etiqueta de cierre '?>'; debe estar en la última línea del archivo (línea %s)"),
    CIERRE_CONTENIDO_POSTERIOR(306, "Se encontró contenido después de la etiqueta de cierre '?>': '%s'"),
    CIERRE_NO_ES_ULTIMA_LINEA(307, "La etiqueta de cierre '?>' no está en la última línea del archivo; después se encontró: '%s'"),
    CIERRE_ESPACIOS_POSTERIORES(308, "Hay %s después de la etiqueta de cierre '?>'; el archivo debe terminar exactamente en '?>'"),

    // ===== 4xx: Comentarios (R5) =====
    COMENTARIO_MAL_FORMADO(401, "Comentario mal formado '%s'; un comentario de línea debe iniciar con '//' o '#'"),
    TEXTO_FUERA_DE_COMENTARIO(402, "Texto '%s' fuera de un comentario: no es código PHP válido; si es parte de un comentario, cada línea debe iniciar con '//' o '#'"),

    // ===== 5xx: Errores léxicos generales =====
    CARACTER_NO_RECONOCIDO(501, "Carácter no reconocido '%s'"),
    CADENA_SIN_CERRAR(502, "La cadena de texto %s no tiene la comilla de cierre"),
    // 503 a 505: estructura general del código (análisis sintáctico, etapa 5)
    LLAVE_SIN_APERTURA(503, "Llave de cierre '}' sin una llave de apertura '{' que le corresponda"),
    ESTRUCTURA_DESCONOCIDA(504, "'%s' no es una estructura de control válida%s"),
    BLOQUE_SIN_CIERRE(505, "El bloque de '%s' que inicia en la línea %s no tiene llave de cierre '}'"),

    // ===== 6xx: Comando while (R3 del Proyecto 2) =====
    WHILE_FALTA_PARENTESIS_APERTURA(601, "Falta el paréntesis de apertura '(' después de 'while'; la forma correcta es 'while (condición) {'"),
    WHILE_FALTA_PARENTESIS_CIERRE(602, "Falta el paréntesis de cierre ')' de la condición del while"),
    WHILE_CONDICION_VACIA(603, "La condición del while está vacía; debe haber una condición entre los paréntesis"),
    WHILE_FALTA_LLAVE_APERTURA(604, "Falta la llave de apertura '{' del bloque del while"),
    WHILE_FALTA_LLAVE_CIERRE(605, "El bloque del while que inicia en la línea %2$s no tiene llave de cierre '}'"),
    WHILE_BLOQUE_VACIO(606, "El bloque del while está vacío; debe contener al menos una línea de código"),
    WHILE_INDENTACION(607, "Indentación incorrecta dentro del while: se esperaban %s espacios y hay %s"),
    WHILE_TABULACION(608, "Se usó una tabulación para indentar dentro del while; solo se permiten espacios (4 por nivel)"),
    WHILE_CIERRE_DESALINEADO(609, "La llave de cierre '}' del while no está alineada con 'while': se esperaban %s espacios y hay %s"),
    WHILE_LLAVE_EN_OTRA_LINEA(610, "La llave de apertura '{' del while debe estar en la misma línea que su condición: 'while (condición) {'"),
    FOR_FALTA_PARENTESIS_APERTURA(611, "Falta el paréntesis de apertura '(' después de 'for'; la forma correcta es 'for (inicio; condición; incremento) {'"),
    FOR_FALTA_PARENTESIS_CIERRE(612, "Falta el paréntesis de cierre ')' de la cabecera del for"),
    FOR_CABECERA_INCOMPLETA(613, "La cabecera del for debe tener tres partes separadas por ';' (inicio; condición; incremento) y tiene %s"),
    FOR_FALTA_LLAVE_APERTURA(614, "Falta la llave de apertura '{' del bloque del for"),
    FOR_FALTA_LLAVE_CIERRE(615, "El bloque del for que inicia en la línea %2$s no tiene llave de cierre '}'"),
    FOR_BLOQUE_VACIO(616, "El bloque del for está vacío; debe contener al menos una línea de código"),
    FOR_INDENTACION(617, "Indentación incorrecta dentro del for: se esperaban %s espacios y hay %s"),
    FOR_TABULACION(618, "Se usó una tabulación para indentar dentro del for; solo se permiten espacios (4 por nivel)"),
    FOR_CIERRE_DESALINEADO(619, "La llave de cierre '}' del for no está alineada con 'for': se esperaban %s espacios y hay %s"),
    FOR_LLAVE_EN_OTRA_LINEA(620, "La llave de apertura '{' del for debe estar en la misma línea que su cabecera: 'for (inicio; condición; incremento) {'"),

    // ===== 7xx: Comando if y bloque else (R4 del Proyecto 2) =====
    IF_FALTA_PARENTESIS_APERTURA(701, "Falta el paréntesis de apertura '(' después de '%s'; la forma correcta es 'if (condición) {'"),
    IF_FALTA_PARENTESIS_CIERRE(702, "Falta el paréntesis de cierre ')' de la condición del %s"),
    IF_CONDICION_VACIA(703, "La condición del %s está vacía; debe haber una condición entre los paréntesis"),
    IF_FALTA_LLAVE_APERTURA(704, "Falta la llave de apertura '{' del bloque del %s"),
    IF_FALTA_LLAVE_CIERRE(705, "El bloque del %s que inicia en la línea %s no tiene llave de cierre '}'"),
    IF_BLOQUE_VACIO(706, "El bloque del if está vacío; debe contener al menos una línea de código"),
    IF_INDENTACION(707, "Indentación incorrecta dentro del if: se esperaban %s espacios y hay %s"),
    IF_TABULACION(708, "Se usó una tabulación para indentar dentro del if; solo se permiten espacios (4 por nivel)"),
    IF_CIERRE_DESALINEADO(709, "La llave de cierre '}' del if no está alineada con 'if': se esperaban %s espacios y hay %s"),
    IF_LLAVE_EN_OTRA_LINEA(710, "La llave de apertura '{' del %s debe estar en la misma línea que su condición: 'if (condición) {'"),
    ELSE_FALTA_LLAVE_APERTURA(711, "Falta la llave de apertura '{' después de 'else'; la forma correcta es '} else {'"),
    ELSE_FALTA_LLAVE_CIERRE(712, "El bloque del else que inicia en la línea %2$s no tiene llave de cierre '}'"),
    ELSE_BLOQUE_VACIO(713, "El bloque del else está vacío; debe contener al menos una línea de código"),
    ELSE_SIN_IF(714, "'%s' sin un 'if' previo: debe ir justo después de la llave de cierre '}' de un if"),
    ELSE_INDENTACION(715, "Indentación incorrecta dentro del else: se esperaban %s espacios y hay %s"),
    ELSE_TABULACION(716, "Se usó una tabulación para indentar dentro del else; solo se permiten espacios (4 por nivel)"),
    ELSE_CIERRE_DESALINEADO(717, "La llave de cierre '}' del else no está alineada con el if: se esperaban %s espacios y hay %s"),
    ELSE_EN_OTRA_LINEA(718, "'%s' debe ir en la misma línea que la llave de cierre '}' del if: '} else {'"),
    ELSE_LLAVE_EN_OTRA_LINEA(719, "La llave de apertura '{' del else debe estar en la misma línea que 'else': '} else {'"),

    // ===== 8xx: Comando echo (R5 del Proyecto 2) =====
    ECHO_SIN_EXPRESION(801, "La sentencia echo no tiene una expresión o cadena para imprimir"),
    ECHO_CON_PARENTESIS(802, "La sentencia echo no debe usar paréntesis para agrupar lo que imprime; la forma correcta es 'echo \"texto\";'"),
    ECHO_COMILLA_DOBLE_ABIERTA(803, "La cadena %s del echo no tiene la comilla doble (\") de cierre"),
    ECHO_COMILLA_SIMPLE_ABIERTA(804, "La cadena %s del echo no tiene la comilla simple (') de cierre"),
    ECHO_SIN_PUNTO_Y_COMA(805, "Falta el punto y coma ';' al final de la sentencia echo"),
    ECHO_EXPRESION_INVALIDA(806, "Expresión inválida en el echo: %s"),

    // ===== 9xx: Análisis semántico (R6 del Proyecto 2) =====
    VARIABLE_NO_ASIGNADA(901, "La variable '%s' se usa pero no se le asignó un valor antes%s"),
    ASIGNACION_EN_CONDICION(902, "La condición del %s asigna un valor fijo ('%s = %s') en lugar de compararlo; para comparar se usa '=='"),
    DIVISION_ENTRE_CERO(903, "División entre cero ('%s 0'): en PHP 8 detiene el programa con DivisionByZeroError");

    private final int numero;
    private final String plantilla;

    CodigoError(int numero, String plantilla) {
        this.numero = numero;
        this.plantilla = plantilla;
    }

    /** @return etapa que detecta el error, según su número (para el resumen en consola). */
    public String getEtapa() {
        if (numero < 200) {
            return "Archivo";
        }
        if (numero < 503) {
            return "Léxico";
        }
        if (numero < 900) {
            return "Sintáctico";
        }
        return "Semántico";
    }

    /** @return número del error, por ejemplo 201. */
    public int getNumero() {
        return numero;
    }

    /**
     * Construye la descripción específica del error completando la plantilla.
     *
     * @param datos valores a insertar (lexema, carácter inválido, etc.)
     * @return descripción lista para escribir en el .log
     */
    public String describir(Object... datos) {
        return datos.length == 0 ? plantilla : String.format(plantilla, datos);
    }
}
