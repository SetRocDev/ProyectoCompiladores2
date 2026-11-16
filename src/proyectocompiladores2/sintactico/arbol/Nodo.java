package proyectocompiladores2.sintactico.arbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectocompiladores2.lexico.Token;

/**
 * Nodo del árbol sintáctico.
 *
 * TRADUCCIÓN ORIENTADA POR LA SINTAXIS (tema 5): cada vez que el analizador
 * sintáctico reconoce una regla de la gramática (un while, un if, un echo,
 * un bloque...) construye el nodo que la representa. El resultado es un
 * árbol que describe la estructura del programa; el análisis semántico
 * (etapa 6) lo recorre y la opción "-arbol" lo muestra en consola.
 *
 * Cada nodo guarda la línea donde empieza, los tokens que le pertenecen
 * directamente (por ejemplo, los de la condición de un while) y sus hijos.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public abstract class Nodo {

    private final int linea;
    private final List<Token> tokens = new ArrayList<>();
    private final List<Nodo> hijos = new ArrayList<>();

    protected Nodo(int linea) {
        this.linea = linea;
    }

    /** @return línea del .php donde empieza este elemento. */
    public int getLinea() {
        return linea;
    }

    /** @return nombre del elemento para mostrar en el árbol (por ejemplo "While"). */
    public abstract String getNombre();

    public void agregarToken(Token token) {
        tokens.add(token);
    }

    public void agregarHijo(Nodo hijo) {
        if (hijo != null) {
            hijos.add(hijo);
        }
    }

    /** @return tokens propios de este nodo (no incluye los de sus hijos). */
    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public List<Nodo> getHijos() {
        return Collections.unmodifiableList(hijos);
    }

    /** @return el texto de los tokens propios, separados por espacio. */
    public String getTexto() {
        StringBuilder texto = new StringBuilder();
        for (Token token : tokens) {
            if (texto.length() > 0) {
                texto.append(' ');
            }
            texto.append(token.getLexema());
        }
        return texto.toString();
    }

    /** Agrega este nodo y sus hijos, con sangría según el nivel, para la opción -arbol. */
    public void imprimir(StringBuilder salida, int nivel) {
        for (int i = 0; i < nivel; i++) {
            salida.append("  ");
        }
        salida.append(getNombre()).append(" (línea ").append(linea).append(')');
        if (!tokens.isEmpty()) {
            salida.append(": ").append(getTexto());
        }
        salida.append(System.lineSeparator());
        for (Nodo hijo : hijos) {
            hijo.imprimir(salida, nivel + 1);
        }
    }
}
