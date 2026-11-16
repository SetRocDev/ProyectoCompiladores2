package proyectocompiladores2.validacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.FlujoTokens;

/**
 * R3 - Identificadores ($variable).
 *
 * Como indica el enunciado, la validación se hace ÚNICAMENTE sobre la
 * variable ubicada a la izquierda del operador de asignación (el que recibe
 * el valor). Las variables que se usan a la derecha no se validan aquí.
 *
 * Aspecto 1 (inicio válido): el lexema debe cumplir el patrón
 *   \$[A-Za-z_][A-Za-z0-9_]*  (Patrones.IDENTIFICADOR). Si no lo cumple se
 *   indica la causa exacta: 201 dígito después de '$', 202 símbolo inválido
 *   después de '$', 203 carácter inválido dentro del nombre, 205 '$' solo.
 *   Un nombre sin '$' que recibe un valor es el Error 206.
 * Aspecto 2 (mayúsculas y minúsculas): la tabla de símbolos usa el lexema
 *   exacto como clave, por lo que $total y $Total son dos identificadores
 *   distintos.
 * Aspecto 3 (palabras reservadas): la variable que recibe el valor no puede
 *   ser una palabra reservada: 204. Se compara sin distinguir mayúsculas
 *   (decisión Q5), así que $IF también es error.
 *
 * Correcciones del Proyecto 1:
 *   - T17: la variable y el '=' pueden estar en líneas distintas o tener un
 *     comentario en medio; por eso se trabaja sobre FlujoTokens (tokens sin
 *     comentarios) y no se compara la línea.
 *   - T18: los errores 201, 202, 203 y 205 ya no se reportan para las
 *     variables que están a la derecha del '='.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorIdentificadores {

    /** Tabla de símbolos: identificador exacto -> línea donde aparece por primera vez. */
    private final Map<String, Integer> tablaDeSimbolos = new LinkedHashMap<>();

    public void validar(List<Token> tokens, GestorErrores errores) {
        for (Token token : tokens) {
            if (token.es(TipoToken.IDENTIFICADOR)) {
                tablaDeSimbolos.putIfAbsent(token.getLexema(), token.getLinea());
            }
        }
        FlujoTokens flujo = new FlujoTokens(tokens);
        for (int i = 0; i < flujo.tamano(); i++) {
            if (Patrones.esOperadorDeAsignacion(flujo.get(i).getLexema())) {
                validarLadoIzquierdo(flujo, i, errores);
            }
        }
    }

    /**
     * Revisa lo que está justo antes del operador de asignación (sin importar
     * la línea ni los comentarios):
     *  - un identificador: aspecto 1 (inicio válido) y aspecto 3 (reservada)
     *  - una palabra sin '$' (nombre_estudiante = ...): Error 206
     */
    private void validarLadoIzquierdo(FlujoTokens flujo, int posicionAsignacion, GestorErrores errores) {
        int posicion = saltarIndices(flujo, posicionAsignacion - 1);
        Token izquierdo = posicion >= 0 ? flujo.get(posicion) : null;
        if (izquierdo == null) {
            return;
        }
        if (izquierdo.es(TipoToken.IDENTIFICADOR)) {
            boolean inicioValido = validarInicio(izquierdo, errores);
            String nombre = izquierdo.getLexema().substring(1);
            if (inicioValido && Patrones.esPalabraReservada(nombre)) {
                errores.registrar(CodigoError.ID_PALABRA_RESERVADA, izquierdo.getLinea(), izquierdo.getColumna(),
                        izquierdo.getLexema(), nombre.toLowerCase(Locale.ROOT));
            }
        } else if ((izquierdo.es(TipoToken.PALABRA) || izquierdo.es(TipoToken.PALABRA_RESERVADA))
                && !esPropiedadOConstante(flujo, posicion)) {
            errores.registrar(CodigoError.ID_SIN_SIMBOLO_DOLAR, izquierdo.getLinea(), izquierdo.getColumna(),
                    izquierdo.getLexema());
        }
    }

    /**
     * Si a la izquierda del '=' hay un elemento de arreglo ($notas[0] = 85),
     * la variable que recibe el valor es la que está antes de los corchetes.
     *
     * @param posicion posición del token justo antes del operador de asignación
     * @return posición de la variable, saltando los índices [..] (puede haber
     *         varios, como en $tabla[1][2]); -1 si los corchetes no cierran
     */
    private int saltarIndices(FlujoTokens flujo, int posicion) {
        while (posicion >= 0 && flujo.get(posicion).getLexema().equals("]")) {
            int profundidad = 0;
            do {
                String lexema = flujo.get(posicion).getLexema();
                if (lexema.equals("]")) {
                    profundidad++;
                } else if (lexema.equals("[")) {
                    profundidad--;
                }
                posicion--;
            } while (posicion >= 0 && profundidad > 0);
        }
        return posicion;
    }

    /**
     * Aspecto 1: el identificador debe cumplir el patrón; si no, se indica la causa.
     *
     * @return true si el identificador tiene un inicio válido
     */
    private boolean validarInicio(Token token, GestorErrores errores) {
        String lexema = token.getLexema();
        if (Patrones.IDENTIFICADOR.matcher(lexema).matches()) {
            return true;
        }
        int linea = token.getLinea();
        int columna = token.getColumna();
        if (lexema.length() == 1) {
            errores.registrar(CodigoError.ID_SIN_NOMBRE, linea, columna);
            return false;
        }
        char segundo = lexema.charAt(1);
        if (Character.isDigit(segundo)) {
            errores.registrar(CodigoError.ID_INICIA_CON_DIGITO, linea, columna, lexema);
        } else if (!esLetraOGuionBajo(segundo)) {
            errores.registrar(CodigoError.ID_SIMBOLO_INICIAL_INVALIDO, linea, columna, lexema, segundo);
        } else {
            for (char c : lexema.substring(2).toCharArray()) {
                if (!esLetraOGuionBajo(c) && !(c >= '0' && c <= '9')) {
                    errores.registrar(CodigoError.ID_CARACTER_INVALIDO, linea, columna, lexema, c);
                    break;
                }
            }
        }
        return false;
    }

    /**
     * En PHP no llevan '$' las propiedades ($obj->valor = 1), los miembros
     * estáticos (Clase::VALOR) ni las constantes (const MAX = 10). Por eso no
     * son Error 206.
     */
    private boolean esPropiedadOConstante(FlujoTokens flujo, int posicionPalabra) {
        Token anterior = flujo.anterior(posicionPalabra);
        if (anterior == null) {
            return false;
        }
        String lexema = anterior.getLexema().toLowerCase(Locale.ROOT);
        return lexema.equals("->") || lexema.equals("::") || lexema.equals("const");
    }

    /** Solo letras del alfabeto inglés (a-z, A-Z) o '_', como indica el enunciado. */
    private boolean esLetraOGuionBajo(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /** Aspecto 2: identificadores distintos encontrados (distingue mayúsculas). */
    public Map<String, Integer> getTablaDeSimbolos() {
        return tablaDeSimbolos;
    }
}
