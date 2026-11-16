package proyectocompiladores2.sintactico;

import java.util.Locale;
import java.util.Set;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.NodoEcho;

/**
 * R5 del Proyecto 2 - Comando echo.
 *
 *     echo      → 'echo' expresion { ',' expresion } ';'
 *     expresion → operando { operador operando }
 *     operando  → CADENA | NUMERO | $variable [ '[' ... ']' ]
 *               | nombre [ '(' argumentos ')' ] | true | false | null
 *     operador  → '.' | '+' | '-' | '*' | '/' | '%' | '**' | comparación | lógico | '?' ':'
 *               | '->' | '::' (acceso a un objeto o a una clase)
 *
 * Los tres aspectos del enunciado:
 *   1. Estructura: inicia con echo, sin paréntesis que agrupen lo que se
 *      imprime (802; los de una llamada a función sí se permiten, decisión
 *      Q10) y con una expresión o cadena válida (801 si no hay nada, 806 si
 *      a un operador le falta un valor o faltan operadores entre valores).
 *   2. Comillas: dobles o simples, siempre cerradas (803, 804). Este error
 *      reemplaza al 502 general de la etapa 4 para no repetir el mensaje, y
 *      no se reporta además la falta de ';' (la cadena abierta llega hasta el
 *      final de la línea).
 *   3. Terminación: la sentencia termina en ';' (805). Una expresión puede
 *      seguir en la línea siguiente si esa línea empieza con un operador.
 *
 * La expresión se revisa con un autómata de dos estados: "espero un valor"
 * y "espero un operador".
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorEcho {

    /**
     * Operadores que unen dos valores en la expresión de un echo: aritméticos,
     * concatenación, comparaciones, lógicos, desplazamientos, el operador
     * ternario (? :) y el acceso a un objeto o a una clase ($o->n, Clase::X).
     * Los lógicos escritos con palabras (and, or, xor, instanceof) se comparan
     * sin distinguir mayúsculas, como en PHP.
     */
    private static final Set<String> OPERADORES = Patrones.conjunto(".", "+", "-", "*", "/", "%", "**",
            "==", "===", "!=", "!==", "<>", "<", ">", "<=", ">=", "<=>", "<<", ">>",
            "&&", "||", "?", ":", "??", "and", "or", "xor", "instanceof", "->", "::");

    /** Tipos que se pueden escribir entre paréntesis para convertir un valor: (int) $x. */
    private static final Set<String> CONVERSIONES = Patrones.conjunto("int", "integer", "float", "double",
            "string", "bool", "boolean", "array", "object");

    /** Palabras reservadas que son valores. */
    private static final Set<String> VALORES_RESERVADOS = Patrones.conjunto("true", "false", "null");

    /**
     * @param analizador el analizador sintáctico, con el token actual en 'echo'
     * @return el nodo del echo (sus tokens son los de la expresión)
     */
    public NodoEcho analizar(AnalizadorSintactico analizador) {
        Token palabraEcho = analizador.actual();
        NodoEcho nodo = new NodoEcho(palabraEcho.getLinea());
        analizador.avanzar();

        boolean esperoValor = true;
        boolean parentesisReportado = false;
        int profundidad = 0;
        Token ultimo = palabraEcho;

        while (!analizador.fin()) {
            Token token = analizador.actual();
            if (AnalizadorSintactico.es(token, ";")) {
                analizador.avanzar();
                return terminar(analizador, nodo, palabraEcho, esperoValor, ultimo, true, profundidad);
            }
            if (terminaSinPuntoYComa(token, ultimo, esperoValor, profundidad)) {
                break;
            }
            if (esperoValor) {
                if (esConversion(analizador, token)) {
                    // (int) $x: el paréntesis convierte el valor, no agrupa
                    ultimo = consumirConversion(analizador, nodo);
                    continue;
                }
                if (AnalizadorSintactico.es(token, "(")) {
                    // Paréntesis que agrupan (no son de una llamada a función)
                    if (!parentesisReportado) {
                        analizador.registrar(CodigoError.ECHO_CON_PARENTESIS, token);
                        parentesisReportado = true;
                    }
                    profundidad++;
                } else if (token.es(TipoToken.CADENA)) {
                    if (!Patrones.CADENA.matcher(token.getLexema()).matches()) {
                        reportarComillaAbierta(analizador, token);
                        nodo.agregarToken(token);
                        analizador.avanzar();
                        return nodo;
                    }
                    esperoValor = false;
                } else if (esValor(token) || esLlamadaAFuncion(analizador, token) || esMiembro(ultimo, token)) {
                    esperoValor = false;
                    if (sigueGrupo(analizador, token)) {
                        nodo.agregarToken(token);
                        analizador.avanzar();
                        ultimo = consumirGrupo(analizador, nodo);
                        continue;
                    }
                } else if (AnalizadorSintactico.es(token, ")") && profundidad > 0) {
                    // "()" sin nada adentro
                    analizador.registrar(CodigoError.ECHO_EXPRESION_INVALIDA, token,
                            "los paréntesis no tienen ningún valor adentro");
                    profundidad--;
                } else if (AnalizadorSintactico.es(token, ":") && AnalizadorSintactico.es(ultimo, "?")) {
                    // "?:" (ternario corto): después sigue el valor alternativo
                } else if (!esSigno(token)) {
                    analizador.registrar(CodigoError.ECHO_EXPRESION_INVALIDA, token,
                            "se esperaba un valor y se encontró '" + token.getLexema() + "'");
                    return recuperar(analizador, nodo, ultimo);
                }
            } else {
                if (esOperador(token) || AnalizadorSintactico.es(token, ",")) {
                    esperoValor = true;
                } else if (AnalizadorSintactico.es(token, "++") || AnalizadorSintactico.es(token, "--")) {
                    // $i++: incremento después del valor; sigue esperando un operador
                } else if (AnalizadorSintactico.es(token, ")") && profundidad > 0) {
                    profundidad--;
                } else if (AnalizadorSintactico.es(token, "[")) {
                    nodo.agregarToken(token);
                    analizador.avanzar();
                    ultimo = consumirHasta(analizador, nodo, "[", "]");
                    continue;
                } else {
                    analizador.registrar(CodigoError.ECHO_EXPRESION_INVALIDA, token,
                            "falta un operador entre '" + ultimo.getLexema() + "' y '" + token.getLexema() + "'");
                    return recuperar(analizador, nodo, ultimo);
                }
            }
            nodo.agregarToken(token);
            ultimo = token;
            analizador.avanzar();
        }
        return terminar(analizador, nodo, palabraEcho, esperoValor, ultimo, false, profundidad);
    }

    /**
     * Recuperación después de un error en la expresión (modo pánico): el
     * resto de la sentencia ya no se revisa token por token, para no repetir
     * el mismo problema. Solo se busca el ';' y, si en ese resto hay una
     * cadena sin cerrar, se reporta como error del echo (803/804).
     */
    private NodoEcho recuperar(AnalizadorSintactico analizador, NodoEcho nodo, Token ultimo) {
        while (!analizador.fin()) {
            Token token = analizador.actual();
            if (AnalizadorSintactico.es(token, ";")) {
                analizador.avanzar();
                return nodo;
            }
            if (AnalizadorSintactico.es(token, "{") || AnalizadorSintactico.es(token, "}")
                    || token.es(TipoToken.ETIQUETA_CIERRE) || token.getLinea() != ultimo.getLinea()) {
                break;
            }
            nodo.agregarToken(token);
            analizador.avanzar();
            if (token.es(TipoToken.CADENA) && !Patrones.CADENA.matcher(token.getLexema()).matches()) {
                reportarComillaAbierta(analizador, token);
                return nodo;
            }
            ultimo = token;
        }
        analizador.registrar(CodigoError.ECHO_SIN_PUNTO_Y_COMA, ultimo);
        return nodo;
    }

    /**
     * Revisa cómo terminó la expresión: vacía (801), con un operador colgando
     * o un paréntesis sin cerrar (806) o sin ';' (805).
     */
    private NodoEcho terminar(AnalizadorSintactico analizador, NodoEcho nodo, Token palabraEcho,
                              boolean esperoValor, Token ultimo, boolean tienePuntoYComa, int profundidad) {
        if (nodo.getTokens().isEmpty()) {
            analizador.registrar(CodigoError.ECHO_SIN_EXPRESION, palabraEcho);
        } else if (esperoValor) {
            analizador.registrar(CodigoError.ECHO_EXPRESION_INVALIDA, ultimo,
                    "el operador '" + ultimo.getLexema() + "' no tiene un valor a la derecha");
        } else if (profundidad > 0) {
            analizador.registrar(CodigoError.ECHO_EXPRESION_INVALIDA, ultimo,
                    "falta el paréntesis de cierre ')'");
        }
        if (!tienePuntoYComa) {
            analizador.registrar(CodigoError.ECHO_SIN_PUNTO_Y_COMA, ultimo);
        }
        return nodo;
    }

    /**
     * Si falta el ';', el echo termina antes de una llave, de '?>', o de una
     * línea nueva que no continúa la expresión (no empieza con un operador).
     */
    private boolean terminaSinPuntoYComa(Token token, Token ultimo, boolean esperoValor, int profundidad) {
        if (AnalizadorSintactico.es(token, "{") || AnalizadorSintactico.es(token, "}")
                || token.es(TipoToken.ETIQUETA_CIERRE) || token.es(TipoToken.ETIQUETA_APERTURA)) {
            return true;
        }
        boolean lineaNueva = token.getLinea() != ultimo.getLinea();
        boolean continuaLaExpresion = esperoValor || profundidad > 0
                || esOperador(token) || AnalizadorSintactico.es(token, ",");
        return lineaNueva && !continuaLaExpresion;
    }

    /** Error 803 o 804, en lugar del 502 general que ya registró la etapa 4. */
    private void reportarComillaAbierta(AnalizadorSintactico analizador, Token cadena) {
        analizador.getErrores().quitar(CodigoError.CADENA_SIN_CERRAR, cadena.getLinea(), cadena.getColumna());
        CodigoError codigo = cadena.getLexema().startsWith("\"")
                ? CodigoError.ECHO_COMILLA_DOBLE_ABIERTA : CodigoError.ECHO_COMILLA_SIMPLE_ABIERTA;
        analizador.registrar(codigo, cadena, cadena.getLexema().trim());
    }

    /** Un valor: número, $variable, nombre de constante o función, true/false/null. */
    private boolean esValor(Token token) {
        return token.es(TipoToken.NUMERO) || token.es(TipoToken.IDENTIFICADOR) || token.es(TipoToken.PALABRA)
                || (token.es(TipoToken.PALABRA_RESERVADA)
                        && VALORES_RESERVADOS.contains(token.getLexema().toLowerCase(Locale.ROOT)));
    }

    /**
     * Una palabra reservada seguida de '(' es una llamada a función: por
     * ejemplo trim, que está en la lista de palabras reservadas del enunciado.
     */
    private boolean esLlamadaAFuncion(AnalizadorSintactico analizador, Token token) {
        return token.es(TipoToken.PALABRA_RESERVADA) && AnalizadorSintactico.es(analizador.siguiente(), "(");
    }

    /** Signo delante de un valor: -5, !$activo, ++$i. */
    private boolean esSigno(Token token) {
        return AnalizadorSintactico.es(token, "-") || AnalizadorSintactico.es(token, "+")
                || AnalizadorSintactico.es(token, "!") || AnalizadorSintactico.es(token, "++")
                || AnalizadorSintactico.es(token, "--");
    }

    /** @return true si el token une dos valores (and, or... sin distinguir mayúsculas). */
    private boolean esOperador(Token token) {
        return OPERADORES.contains(token.getLexema().toLowerCase(Locale.ROOT));
    }

    /**
     * Después de "->" o "::" va el nombre de la propiedad, el método o la
     * constante, aunque sea una palabra reservada ($pedido->print).
     */
    private boolean esMiembro(Token anterior, Token token) {
        return (AnalizadorSintactico.es(anterior, "->") || AnalizadorSintactico.es(anterior, "::"))
                && (token.es(TipoToken.PALABRA) || token.es(TipoToken.PALABRA_RESERVADA));
    }

    /** "(int)", "(string)"...: un '(' seguido de un tipo y de ')'. */
    private boolean esConversion(AnalizadorSintactico analizador, Token token) {
        Token tipo = analizador.adelante(1);
        return AnalizadorSintactico.es(token, "(") && tipo != null
                && CONVERSIONES.contains(tipo.getLexema().toLowerCase(Locale.ROOT))
                && AnalizadorSintactico.es(analizador.adelante(2), ")");
    }

    /** Toma "( tipo )" de una conversión. @return el ')' */
    private Token consumirConversion(AnalizadorSintactico analizador, NodoEcho nodo) {
        Token ultimo = null;
        for (int i = 0; i < 3; i++) {
            ultimo = analizador.actual();
            nodo.agregarToken(ultimo);
            analizador.avanzar();
        }
        return ultimo;
    }

    /** @return true si al valor le sigue '(' (llamada a función) o '[' (posición de un arreglo). */
    private boolean sigueGrupo(AnalizadorSintactico analizador, Token valor) {
        Token siguiente = analizador.siguiente();
        // $funcion($x) también es una llamada: el nombre de la función está en una variable
        return ((valor.es(TipoToken.PALABRA) || valor.es(TipoToken.PALABRA_RESERVADA)
                        || valor.es(TipoToken.IDENTIFICADOR)) && AnalizadorSintactico.es(siguiente, "("))
                || (valor.es(TipoToken.IDENTIFICADOR) && AnalizadorSintactico.es(siguiente, "["));
    }

    /** Toma los argumentos de una función o el índice de un arreglo, con sus paréntesis o corchetes. */
    private Token consumirGrupo(AnalizadorSintactico analizador, NodoEcho nodo) {
        Token apertura = analizador.actual();
        String cierre = AnalizadorSintactico.es(apertura, "(") ? ")" : "]";
        nodo.agregarToken(apertura);
        analizador.avanzar();
        return consumirHasta(analizador, nodo, apertura.getLexema(), cierre);
    }

    /**
     * Toma tokens hasta el cierre que corresponde (contando aperturas
     * internas), sin pasar de un ';' o una llave.
     *
     * @return el último token tomado
     */
    private Token consumirHasta(AnalizadorSintactico analizador, NodoEcho nodo, String apertura, String cierre) {
        int profundidad = 1;
        Token ultimo = analizador.anterior();
        while (!analizador.fin() && profundidad > 0) {
            Token token = analizador.actual();
            if (AnalizadorSintactico.es(token, ";") || AnalizadorSintactico.es(token, "{")
                    || AnalizadorSintactico.es(token, "}")) {
                break;
            }
            if (AnalizadorSintactico.es(token, apertura)) {
                profundidad++;
            } else if (AnalizadorSintactico.es(token, cierre)) {
                profundidad--;
            }
            nodo.agregarToken(token);
            ultimo = token;
            analizador.avanzar();
        }
        // Después de "funcion(...)" o "$arreglo[...]" puede venir otro índice: $tabla[1][2]
        if (!analizador.fin() && AnalizadorSintactico.es(analizador.actual(), "[")) {
            nodo.agregarToken(analizador.actual());
            analizador.avanzar();
            return consumirHasta(analizador, nodo, "[", "]");
        }
        return ultimo;
    }
}
