package proyectocompiladores2.sintactico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;

/**
 * Secuencia de tokens "significativos": todos los tokens del archivo, en
 * orden, SIN los comentarios.
 *
 * Una instrucción de PHP puede ocupar varias líneas y tener comentarios en
 * medio; lo que importa es el orden de los tokens, no la línea en la que
 * están. Por ejemplo, estas dos formas son la misma asignación:
 *
 *     $if = 10;            $if      // comentario
 *                              = 10;
 *
 * Esta clase permite preguntar por el token anterior o siguiente sin
 * importar los saltos de línea ni los comentarios (corrección del Proyecto 1,
 * tarea T17), y es la base sobre la que trabaja el análisis sintáctico.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class FlujoTokens {

    private final List<Token> tokens;

    /**
     * @param todos los tokens ya clasificados (etapa 2), incluidos los comentarios
     */
    public FlujoTokens(List<Token> todos) {
        List<Token> significativos = new ArrayList<>();
        for (Token token : todos) {
            if (!token.es(TipoToken.COMENTARIO)) {
                significativos.add(token);
            }
        }
        this.tokens = Collections.unmodifiableList(significativos);
    }

    /** @return cantidad de tokens significativos. */
    public int tamano() {
        return tokens.size();
    }

    /** @return el token en la posición indicada (desde 0). */
    public Token get(int posicion) {
        return tokens.get(posicion);
    }

    /** @return los tokens significativos, en orden. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** @return el token significativo anterior, o null si es el primero. */
    public Token anterior(int posicion) {
        return posicion > 0 ? tokens.get(posicion - 1) : null;
    }

    /** @return el token significativo siguiente, o null si es el último. */
    public Token siguiente(int posicion) {
        return posicion + 1 < tokens.size() ? tokens.get(posicion + 1) : null;
    }

    /**
     * Una sentencia nueva empieza al inicio del archivo, después de la
     * etiqueta '<?php' o después de ';', '{' o '}'.
     *
     * @return true si el token de esa posición es el primero de una sentencia
     */
    public boolean esInicioDeSentencia(int posicion) {
        Token previo = anterior(posicion);
        if (previo == null || previo.es(TipoToken.ETIQUETA_APERTURA)) {
            return true;
        }
        String lexema = previo.getLexema();
        return lexema.equals(";") || lexema.equals("{") || lexema.equals("}");
    }
}
