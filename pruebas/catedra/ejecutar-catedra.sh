#!/usr/bin/env bash
# =============================================================================
#  Variantes del código de la cátedra (Linux / macOS / Git Bash)
#
#  Copia el .jar y las variantes de "casos/" (código de la cátedra con errores
#  introducidos) a una carpeta temporal, ejecuta el programa y compara cada
#  -errores.log con su versión esperada de "esperados/", BYTE POR BYTE.
#  La lista de cambios y errores esperados está en esperado.txt.
#
#  Uso:
#     pruebas/catedra/ejecutar-catedra.sh       usa dist/ProyectoCompiladores2.jar
#     pruebas/catedra/ejecutar-catedra.sh -v    además muestra las diferencias
#  (Para regenerar la carpeta: pruebas/herramientas/variantes_catedra.py --carpeta)
#  Variables opcionales:  JAR=ruta/al.jar   JAVA=ruta/a/java
#
#  Uso de IA: script generado con apoyo de Claude Code y revisado por el estudiante.
# =============================================================================
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="${JAR:-$DIR/../../dist/ProyectoCompiladores2.jar}"
JAVA="${JAVA:-java}"
DETALLE=0
for opcion in "$@"; do
    case "$opcion" in
        -v) DETALLE=1 ;;
    esac
done

if [ ! -f "$JAR" ]; then
    echo "No se encontró el programa: $JAR (compile primero con Clean and Build)"
    exit 2
fi

TEMPORAL="$(mktemp -d)"
cp "$JAR" "$TEMPORAL/ProyectoCompiladores2.jar"
cp "$DIR"/casos/* "$TEMPORAL/"
(cd "$TEMPORAL" && "$JAVA" -jar ProyectoCompiladores2.jar > consola.txt 2>&1)

correctas=0
fallidas=0
for esperado in "$DIR"/esperados/*-errores.log; do
    nombre="$(basename "$esperado")"
    if cmp -s "$esperado" "$TEMPORAL/$nombre"; then
        correctas=$((correctas + 1))
    else
        fallidas=$((fallidas + 1))
        echo "FALLA  $nombre"
        if [ "$DETALLE" -eq 1 ]; then
            diff "$esperado" "$TEMPORAL/$nombre" | sed 's/^/       /'
        fi
    fi
done

# Un .log que no está en "esperados/" también es una falla (por ejemplo, si se
# procesara un archivo con extensión distinta de .php)
for generado in "$TEMPORAL"/*-errores.log; do
    nombre="$(basename "$generado")"
    if [ ! -e "$DIR/esperados/$nombre" ]; then
        fallidas=$((fallidas + 1))
        echo "FALLA  $nombre (se generó un .log que no se esperaba)"
    fi
done

echo
echo "Pruebas correctas: $correctas   Fallidas: $fallidas"
if [ "$fallidas" -eq 0 ]; then
    rm -rf "$TEMPORAL"
    exit 0
fi
echo "Los resultados generados quedaron en: $TEMPORAL"
exit 1
