@echo off
rem ============================================================
rem  Crea el INSTALADOR de Compas (Compas-1.0.0.exe) usando jpackage + WiX.
rem  Requisitos: JDK 21 completo y WiX Toolset 3.14 (https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm -> wix314.exe).
rem  Antes: en IntelliJ ejecuta una vez Maven -> Lifecycle -> package (o compile).
rem  Resultado:  dist\Compas-1.0.0.exe  (instalador: acceso directo, menu Inicio, desinstalable)
rem ============================================================
cd /d "%~dp0"

rem --- 1) Buscar el JDK 21 (con jpackage) ---
set "JBIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JBIN=%JAVA_HOME%\bin"
if not defined JBIN for /d %%D in ("%USERPROFILE%\.jdks\*21*") do if exist "%%D\bin\jpackage.exe" set "JBIN=%%D\bin"
if not defined JBIN for /d %%D in ("%LOCALAPPDATA%\Programs\TemurinJDK21\jdk-21*") do if exist "%%D\bin\jpackage.exe" set "JBIN=%%D\bin"
if not defined JBIN for /d %%D in ("C:\Program Files\Java\jdk-21*" "C:\Program Files\Eclipse Adoptium\jdk-21*" "C:\Program Files\Microsoft\jdk-21*" "C:\Program Files\Amazon Corretto\jdk21*") do if exist "%%D\bin\jpackage.exe" set "JBIN=%%D\bin"
if not defined JBIN for /d %%D in ("%USERPROFILE%\.jdks\*") do if exist "%%D\bin\jpackage.exe" set "JBIN=%%D\bin"
if not defined JBIN (
  where jpackage >nul 2>&1
  if not errorlevel 1 for /f "delims=" %%P in ('where jpackage') do set "JBIN=%%~dpP"
)
if not defined JBIN (
  echo No se encontro un JDK con jpackage. Instala JDK 21 completo o define JAVA_HOME.
  if not defined CI pause
  exit /b 1
)
echo Usando JDK en: %JBIN%

rem --- WiX Toolset (necesario para crear el instalador) ---
if exist "%ProgramFiles(x86)%\WiX Toolset v3.14\bin\candle.exe" set "PATH=%ProgramFiles(x86)%\WiX Toolset v3.14\bin;%PATH%"
where candle >nul 2>&1
if errorlevel 1 (
  echo Falta WiX Toolset 3.14. Descarga wix314.exe desde:
  echo https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
  echo Instalalo y vuelve a ejecutar este archivo.
  if not defined CI pause
  exit /b 1
)

rem --- 2) Compilar con Maven si existe; si no, usar lo ya compilado por IntelliJ ---
echo [1/3] Compilando el proyecto...
where mvn >nul 2>&1
if not errorlevel 1 (
  call mvn -q -DskipTests package
  if errorlevel 1 (
    echo Maven fallo al compilar.
    if not defined CI pause
    exit /b 1
  )
) else (
  echo Maven no esta en el PATH: se usa target\classes ya compilado por IntelliJ.
)
if not exist target\classes\com\agendapersonal (
  echo Falta compilar. En IntelliJ: panel Maven - Lifecycle - doble clic en "package". Luego vuelve a ejecutar este archivo.
  if not defined CI pause
  exit /b 1
)
if not exist target\dependency (
  echo Falta target\dependency. En IntelliJ: panel Maven - Lifecycle - doble clic en "package".
  if not defined CI pause
  exit /b 1
)

echo [2/3] Preparando archivos de entrada...
if exist target\app-input rmdir /s /q target\app-input
mkdir target\app-input
"%JBIN%\jar.exe" --create --file target\app-input\cronosys-1.0.0.jar -C target\classes .
if errorlevel 1 (
  echo No se pudo crear el jar.
  if not defined CI pause
  exit /b 1
)
copy /y target\dependency\sqlite-jdbc-*.jar target\app-input\ >nul

echo [3/3] Generando el instalador (puede tardar un par de minutos)...
if exist dist rmdir /s /q dist
"%JBIN%\jpackage.exe" --type exe --name Compas --app-version 1.0.0 --vendor "Compas" --description "Compas - agenda personal y docente" --win-menu --win-menu-group "Compas" --win-shortcut --win-dir-chooser --win-per-user-install --win-upgrade-uuid 7f3b2c1e-5a4d-4e8b-9c21-3d6a8b0f1e42 ^
  --input target\app-input --main-jar cronosys-1.0.0.jar --main-class com.agendapersonal.wkniMain ^
  --module-path target\dependency ^
  --add-modules java.se,jdk.unsupported,javafx.controls,javafx.fxml,javafx.media ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --dest dist
if errorlevel 1 (
  echo jpackage fallo. Revisa JDK 21 completo y WiX 3.14 instalado.
  if not defined CI pause
  exit /b 1
)

echo.
echo Listo: dist\Compas-1.0.0.exe  (ejecutalo para instalar Compas)
if not defined CI pause
