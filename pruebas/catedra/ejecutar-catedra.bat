@echo off
rem ============================================================================
rem  Variantes del codigo de la catedra (Windows, CMD)
rem
rem  Copia el .jar y las variantes de "casos" (codigo de la catedra con errores
rem  introducidos) a una carpeta temporal, ejecuta el programa y compara cada
rem  -errores.log con su version esperada de "esperados", BYTE POR BYTE.
rem  La lista de cambios y errores esperados esta en esperado.txt.
rem
rem  Uso: doble clic sobre este archivo, o desde el CMD:
rem          pruebas\catedra\ejecutar-catedra.bat
rem
rem  Uso de IA: script generado con apoyo de Claude Code y revisado por el estudiante.
rem ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAR=%~dp0..\..\dist\ProyectoCompiladores2.jar"
set "TEMPORAL=%TEMP%\pruebas-catedra-compiladores2"

if not exist "%JAR%" (
    echo No se encontro el programa: %JAR%
    echo Compile primero el proyecto con Clean and Build en NetBeans.
    pause
    exit /b 2
)

if exist "%TEMPORAL%" rmdir /s /q "%TEMPORAL%"
mkdir "%TEMPORAL%"
copy /y "%JAR%" "%TEMPORAL%\ProyectoCompiladores2.jar" >nul
copy /y "casos\*" "%TEMPORAL%" >nul

pushd "%TEMPORAL%"
java -jar ProyectoCompiladores2.jar > consola.txt 2>&1
popd

set /a CORRECTAS=0
set /a FALLIDAS=0
for %%F in (esperados\*-errores.log) do (
    fc /b "%%F" "%TEMPORAL%\%%~nxF" >nul 2>&1
    if errorlevel 1 (
        echo FALLA  %%~nxF
        set /a FALLIDAS+=1
    ) else (
        set /a CORRECTAS+=1
    )
)

rem Un .log que no esta en "esperados" tambien es una falla
for %%G in ("%TEMPORAL%\*-errores.log") do (
    if not exist "esperados\%%~nxG" (
        echo FALLA  %%~nxG  ^(se genero un .log que no se esperaba^)
        set /a FALLIDAS+=1
    )
)

echo.
echo Pruebas correctas: !CORRECTAS!   Fallidas: !FALLIDAS!
if !FALLIDAS! gtr 0 echo Los resultados generados quedaron en: %TEMPORAL%
echo.
pause
if !FALLIDAS! gtr 0 exit /b 1
exit /b 0
