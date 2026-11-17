package proyectocompiladores2;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.io.ArchivoFuente;
import proyectocompiladores2.io.EscritorLog;
import proyectocompiladores2.io.LectorArchivoPHP;
import proyectocompiladores2.io.LocalizadorArchivo;
import proyectocompiladores2.lexico.AnalizadorLexico;
import proyectocompiladores2.lexico.ClasificadorTokens;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.lexico.ValidadorLexema;
import proyectocompiladores2.semantico.AnalizadorSemantico;
import proyectocompiladores2.sintactico.AnalizadorSintactico;
import proyectocompiladores2.sintactico.FlujoTokens;
import proyectocompiladores2.sintactico.arbol.NodoPrograma;
import proyectocompiladores2.validacion.ValidadorEtiquetas;

/**
 * Punto de entrada del analizador de PHP (léxico, sintáctico y semántico).
 *
 * Curso: 03307 Compiladores - UNED, III Cuatrimestre 2026.
 * Proyecto 2: continuación del Proyecto 1 (análisis léxico) con análisis
 * sintáctico y semántico.
 *
 * Uso:  java -jar ProyectoCompiladores2.jar           analiza los .php de la carpeta del .jar
 *       java -jar ProyectoCompiladores2.jar -tokens   además muestra los tokens y las tablas de símbolos
 *       java -jar ProyectoCompiladores2.jar -arbol    además muestra el árbol sintáctico
 *
 * Flujo para cada archivo .php:
 *   Lectura         -> LectorArchivoPHP     (R1)
 *   Etapa 1         -> AnalizadorLexico     obtención de tokens
 *   Etapa 2         -> ClasificadorTokens   clasificación de tokens
 *   Etapa 3         -> Patrones             patrón de cada tipo (lo usan las etapas 2 y 4)
 *   Etapa 4         -> ValidadorLexema      validación del lexema (R3, R4, R5 del Proyecto 1)
 *   Etapa 5         -> AnalizadorSintactico análisis sintáctico: while, if/else, echo (Proyecto 2)
 *   Etapa 6         -> AnalizadorSemantico  análisis semántico: variables sin valor y división entre cero (Proyecto 2)
 *   Archivo de log  -> EscritorLog          nombreArchivo-errores.log (R2)
 *
 * Uso de IA: la estructura de esta clase fue generada con apoyo de
 * Claude Code (Anthropic) y revisada por el estudiante.
 */
public class Main {

    public static void main(String[] args) {
        boolean mostrarTokens = tieneOpcion(args, "-tokens");
        boolean mostrarArbol = tieneOpcion(args, "-arbol");

        LocalizadorArchivo localizador = new LocalizadorArchivo();
        Path carpeta = localizador.obtenerCarpetaDeEjecucion();
        imprimir("=== Analizador de PHP: lexico, sintactico y semantico ===");
        imprimir("Carpeta: " + carpeta);

        List<Path> archivos;
        try {
            for (Path ignorado : localizador.buscarArchivosConExtensionInvalida(carpeta)) {
                mostrarError(CodigoError.EXTENSION_INVALIDA, ignorado.getFileName());
            }
            archivos = localizador.buscarArchivosPhp(carpeta);
        } catch (IOException e) {
            mostrarError(CodigoError.ERROR_LECTURA, carpeta, e.getMessage());
            return;
        }

        // Decisión Q1: sin archivos .php se avisa en consola y no se genera log
        if (archivos.isEmpty()) {
            mostrarError(CodigoError.ARCHIVO_NO_ENCONTRADO, carpeta);
            return;
        }

        for (Path ruta : archivos) {
            analizarArchivo(ruta, mostrarTokens, mostrarArbol);
        }
    }

