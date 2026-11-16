#!/usr/bin/env bash
# =============================================================================
#  Batería de pruebas automáticas (Linux / macOS / Git Bash)
#
#  Copia el .jar y todos los casos de "casos/" a una carpeta temporal, ejecuta
#  el programa y compara cada -errores.log generado con su versión esperada de
#  "esperados/", BYTE POR BYTE (incluye espacios y saltos de línea).
#
#  Uso:
#     pruebas/ejecutar-pruebas.sh               usa dist/ProyectoCompiladores2.jar
#     pruebas/ejecutar-pruebas.sh -v            además muestra las diferencias
#     pruebas/ejecutar-pruebas.sh --actualizar  copia los .log generados a "esperados/"
#                                               (solo cuando un cambio es intencional
#                                               y se justifica en el Pull Request)
#  Variables opcionales:  JAR=ruta/al.jar   JAVA=ruta/a/java
#
#  Uso de IA: script generado con apoyo de Claude Code y revisado por el estudiante.
# =============================================================================
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="${JAR:-$DIR/../dist/ProyectoCompiladores2.jar}"
JAVA="${JAVA:-java}"
DETALLE=0
ACTUALIZAR=0
for opcion in "$@"; do
    case "$opcion" in
        -v) DETALLE=1 ;;
        --actualizar) ACTUALIZAR=1 ;;
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

if [ "$ACTUALIZAR" -eq 1 ]; then
    rm -f "$DIR"/esperados/*-errores.log
    cp "$TEMPORAL"/*-errores.log "$DIR/esperados/"
    echo "Se actualizaron $(ls "$DIR"/esperados/*-errores.log | wc -l) resultados esperados."
    rm -rf "$TEMPORAL"
    exit 0
fi

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
