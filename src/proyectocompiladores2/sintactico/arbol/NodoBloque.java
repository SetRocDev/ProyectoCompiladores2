package proyectocompiladores2.sintactico.arbol;

/**
 * Bloque delimitado por llaves { }. Sus hijos son las sentencias del bloque.
 * Regla: bloque → '{' sentencia { sentencia } '}'
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoBloque extends Nodo {

    public NodoBloque(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Bloque";
    }
}
