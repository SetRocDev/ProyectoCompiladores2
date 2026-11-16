package proyectocompiladores2.semantico;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.Nodo;
import proyectocompiladores2.sintactico.arbol.NodoCabeceraFor;
import proyectocompiladores2.sintactico.arbol.NodoCondicion;
import proyectocompiladores2.sintactico.arbol.NodoEstructura;
import proyectocompiladores2.sintactico.arbol.NodoIf;
import proyectocompiladores2.sintactico.arbol.NodoWhile;

/**
 * ===== ETAPA 6: ANÁLISIS SEMÁNTICO =====
 *
 * El análisis sintáctico revisa que el código esté BIEN ESCRITO (el orden de
 * los tokens); el semántico revisa que TENGA SENTIDO. Recorre el árbol
 * sintáctico en el orden del programa, con una tabla de símbolos, y
 * verifica (decisión Q11):
 *
 *   901 - Uso correcto de los identificadores: una variable no se puede usar
 *         antes de que reciba un valor. Se revisan también las variables
 *         dentro de cadenas con comillas dobles ("Promedio: $promedio"),
 *         porque PHP las reemplaza por su valor. Si existe una variable
 *         parecida se sugiere ($Total → ¿quiso decir $total?).
 *   902 - Coherencia de las instrucciones: la condición de un while o un if
 *         que ASIGNA un valor fijo ($x = 5) en lugar de compararlo ($x == 5)
 *         siempre da el mismo resultado; casi siempre es un '==' mal escrito.
 *   903 - Consistencia del programa: una división (o módulo) entre el
 *         número 0 escrito en el código detiene el programa en PHP 8.
 *
 * Una regla de "posible ciclo infinito" se descartó: el enunciado muestra
 * "while ($x < 10) { echo $x; }" como ejemplo PERMITIDO y esa regla lo
 * habría marcado como error.
 *
 * Para no repetir mensajes, cada variable se reporta una sola vez (en su
 * primer uso), y no se reporta si su asignación tiene un error léxico (por
 * ejemplo $1nombre = ...; ese error ya lo explica la etapa 4).
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class AnalizadorSemantico {

    /** Valores fijos que no son números ni cadenas. */
    private static final Set<String> VALORES_FIJOS = Patrones.conjunto("true", "false", "null");

    /** Lo que puede seguir al valor en una condición: el final, ')' o un operador lógico. */
    private static final Set<String> FIN_DE_CONDICION = Patrones.conjunto(")", "&&", "||", "and", "or", "xor");

    /** Variable dentro de una cadena con comillas dobles (sin contar "\$"). */
    private static final Pattern VARIABLE_EN_CADENA = Pattern.compile("(?<!\\\\)\\$[A-Za-z_][A-Za-z0-9_]*");

    private final GestorErrores errores;
    private final TablaSimbolos tabla = new TablaSimbolos();
    private final Set<String> yaReportadas = new HashSet<>();
    private final List<String> asignacionesConErrorLexico = new ArrayList<>();

    public AnalizadorSemantico(GestorErrores errores) {
        this.errores = errores;
    }

    /**
     * @param raiz árbol sintáctico del programa (etapa 5)
     */
    public void analizar(Nodo raiz) {
        recolectarAsignacionesConError(raiz);
        visitar(raiz);
    }

    /** @return la tabla de símbolos al terminar el análisis. */
    public TablaSimbolos getTablaSimbolos() {
        return tabla;
    }

    // =====================================================================
    //  Recorrido del árbol
    // =====================================================================

    /** Recorre el árbol en el orden del programa: primero los tokens del nodo y luego sus hijos. */
    private void visitar(Nodo nodo) {
        revisarDivisionEntreCero(nodo.getTokens());
        if (nodo instanceof NodoWhile || nodo instanceof NodoIf) {
            revisarAsignacionEnCondicion(nodo);
        }
        if (nodo instanceof NodoEstructura && esDeclaracionDeFuncion((NodoEstructura) nodo)) {
            // function nombre($a, $b): los parámetros reciben su valor al llamar la función
            for (Token token : nodo.getTokens()) {
                if (esVariable(token)) {
                    tabla.asignar(token.getLexema(), token.getLinea());
                }
            }
        } else {
            // Sentencias, echo, condiciones y la cabecera del for. La cabecera
            // tiene tres partes separadas por ';', y cada parte puede tener
            // varias expresiones separadas por ',' (for ($i = 0, $j = 9; ...)).
            // En una condición también puede haber una asignación:
            // en "while ($fila = leer())" $fila recibe un valor, no se usa.
            boolean separarComas = nodo instanceof NodoCabeceraFor;
            for (List<Token> parte : separarEnPartes(nodo.getTokens(), separarComas)) {
                revisarSentencia(parte);
            }
        }
        for (Nodo hijo : nodo.getHijos()) {
            visitar(hijo);
        }
    }

    /**
     * En una sentencia, primero se usan los valores de la derecha y después
     * se asigna la variable de la izquierda: en "$x = $x + 1" el $x de la
     * derecha debe tener valor antes.
     */
    private void revisarSentencia(List<Token> tokens) {
        List<Token> asignadas = variablesAsignadas(tokens);
        List<Token> usadas = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (esRevisionDeExistencia(tokens, i)) {
                // isset($x) y empty($x) sirven justamente para preguntar si $x tiene valor
                i = saltarParentesis(tokens, i + 1);
                continue;
            }
            if (esVariable(token) && (!asignadas.contains(token) || esAsignacionCompuesta(tokens, i))) {
                usadas.add(token);
            } else if (esCadenaConVariables(token)) {
                usadas.add(token);
            }
        }
        revisarUsos(usadas);
        for (Token token : asignadas) {
            tabla.asignar(token.getLexema(), token.getLinea());
        }
    }

    /** 901: cada variable usada debe tener un valor asignado antes. */
    private void revisarUsos(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (esRevisionDeExistencia(tokens, i)) {
                i = saltarParentesis(tokens, i + 1);
                continue;
            }
            if (esVariable(token)) {
                revisarUso(token.getLexema(), token);
            } else if (esCadenaConVariables(token)) {
                Matcher variables = VARIABLE_EN_CADENA.matcher(token.getLexema());
                while (variables.find()) {
                    revisarUso(variables.group(), token);
                }
            }
        }
    }

    private void revisarUso(String variable, Token donde) {
        if (tabla.estaAsignada(variable) || yaReportadas.contains(variable) || tieneAsignacionConError(variable)) {
            return;
        }
        yaReportadas.add(variable);
        String parecida = tabla.parecida(variable);
        String sugerencia = parecida == null ? "" : "; ¿quiso decir '" + parecida + "'?";
        errores.registrar(CodigoError.VARIABLE_NO_ASIGNADA, donde.getLinea(), donde.getColumna(), variable, sugerencia);
    }


    /**
     * 902: "while ($x = 5)" o "if ($estado = true)": la condición le asigna
     * un valor fijo a la variable, así que siempre da el mismo resultado.
     * Solo se reporta con un valor fijo a la derecha (número, texto, true,
     * false o null); "while ($fila = siguiente())" es una forma común de PHP.
     */
    private void revisarAsignacionEnCondicion(Nodo estructura) {
        if (estructura.getHijos().isEmpty() || !(estructura.getHijos().get(0) instanceof NodoCondicion)) {
            return;
        }
        List<Token> condicion = estructura.getHijos().get(0).getTokens();
        String nombre = estructura instanceof NodoWhile ? "while" : "if";
        for (int i = 1; i + 1 < condicion.size(); i++) {
            Token igual = condicion.get(i);
            Token valor = condicion.get(i + 1);
            boolean valorTermina = i + 2 >= condicion.size()
                    || FIN_DE_CONDICION.contains(condicion.get(i + 2).getLexema().toLowerCase(Locale.ROOT));
            if (igual.es(TipoToken.ASIGNACION) && esValorFijo(valor) && valorTermina) {
                errores.registrar(CodigoError.ASIGNACION_EN_CONDICION, igual.getLinea(), igual.getColumna(),
                        nombre, condicion.get(i - 1).getLexema(), valor.getLexema());
            }
        }
    }

    /** 903: "/ 0", "% 0" o "/= 0" con el número 0 escrito en el código. */
    private void revisarDivisionEntreCero(List<Token> tokens) {
        for (int i = 0; i + 1 < tokens.size(); i++) {
            String operador = tokens.get(i).getLexema();
            Token divisor = tokens.get(i + 1);
            boolean esDivision = operador.equals("/") || operador.equals("%") || operador.equals("/=");
            if (esDivision && divisor.es(TipoToken.NUMERO) && esCero(divisor.getLexema())) {
                errores.registrar(CodigoError.DIVISION_ENTRE_CERO, tokens.get(i).getLinea(),
                        tokens.get(i).getColumna(), operador);
            }
        }
    }

    private boolean esValorFijo(Token token) {
        return token.es(TipoToken.NUMERO) || token.es(TipoToken.CADENA)
                || VALORES_FIJOS.contains(token.getLexema().toLowerCase(Locale.ROOT));
    }

    /** @return true si el número es cero: 0, 00, 0.0 */
    private boolean esCero(String numero) {
        return numero.replace("0", "").replace(".", "").isEmpty();
    }

    // =====================================================================
    //  Ayudas
    // =====================================================================

    /**
     * Variables que reciben un valor en la sentencia: a la izquierda de '=' o
     * de una asignación compuesta (+=, .=...), las de ++ y --, las de un
     * foreach (as $v, as $k => $v) y las declaradas con global o static.
     */
    private List<Token> variablesAsignadas(List<Token> tokens) {
        List<Token> asignadas = new ArrayList<>();
        boolean despuesDeAs = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String lexema = token.getLexema().toLowerCase(Locale.ROOT);
            if (lexema.equals("as")) {
                despuesDeAs = true;
            }
            if (!esVariable(token)) {
                continue;
            }
            String anterior = i > 0 ? tokens.get(i - 1).getLexema().toLowerCase(Locale.ROOT) : "";
            String siguiente = siguienteSinIndices(tokens, i);
            if (Patrones.esOperadorDeAsignacion(siguiente) || siguiente.equals("++") || siguiente.equals("--")
                    || anterior.equals("++") || anterior.equals("--") || despuesDeAs
                    || anterior.equals("global") || anterior.equals("static")) {
                asignadas.add(token);
            }
        }
        return asignadas;
    }

    /** @return true si en esa posición empieza isset( o empty( */
    private boolean esRevisionDeExistencia(List<Token> tokens, int i) {
        String lexema = tokens.get(i).getLexema().toLowerCase(Locale.ROOT);
        return (lexema.equals("isset") || lexema.equals("empty"))
                && i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(");
    }

    /** @return la posición del ')' que cierra el '(' de la posición indicada. */
    private int saltarParentesis(List<Token> tokens, int apertura) {
        int profundidad = 0;
        for (int j = apertura; j < tokens.size(); j++) {
            if (tokens.get(j).getLexema().equals("(")) {
                profundidad++;
            } else if (tokens.get(j).getLexema().equals(")") && --profundidad == 0) {
                return j;
            }
        }
        return tokens.size() - 1;
    }

    /** "$suma += 5": $suma recibe un valor pero también se usa el que tenía. */
    private boolean esAsignacionCompuesta(List<Token> tokens, int i) {
        String siguiente = siguienteSinIndices(tokens, i);
        return Patrones.ASIGNACIONES_COMPUESTAS.contains(siguiente) || siguiente.equals("++")
                || siguiente.equals("--") || (i > 0 && (tokens.get(i - 1).getLexema().equals("++")
                || tokens.get(i - 1).getLexema().equals("--")));
    }

    /** @return el lexema que sigue a la variable, saltando índices como [0] o [$i]. */
    private String siguienteSinIndices(List<Token> tokens, int i) {
        int j = i + 1;
        while (j < tokens.size() && tokens.get(j).getLexema().equals("[")) {
            int profundidad = 0;
            do {
                if (tokens.get(j).getLexema().equals("[")) {
                    profundidad++;
                } else if (tokens.get(j).getLexema().equals("]")) {
                    profundidad--;
                }
                j++;
            } while (j < tokens.size() && profundidad > 0);
        }
        return j < tokens.size() ? tokens.get(j).getLexema() : "";
    }



    /**
     * Las asignaciones cuya variable tiene un error léxico ($1nombre, $if,
     * nombre sin $) se guardan para no reportar en cascada la variable que
     * probablemente se quiso escribir.
     */
    private void recolectarAsignacionesConError(Nodo nodo) {
        List<Token> tokens = nodo.getTokens();
        for (int i = 0; i + 1 < tokens.size(); i++) {
            Token token = tokens.get(i);
            boolean esNombre = token.es(TipoToken.IDENTIFICADOR) || token.es(TipoToken.PALABRA)
                    || token.es(TipoToken.PALABRA_RESERVADA);
            if (esNombre && !esVariable(token) && Patrones.esOperadorDeAsignacion(siguienteSinIndices(tokens, i))) {
                String nombre = token.getLexema().startsWith("$") ? token.getLexema() : "$" + token.getLexema();
                asignacionesConErrorLexico.add(nombre);
            }
        }
        for (Nodo hijo : nodo.getHijos()) {
            recolectarAsignacionesConError(hijo);
        }
    }

    /** @return true si la variable se parece a una asignación con error léxico (la explica la etapa 4). */
    private boolean tieneAsignacionConError(String variable) {
        for (String conError : asignacionesConErrorLexico) {
            // Una sola letra de diferencia: $1nombre / $nombre, $-dato / $dato, $suma@notas / $suma_notas
            if (Patrones.distanciaDeEdicion(conError, variable) <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Una variable válida que no es palabra reservada. Las mal escritas
     * ($1abc) o reservadas ($if) son errores léxicos de la etapa 4.
     */
    private boolean esVariable(Token token) {
        return token.es(TipoToken.IDENTIFICADOR) && Patrones.IDENTIFICADOR.matcher(token.getLexema()).matches()
                && !Patrones.esPalabraReservada(token.getLexema().substring(1));
    }

    /** Solo las cadenas con comillas dobles reemplazan variables. */
    private boolean esCadenaConVariables(Token token) {
        return token.es(TipoToken.CADENA) && token.getLexema().startsWith("\"");
    }

    private boolean esDeclaracionDeFuncion(NodoEstructura estructura) {
        String palabra = estructura.getPalabraClave();
        return palabra.equals("function") || palabra.equals("catch");
    }

    /**
     * Separa los tokens de un nodo en partes por ';' (la cabecera de un for
     * tiene tres) y, si se indica, también por ',' fuera de paréntesis y
     * corchetes (en "array(1, 2)" la coma no separa expresiones).
     */
    private List<List<Token>> separarEnPartes(List<Token> tokens, boolean tambienPorComas) {
        List<List<Token>> partes = new ArrayList<>();
        List<Token> actual = new ArrayList<>();
        int profundidad = 0;
        for (Token token : tokens) {
            String lexema = token.getLexema();
            if (lexema.equals("(") || lexema.equals("[")) {
                profundidad++;
            } else if ((lexema.equals(")") || lexema.equals("]")) && profundidad > 0) {
                profundidad--;
            }
            boolean separa = lexema.equals(";") || (tambienPorComas && lexema.equals(",") && profundidad == 0);
            if (separa) {
                partes.add(actual);
                actual = new ArrayList<>();
            } else {
                actual.add(token);
            }
        }
        partes.add(actual);
        return partes;
    }
}
