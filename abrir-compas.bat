@echo off
cd /d "%~dp0"

rem Recompila antes de abrir para que se apliquen las preferencias y cambios de interfaz recientes.
echo Preparando Compas...
call mvn -q -Dmaven.repo.local="%USERPROFILE%\.m2\repository" -DskipTests package
if errorlevel 1 (
  echo No se pudo compilar Compas. Revisa que JDK 21 y Maven esten instalados.
  pause
  exit /b 1
)

rem --- Busca Java 21: 1) JAVA_HOME  2) JDK 21 portable  3) java del PATH ---
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA for /d %%D in ("%LOCALAPPDATA%\Programs\TemurinJDK21\jdk-21*") do set "JAVA=%%D\bin\java.exe"
if not defined JAVA set "JAVA=java"

"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo No se encontro Java 21. Instala JDK 21 o define JAVA_HOME.
  pause
  exit /b 1
)
if not exist "target\classes" (
  echo Falta compilar. Ejecuta primero: mvn package -DskipTests
  pause
  exit /b 1
)

echo Iniciando Compas con Java 21... NO cierres esta ventana.
"%JAVA%" --enable-native-access=ALL-UNNAMED --module-path "target\dependency" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\classes;target\dependency\sqlite-jdbc-3.50.2.0.jar" com.agendapersonal.wkniMain > launch-out.log 2> launch-error.log

echo.
echo La app termino con codigo %ERRORLEVEL%. Revisa launch-error.log si no se abrio.
type launch-error.log
pause
