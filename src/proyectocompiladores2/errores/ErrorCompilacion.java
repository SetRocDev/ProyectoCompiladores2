package proyectocompiladores2.errores;

/**
 * Representa un error concreto detectado en cualquier etapa del análisis
 * (léxica, sintáctica o semántica). En el Proyecto 1 se llamaba ErrorLexico.
 *
 * Es inmutable: guarda el código del catálogo, la ubicación (línea y columna)
 * y la descripción ya completada con el lexema que falló.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public final class ErrorCompilacion {

    private final CodigoError codigo;
    private final int linea;
    private final int columna;
    private final String descripcion;

    /**
     * @param codigo      tipo de error según el catálogo
     * @param linea       número de línea (desde 1)
     * @param columna     columna donde inicia el problema (desde 1), 0 si no aplica
     * @param descripcion descripción específica del error
     */
    public ErrorCompilacion(CodigoError codigo, int linea, int columna, String descripcion) {
        this.codigo = codigo;
        this.linea = linea;
        this.columna = columna;
        this.descripcion = descripcion;
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Formato del .log (Opción 1, el error va debajo de su línea). La
     * descripción empieza con el tipo de análisis que encontró el error, para
     * distinguir las validaciones léxicas, sintácticas y semánticas:
     * {@code Error 607. [Sintáctico] Indentación incorrecta dentro del while...}
     * {@code Error 901. [Semántico] La variable '$x' se usa pero...}
     */
    @Override
    public String toString() {
        return "Error " + codigo.getNumero() + ". [" + codigo.getEtapa() + "] " + descripcion;
    }
}
