@echo off
setlocal
set "MTA_MIGRATE_HOME=%~dp0.."
set "JAR=%~dp0..\migrate-cli\target\quarkus-app\quarkus-run.jar"
if not exist "%JAR%" (
  echo Workbench is not built. Run: mvn clean install
  exit /b 2
)
java -jar "%JAR%" %*
