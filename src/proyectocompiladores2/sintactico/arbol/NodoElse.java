package proyectocompiladores2.sintactico.arbol;

/**
 * Bloque else de un if. Su hijo es el bloque (o un If, en el caso 'else if').
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoElse extends Nodo {

    public NodoElse(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Else";
    }
}
