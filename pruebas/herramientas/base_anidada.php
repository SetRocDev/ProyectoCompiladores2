<?php
// Programa de prueba con estructuras anidadas
$notas = array(70, 85, 90);
$total = 0;
$i = 0;
while ($i < 3) {
    if ($notas[$i] >= 80) {
        echo "Alta: " . $notas[$i] . "\n";
        $total = $total + $notas[$i];
    } elseif ($notas[$i] >= 70) {
        echo "Media\n";
    } else {
        echo "Baja\n";
    }
    $i = $i + 1;
}
if ($total > 0) {
    $j = 0;
    while ($j < 2) {
        if ($j == 1) {
            echo "Fin del ciclo interno\n";
        }
        $j = $j + 1;
    }
} else {
    echo "Sin notas altas\n";
}
for ($k = 0; $k < 2; $k = $k + 1) {
    while ($k < 0) {
        echo "nunca\n";
    }
}
echo "Total: " . $total . "\n";
?>