package proyectocompiladores2.sintactico.arbol;

/**
 * Estructura que el enunciado no pide validar (foreach, function, switch,
 * class...). Se reconoce para respetar sus llaves y el nivel de
 * indentación de lo que tiene dentro (decisión Q12). Sus tokens son los de
 * la cabecera (por ejemplo "foreach ( $notas as $nota )").
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoEstructura extends Nodo {

    private final String palabraClave;

    public NodoEstructura(int linea, String palabraClave) {
        super(linea);
        this.palabraClave = palabraClave;
    }

    public String getPalabraClave() {
        return palabraClave;
    }

    @Override
    public String getNombre() {
        return "Estructura " + palabraClave;
    }
}