    /** @return true si la opción viene en la línea de comandos (sin importar mayúsculas ni el orden). */
    private static boolean tieneOpcion(String[] args, String opcion) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(opcion)) {
                return true;
            }
        }
        return false;
    }

    /** Ejecuta las 6 etapas sobre un archivo y genera su log. */
    private static void analizarArchivo(Path ruta, boolean mostrarTokens, boolean mostrarArbol) {
        imprimir("");
        imprimir("Archivo: " + ruta.getFileName());
        ArchivoFuente fuente;
        try {
            // Lectura respetando el formato original (R1)
            fuente = new LectorArchivoPHP().leer(ruta);
        } catch (IOException e) {
            mostrarError(CodigoError.ERROR_LECTURA, ruta.getFileName(), e.getMessage());
            return;
        }

        GestorErrores errores = new GestorErrores();
        List<Token> tokens = null;
        ValidadorLexema validador = new ValidadorLexema();
        NodoPrograma arbol = null;
        AnalizadorSemantico semantico = new AnalizadorSemantico(errores);
        try {
            // Etapa 1: obtención de tokens
            tokens = new AnalizadorLexico().obtenerTokens(fuente);

            // Etapa 2: clasificación (usa los patrones de la etapa 3)
            new ClasificadorTokens().clasificar(tokens);

            // Etapa 4: validación de cada lexema según las reglas definidas. Lo que
            // está fuera de las etiquetas <?php y ?> no es código: solo lo
            // revisan las reglas de etiquetas (301, 306, 307)
            List<Token> codigo = ValidadorEtiquetas.codigoPhp(tokens);
            FlujoTokens flujo = new FlujoTokens(codigo);
            validador.validar(tokens, codigo, flujo, fuente, errores);

            // Etapa 5: análisis sintáctico (orden de los tokens según la gramática)
            arbol = new AnalizadorSintactico(flujo, fuente, errores).analizar();

            // Etapa 6: análisis semántico (significado: uso de las variables)
            semantico.analizar(arbol);
        } catch (RuntimeException | StackOverflowError e) {
            // Un fallo inesperado no impide escribir el .log con lo encontrado
            // hasta ahí, ni revisar los demás archivos de la carpeta
            imprimir("  Aviso: el analisis se detuvo antes de terminar (" + e + ")");
        }

        try {
            // Archivo de errores (R2)
            Path log = new EscritorLog().escribir(fuente, errores);
            if (mostrarTokens && tokens != null) {
                mostrarTokens(tokens, validador, semantico);
            }
            if (mostrarArbol && arbol != null) {
                StringBuilder salida = new StringBuilder();
                arbol.imprimir(salida, 1);
                imprimir("  Arbol sintactico (traduccion orientada por la sintaxis):");
                for (String linea : salida.toString().split("\\R")) {
                    imprimir("  " + linea);
                }
            }
            imprimir("  Lineas: " + fuente.getCantidadLineas()
                    + " | Tokens: " + (tokens == null ? 0 : tokens.size()));
            imprimir("  Errores: " + errores.total()
                    + " (lexicos: " + errores.total("Léxico")
                    + ", sintacticos: " + errores.total("Sintáctico")
                    + ", semanticos: " + errores.total("Semántico") + ")");
            imprimir("  Log generado: " + log.getFileName());
        } catch (IOException e) {
            mostrarError(CodigoError.ERROR_ESCRITURA, ruta.getFileName(), e.getMessage());
        }
    }

    /** Opción -tokens: la tabla de tokens y las tablas de símbolos. */
    private static void mostrarTokens(List<Token> tokens, ValidadorLexema validador, AnalizadorSemantico semantico) {
        imprimir("  LINEA:COL TIPO                 LEXEMA");
        for (Token token : tokens) {
            imprimir("  " + token);
        }
        imprimir("  Identificadores encontrados (distingue mayusculas):");
        for (Map.Entry<String, Integer> simbolo : validador.getTablaDeSimbolos().entrySet()) {
            imprimir("    " + simbolo.getKey() + "  (linea " + simbolo.getValue() + ")");
        }
        imprimir("  Tabla de simbolos del analisis semantico (variable: linea donde recibe su valor):");
        for (Map.Entry<String, Integer> simbolo : semantico.getTablaSimbolos().getAsignadas().entrySet()) {
            imprimir("    " + simbolo.getKey() + "  (linea " + simbolo.getValue() + ")");
        }
    }

    private static void mostrarError(CodigoError codigo, Object... datos) {
        imprimir("Error " + codigo.getNumero() + ". " + codigo.describir(datos));
    }

    /**
     * Toda la salida de consola pasa por aquí. Cada consola de Windows (CMD,
     * NetBeans, PowerShell) usa una codificación distinta y las tildes pueden
     * verse como '?'; por eso en consola se muestran sin tildes. El .log sí
     * conserva las tildes porque se escribe con la codificación del .php.
     */
    private static void imprimir(String texto) {
        System.out.println(Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", ""));
    }
}
