package proyectocompiladores2.sintactico;

import proyectocompiladores2.errores.CodigoError;

/**
 * Tipos de bloque { } que reconoce el análisis sintáctico y el código de
 * error que corresponde a cada regla en cada tipo (incluidas las de la
 * cabecera "palabra (condición)" del while, del if y del for).
 *
 * Así las reglas comunes a todos los bloques (llaves, bloque vacío,
 * indentación) se programan una sola vez en el AnalizadorSintactico y en el
 * ValidadorIndentacion, y cada estructura reporta con sus propios números de
 * error (6xx el while y el for, 7xx el if...).
 *
 * Un código null significa que esa regla no se valida para ese tipo de
 * bloque. Por ejemplo, el enunciado no pide validar foreach ni function
 * (decisión Q12): su bloque es GENERICO, solo se exige que cierre su llave.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public enum TipoBloque {

    /** Bloque del while (R3): errores 6xx. */
    WHILE(CodigoError.WHILE_FALTA_PARENTESIS_APERTURA, CodigoError.WHILE_FALTA_PARENTESIS_CIERRE,
            CodigoError.WHILE_CONDICION_VACIA,
            CodigoError.WHILE_FALTA_LLAVE_APERTURA, CodigoError.WHILE_LLAVE_EN_OTRA_LINEA,
            CodigoError.WHILE_FALTA_LLAVE_CIERRE, CodigoError.WHILE_BLOQUE_VACIO,
            CodigoError.WHILE_INDENTACION, CodigoError.WHILE_TABULACION, CodigoError.WHILE_CIERRE_DESALINEADO),

    /** Bloque del for: errores 611-620 (la rúbrica menciona los errores del for). */
    FOR(CodigoError.FOR_FALTA_PARENTESIS_APERTURA, CodigoError.FOR_FALTA_PARENTESIS_CIERRE, null,
            CodigoError.FOR_FALTA_LLAVE_APERTURA, CodigoError.FOR_LLAVE_EN_OTRA_LINEA,
            CodigoError.FOR_FALTA_LLAVE_CIERRE, CodigoError.FOR_BLOQUE_VACIO,
            CodigoError.FOR_INDENTACION, CodigoError.FOR_TABULACION, CodigoError.FOR_CIERRE_DESALINEADO),

    /** Bloque del if y del elseif (R4): errores 701-710. */
    IF(CodigoError.IF_FALTA_PARENTESIS_APERTURA, CodigoError.IF_FALTA_PARENTESIS_CIERRE,
            CodigoError.IF_CONDICION_VACIA,
            CodigoError.IF_FALTA_LLAVE_APERTURA, CodigoError.IF_LLAVE_EN_OTRA_LINEA,
            CodigoError.IF_FALTA_LLAVE_CIERRE, CodigoError.IF_BLOQUE_VACIO,
            CodigoError.IF_INDENTACION, CodigoError.IF_TABULACION, CodigoError.IF_CIERRE_DESALINEADO),

    /** Bloque del else (R4): errores 711-719. El else no tiene condición. */
    ELSE(null, null, null,
            CodigoError.ELSE_FALTA_LLAVE_APERTURA, CodigoError.ELSE_LLAVE_EN_OTRA_LINEA,
            CodigoError.ELSE_FALTA_LLAVE_CIERRE, CodigoError.ELSE_BLOQUE_VACIO,
            CodigoError.ELSE_INDENTACION, CodigoError.ELSE_TABULACION, CodigoError.ELSE_CIERRE_DESALINEADO),

    /** foreach, function, switch, class... y bloques { } sueltos. */
    GENERICO(null, null, null, null, null, CodigoError.BLOQUE_SIN_CIERRE, null, null, null, null);

    private final CodigoError faltaParentesisApertura;
    private final CodigoError faltaParentesisCierre;
    private final CodigoError condicionVacia;

    private final CodigoError faltaApertura;
    private final CodigoError aperturaEnOtraLinea;
    private final CodigoError faltaCierre;
    private final CodigoError bloqueVacio;
    private final CodigoError indentacion;
    private final CodigoError tabulacion;
    private final CodigoError cierreDesalineado;

    TipoBloque(CodigoError faltaParentesisApertura, CodigoError faltaParentesisCierre,
               CodigoError condicionVacia,
               CodigoError faltaApertura, CodigoError aperturaEnOtraLinea, CodigoError faltaCierre,
               CodigoError bloqueVacio, CodigoError indentacion, CodigoError tabulacion,
               CodigoError cierreDesalineado) {
        this.faltaParentesisApertura = faltaParentesisApertura;
        this.faltaParentesisCierre = faltaParentesisCierre;
        this.condicionVacia = condicionVacia;
        this.faltaApertura = faltaApertura;
        this.aperturaEnOtraLinea = aperturaEnOtraLinea;
        this.faltaCierre = faltaCierre;
        this.bloqueVacio = bloqueVacio;
        this.indentacion = indentacion;
        this.tabulacion = tabulacion;
        this.cierreDesalineado = cierreDesalineado;
    }

    /** Error cuando falta el '(' después de la palabra clave (while, if, for). */
    public CodigoError getFaltaParentesisApertura() {
        return faltaParentesisApertura;
    }

    /** Error cuando falta el ')' que cierra la condición. */
    public CodigoError getFaltaParentesisCierre() {
        return faltaParentesisCierre;
    }

    /** Error cuando no hay nada entre los paréntesis. */
    public CodigoError getCondicionVacia() {
        return condicionVacia;
    }

    /** Error cuando no hay '{' después de la cabecera. */
    public CodigoError getFaltaApertura() {
        return faltaApertura;
    }

    /** Error cuando la '{' está en la línea siguiente y no en la de la cabecera (decisión Q7). */
    public CodigoError getAperturaEnOtraLinea() {
        return aperturaEnOtraLinea;
    }

    /** Error cuando el bloque no tiene '}'. Su descripción recibe (palabra clave, línea de inicio). */
    public CodigoError getFaltaCierre() {
        return faltaCierre;
    }

    /** Error cuando el bloque no tiene ninguna línea de código (decisión Q8). */
    public CodigoError getBloqueVacio() {
        return bloqueVacio;
    }

    /** Error de cantidad de espacios. Su descripción recibe (esperados, encontrados). */
    public CodigoError getIndentacion() {
        return indentacion;
    }

    /** Error por usar tabulaciones en la indentación. */
    public CodigoError getTabulacion() {
        return tabulacion;
    }

    /** Error cuando la '}' no está alineada. Su descripción recibe (esperados, encontrados). */
    public CodigoError getCierreDesalineado() {
        return cierreDesalineado;
    }

    /** @return true si este tipo de bloque tiene reglas de indentación propias. */
    public boolean validaIndentacion() {
        return indentacion != null;
    }
}
