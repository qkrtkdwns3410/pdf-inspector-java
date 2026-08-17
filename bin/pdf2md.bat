@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
set "GRADLE_JAR=%PROJECT_DIR%\build\libs\pdf-inspector-java-0.1.0-SNAPSHOT-all.jar"

if exist "%GRADLE_JAR%" (
  set "JAR=%GRADLE_JAR%"
) else (
  echo Build the executable first: gradlew.bat build 1>&2
  exit /b 1
)

java -jar "%JAR%" %*
