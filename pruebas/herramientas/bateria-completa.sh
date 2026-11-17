#!/usr/bin/env bash
# =============================================================================
#  Verificación completa (Linux / macOS / Git Bash). Necesita Java y Python 3;
#  el contraste con PHP real se omite si no está instalado el comando php.
#
#  Corre, en orden: la batería de casos fijos, las mutaciones tipo tutor
#  (combinaciones de 2 y de 3 errores), la batería extrema, el fuzz con la
#  verificación de R2 y el contraste con PHP. No modifica el proyecto: todo se
#  genera en una carpeta temporal.
#
#  Uso:  pruebas/herramientas/bateria-completa.sh
#
#  Uso de IA: script generado con apoyo de Claude Code y revisado por el estudiante.
# =============================================================================
set -u
H="$(cd "$(dirname "$0")" && pwd)"
RAIZ="$(cd "$H/../.." && pwd)"
JAR="$RAIZ/dist/ProyectoCompiladores2.jar"
BASE="$RAIZ/recursos/CodigoProyecto.php"
TMP="$(mktemp -d)"

echo "== Casos fijos (byte por byte)"
"$RAIZ/pruebas/ejecutar-pruebas.sh" | tail -1
echo "== Mutaciones tipo tutor (combinaciones de 2 errores)"
python3 "$H/mutaciones_tutor.py" "$JAR" "$BASE" "$TMP/tutor2" | tail -1
echo "== Mutaciones tipo tutor (combinaciones de 3 errores)"
python3 "$H/mutaciones_tutor.py" "$JAR" "$BASE" "$TMP/tutor3" 3 600 11 | tail -1
echo "== Bateria extrema"
python3 "$H/extremas.py" "$JAR" "$TMP/extremas" | tail -1
echo "== Fuzz (cambios al azar) y verificacion de R2"
for semilla in 1 2; do
    python3 "$H/fuzz.py" "$semilla" 1000 "$JAR" "$BASE" "$TMP/fuzz$semilla"
    python3 "$H/verificar_r2.py" "$TMP"/fuzz$semilla/*.php | tail -1
done
python3 "$H/fuzz.py" 3 1000 "$JAR" "$H/base_anidada.php" "$TMP/fuzz3"
python3 "$H/verificar_r2.py" "$TMP"/fuzz3/*.php | tail -1
if command -v php >/dev/null 2>&1; then
    echo "== Contraste con PHP real"
    python3 "$H/contraste_php.py"
else
    echo "== Contraste con PHP real: omitido (no se encontro el comando php)"
fi
rm -rf "$TMP"
