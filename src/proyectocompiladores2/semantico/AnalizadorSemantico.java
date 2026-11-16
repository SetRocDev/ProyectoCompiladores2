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
import proyectocompiladores2.sintactico.arbol.NodoCondicion;
import proyectocompiladores2.sintactico.arbol.NodoEstructura;

/**
 * ===== ETAPA 6: ANÁLISIS SEMÁNTICO =====
 *
 * El análisis sintáctico revisa que el código esté BIEN ESCRITO (el orden de
 * los tokens); el semántico revisa que TENGA SENTIDO. Recorre el árbol
 * sintáctico en el orden del programa, con una tabla de símbolos, y
 * verifica (decisión Q11) el uso correcto de los identificadores:
 *
 *   901 - Una variable no se puede usar antes de que reciba un valor. Se
 *         revisan también las variables dentro de cadenas con comillas
 *         dobles ("Promedio: $promedio"), porque PHP las reemplaza por su
 *         valor. Si existe una variable parecida se sugiere
 *         ($Total → ¿quiso decir $total?).
 *
 * La regla 902 (posible ciclo infinito) se retiró: el enunciado muestra
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

    /** Variable dentro de una cadena con comillas dobles (sin contar "\$"). */
    private static final Pattern VARIABLE_EN_CADENA = Pattern.compile("(?<!\\\\)\\$[A-Za-z_][A-Za-z0-9_]*");

    private final GestorErrores errores;
    private final TablaSimbolos tabla = new TablaSimbolos();
    private final Set<String> yaReportadas = new HashSet<>();
    private final List<String> asignacionesConErrorCompilacion = new ArrayList<>();

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
        if (nodo instanceof NodoCondicion) {
            revisarUsos(nodo.getTokens());
        } else if (nodo instanceof NodoEstructura && esDeclaracionDeFuncion((NodoEstructura) nodo)) {
            // function nombre($a, $b): los parámetros reciben su valor al llamar la función
            for (Token token : nodo.getTokens()) {
                if (esVariable(token)) {
                    tabla.asignar(token.getLexema(), token.getLinea());
                }
            }
        } else {
            // Sentencias, echo y cabeceras (for): pueden tener varias partes separadas por ';'
            for (List<Token> parte : separarPorPuntoYComa(nodo.getTokens())) {
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
                asignacionesConErrorCompilacion.add(nombre);
            }
        }
        for (Nodo hijo : nodo.getHijos()) {
            recolectarAsignacionesConError(hijo);
        }
    }

    /** @return true si la variable se parece a una asignación con error léxico (la explica la etapa 4). */
    private boolean tieneAsignacionConError(String variable) {
        for (String conError : asignacionesConErrorCompilacion) {
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

    /** Separa los tokens de un nodo en partes por ';' (la cabecera de un for tiene tres). */
    private List<List<Token>> separarPorPuntoYComa(List<Token> tokens) {
        List<List<Token>> partes = new ArrayList<>();
        List<Token> actual = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getLexema().equals(";")) {
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
