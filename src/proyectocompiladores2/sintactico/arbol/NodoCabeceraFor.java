package proyectocompiladores2.sintactico.arbol;

/**
 * Cabecera de un for: los tokens entre los paréntesis, con sus tres partes
 * separadas por ';' (inicio; condición; incremento). A diferencia de la
 * condición de un while o un if, aquí sí hay asignaciones ($i = 0), por eso
 * el análisis semántico la revisa como tres sentencias.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoCabeceraFor extends Nodo {

    public NodoCabeceraFor(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Cabecera";
    }
}
