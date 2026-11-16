package proyectocompiladores2.sintactico;

import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.NodoCabeceraFor;
import proyectocompiladores2.sintactico.arbol.NodoFor;

/**
 * Comando for. El enunciado pide while, if y echo, pero la rúbrica de
 * "Manejo de errores en el archivo .log" menciona los errores "relacionados
 * con While y For", y el código de la cátedra tiene un for. Por eso el for se
 * valida con las mismas reglas que el while:
 *
 *     for → 'for' '(' inicio ';' condicion ';' incremento ')' '{' sentencias '}'
 *
 *   1. Estructura: paréntesis de apertura (611) y cierre (612), cabecera con
 *      sus tres partes separadas por ';' (613), llaves (614, 615; '{' en
 *      otra línea: 620).
 *   2. Contenido del bloque: al menos una línea de código (616).
 *   3. Indentación: 4 espacios por nivel (617), sin tabulaciones (618) y la
 *      '}' alineada con el for (619).
 *
 * El cuerpo lo revisa la clase base, ValidadorEstructuraControl, con los
 * códigos de TipoBloque.FOR.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorFor extends ValidadorEstructuraControl {

    /** Partes de la cabecera: inicio; condición; incremento. */
    private static final int PUNTOS_Y_COMA_DE_LA_CABECERA = 2;

    /**
     * @param analizador el analizador sintáctico, con el token actual en 'for'
     * @return el nodo del for (cabecera y bloque)
     */
    public NodoFor analizar(AnalizadorSintactico analizador) {
        Token palabraFor = analizador.actual();
        NodoFor nodo = new NodoFor(palabraFor.getLinea());
        nodo.agregarHijo(analizarCabeceraDelFor(analizador, palabraFor));
        nodo.agregarHijo(analizarCuerpo(analizador, TipoBloque.FOR, "for", palabraFor,
                analizador.getIndentacion().anchoDe(palabraFor.getLinea())));
        return nodo;
    }

    /**
     * cabecera → '(' inicio ';' condicion ';' incremento ')'
     *
     * @return la cabecera completa (los tokens entre los paréntesis)
     */
    private NodoCabeceraFor analizarCabeceraDelFor(AnalizadorSintactico analizador, Token palabraFor) {
        analizador.avanzar(); // for
        NodoCabeceraFor cabecera = new NodoCabeceraFor(palabraFor.getLinea());
        Token ultimo = palabraFor;

        boolean tieneApertura = !analizador.fin() && AnalizadorSintactico.es(analizador.actual(), "(");
        if (tieneApertura) {
            ultimo = analizador.actual();
            analizador.avanzar();
        } else {
            analizador.registrar(CodigoError.FOR_FALTA_PARENTESIS_APERTURA, palabraFor);
        }

        int profundidad = 0;
        int puntosYComa = 0;
        boolean tieneCierre = false;
        while (!analizador.fin()) {
            Token token = analizador.actual();
            // Aquí los ';' son parte de la cabecera; la terminan la '{', la '}' o '?>'
            if (AnalizadorSintactico.es(token, "{") || AnalizadorSintactico.es(token, "}")
                    || token.es(TipoToken.ETIQUETA_CIERRE) || token.es(TipoToken.ETIQUETA_APERTURA)) {
                break;
            }
            if (AnalizadorSintactico.es(token, "(")) {
                profundidad++;
            } else if (AnalizadorSintactico.es(token, ")")) {
                if (profundidad == 0) {
                    tieneCierre = true;
                    ultimo = token;
                    analizador.avanzar();
                    break;
                }
                profundidad--;
            } else if (AnalizadorSintactico.es(token, ";") && profundidad == 0) {
                puntosYComa++;
            }
            cabecera.agregarToken(token);
            ultimo = token;
            analizador.avanzar();
        }

        if (!tieneCierre) {
            analizador.registrar(CodigoError.FOR_FALTA_PARENTESIS_CIERRE, ultimo);
        }
        if (puntosYComa != PUNTOS_Y_COMA_DE_LA_CABECERA) {
            analizador.registrar(CodigoError.FOR_CABECERA_INCOMPLETA, palabraFor,
                    (puntosYComa + 1) + (puntosYComa == 0 ? " parte" : " partes"));
        }
        setUltimoDeCabecera(ultimo);
        return cabecera;
    }
}
