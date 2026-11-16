package proyectocompiladores2.sintactico;

import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.NodoWhile;

/**
 * R3 del Proyecto 2 - Comando while.
 *
 *     while → 'while' '(' condicion ')' '{' sentencia { sentencia } '}'
 *
 * Los tres aspectos del enunciado:
 *   1. Estructura sintáctica: palabra reservada while, paréntesis de
 *      apertura (601) y cierre (602), condición presente (603), llave de
 *      apertura (604, en la misma línea: 610) y de cierre (605).
 *   2. Contenido del bloque: al menos una línea de código (606).
 *   3. Indentación: 4 espacios por nivel (607), sin tabulaciones (608) y la
 *      '}' alineada con el while (609).
 *
 * La cabecera y el cuerpo los revisa la clase base, ValidadorEstructuraControl,
 * con los códigos 6xx de TipoBloque.WHILE.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorWhile extends ValidadorEstructuraControl {

    /**
     * @param analizador el analizador sintáctico, con el token actual en 'while'
     * @return el nodo del while (condición y bloque)
     */
    public NodoWhile analizar(AnalizadorSintactico analizador) {
        Token palabraWhile = analizador.actual();
        NodoWhile nodo = new NodoWhile(palabraWhile.getLinea());
        nodo.agregarHijo(analizarCabecera(analizador, TipoBloque.WHILE));
        nodo.agregarHijo(analizarCuerpo(analizador, TipoBloque.WHILE, "while", palabraWhile));
        return nodo;
    }
}
