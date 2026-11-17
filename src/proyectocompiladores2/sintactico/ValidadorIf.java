package proyectocompiladores2.sintactico;

import java.util.Locale;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.NodoBloque;
import proyectocompiladores2.sintactico.arbol.NodoElse;
import proyectocompiladores2.sintactico.arbol.NodoIf;

/**
 * R4 del Proyecto 2 - Comando if con su bloque else opcional.
 *
 *     if   → 'if' '(' condicion ')' '{' sentencias '}' [ else ]
 *     else → 'else' '{' sentencias '}'
 *          | 'else' if             ("else if", decisión Q12)
 *          | 'elseif' '(' condicion ')' '{' sentencias '}' [ else ]
 *
 * Los tres aspectos del enunciado:
 *   1. Estructura del if: paréntesis (701, 702), condición (703), llaves
 *      (704, 705).
 *   2. Bloque else, si existe: palabra else, llaves (711, 712), al menos una
 *      línea (713); un else sin if es error (714).
 *   3. Indentación en el if (707, 708, 709) y en el else (715, 716, 717).
 *
 * La '{' puede ir en la misma línea o sola en la siguiente, y el else puede
 * ir junto a la '}' ("} else {") o en su propia línea: la profesora aclaró
 * que eso no es error (decisión Q7). Lo que se revisa es que estén alineados
 * con el if: la '{' sola en su línea (710, 719) y el else en su propia
 * línea (718).
 *
 * La cabecera y el cuerpo del if los revisa la clase base,
 * ValidadorEstructuraControl, con los códigos de TipoBloque.IF; el bloque del
 * else usa TipoBloque.ELSE.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorIf extends ValidadorEstructuraControl {

    /**
     * @param analizador el analizador sintáctico, con el token actual en 'if'
     * @return el nodo del if (condición, bloque y else si existe)
     */
    public NodoIf analizar(AnalizadorSintactico analizador) {
        Token palabraIf = analizador.actual();
        return analizarIf(analizador, analizador.getIndentacion().anchoDe(palabraIf.getLinea()));
    }

    /**
     * Analiza un if (o un elseif) completo.
     *
     * @param anchoReferencia espacios de la línea del if original; en una
     *                        cadena "} else if (...) {" todos los bloques se
     *                        miden desde el primer if
     */
    private NodoIf analizarIf(AnalizadorSintactico analizador, int anchoReferencia) {
        Token palabraIf = analizador.actual();
        String palabraClave = palabraIf.getLexema().toLowerCase(Locale.ROOT);
        NodoIf nodo = new NodoIf(palabraIf.getLinea());
        nodo.agregarHijo(analizarCabecera(analizador, TipoBloque.IF));
        nodo.agregarHijo(analizarCuerpo(analizador, TipoBloque.IF, palabraClave, palabraIf, anchoReferencia));
        if (!analizador.fin() && (AnalizadorSintactico.esPalabra(analizador.actual(), "else")
                || AnalizadorSintactico.esPalabra(analizador.actual(), "elseif"))) {
            nodo.agregarHijo(analizarElse(analizador, anchoReferencia));
        }
        return nodo;
    }

    /**
     * El else (o elseif) que sigue al bloque de un if. El token actual es la
     * palabra else / elseif.
     */
    private NodoElse analizarElse(AnalizadorSintactico analizador, int anchoReferencia) {
        Token palabraElse = analizador.actual();
        NodoElse nodo = new NodoElse(palabraElse.getLinea());

        // El else en su propia línea va alineado con el if (decisión Q7)
        analizador.getIndentacion().validarAlineacion(palabraElse, anchoReferencia,
                CodigoError.ELSE_DESALINEADO, CodigoError.ELSE_TABULACION,
                palabraElse.getLexema().toLowerCase(Locale.ROOT));

        if (AnalizadorSintactico.esPalabra(palabraElse, "elseif")) {
            nodo.agregarHijo(new ValidadorIf().analizarIf(analizador, anchoReferencia));
            return nodo;
        }
        analizador.avanzar(); // else
        if (!analizador.fin() && AnalizadorSintactico.esPalabra(analizador.actual(), "if")
                && analizador.actual().getLinea() == palabraElse.getLinea()) {
            // "else if (...) {": un if completo dentro del else
            nodo.agregarHijo(new ValidadorIf().analizarIf(analizador, anchoReferencia));
            return nodo;
        }
        nodo.agregarHijo(cuerpoDelElse(analizador, palabraElse, anchoReferencia));
        return nodo;
    }

    /** Bloque del else: '{' sentencias '}' con las reglas de TipoBloque.ELSE. */
    private NodoBloque cuerpoDelElse(AnalizadorSintactico analizador, Token palabraElse, int anchoReferencia) {
        if (!analizador.fin() && AnalizadorSintactico.es(analizador.actual(), "{")) {
            Token llave = analizador.actual();
            if (llave.getLinea() != palabraElse.getLinea()) {
                analizador.getIndentacion().validarAlineacion(llave, anchoReferencia,
                        CodigoError.ELSE_APERTURA_DESALINEADA, CodigoError.ELSE_TABULACION);
            }
            return analizador.bloque(TipoBloque.ELSE, "else", palabraElse, anchoReferencia);
        }
        analizador.registrar(CodigoError.ELSE_FALTA_LLAVE_APERTURA, palabraElse);
        return analizador.cuerpoSinLlave(TipoBloque.ELSE, "else", palabraElse, anchoReferencia);
    }

    /**
     * Un else o elseif que no viene después del bloque de un if (Error 714).
     * Igual se analiza su bloque, para seguir encontrando los errores de
     * adentro sin generar errores en cascada.
     */
    public NodoElse analizarElseSinIf(AnalizadorSintactico analizador) {
        Token palabraElse = analizador.actual();
        analizador.registrar(CodigoError.ELSE_SIN_IF, palabraElse, palabraElse.getLexema().toLowerCase(Locale.ROOT));
        NodoElse nodo = new NodoElse(palabraElse.getLinea());
        int ancho = analizador.getIndentacion().anchoDe(palabraElse.getLinea());
        if (AnalizadorSintactico.esPalabra(palabraElse, "elseif")) {
            nodo.agregarHijo(new ValidadorIf().analizarIf(analizador, ancho));
            return nodo;
        }
        analizador.avanzar();
        nodo.agregarHijo(cuerpoDelElse(analizador, palabraElse, ancho));
        return nodo;
    }
}
