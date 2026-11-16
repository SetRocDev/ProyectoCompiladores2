package proyectocompiladores2.sintactico.arbol;

/**
 * Estructura if: hijos = condición, bloque del if y, si existe, el else.
 * Regla: if → 'if' '(' condicion ')' bloque [ 'else' bloque ]
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoIf extends Nodo {

    private final String palabraClave;

    /**
     * @param linea        línea donde empieza
     * @param palabraClave "if" o "elseif" (en minúscula)
     */
    public NodoIf(int linea, String palabraClave) {
        super(linea);
        this.palabraClave = palabraClave;
    }

    /** @return "if" o "elseif": la palabra con la que se escribió en el código. */
    public String getPalabraClave() {
        return palabraClave;
    }

    @Override
    public String getNombre() {
        return "If";
    }
}
