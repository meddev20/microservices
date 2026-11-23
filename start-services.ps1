$JAVA_BIN = "C:\Program Files\Java\jdk-17.0.5\bin\java.exe"
$LOG_DIR = "$PSScriptRoot\logs"
New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null

function Start-Microservice {
    param (
        [string]$Name,
        [string]$JarPath,
        [int]$Port,
        [int]$WaitSec = 8
    )

    Write-Host "[$Name] Démarrage sur le port $Port..." -ForegroundColor Cyan
    $stdout = "$LOG_DIR\$Name.log"
    $stderr = "$LOG_DIR\$Name-err.log"
    $fullJar = "$PSScriptRoot\$JarPath"

    $proc = Start-Process -FilePath $JAVA_BIN -ArgumentList "-jar `"$fullJar`"" -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    Write-Host "[$Name] Processus lancé (PID: $($proc.Id)) - attente $WaitSec s..." -ForegroundColor Gray
    Start-Sleep -Seconds $WaitSec
}

# 0. Eureka Server
$eurekaCheck = Test-NetConnection -ComputerName localhost -Port 8761 -InformationLevel Quiet
if (-not $eurekaCheck) {
    Start-Microservice -Name "eureka-server" -JarPath "eureka-server\target\eureka-server-1.0.0-SNAPSHOT.jar" -Port 8761 -WaitSec 12
} else {
    Write-Host "[eureka-server] Déjà en cours d'exécution sur le port 8761." -ForegroundColor Green
}

# 1. Config Server
Start-Microservice -Name "config-server" -JarPath "config-server\target\config-server-1.0.0-SNAPSHOT.jar" -Port 8888 -WaitSec 10

# 2. Praticiens Service
Start-Microservice -Name "service-praticiens" -JarPath "service-praticiens\target\service-praticiens-1.0.0-SNAPSHOT.jar" -Port 8081 -WaitSec 10

# 3. Patients Service
Start-Microservice -Name "service-patients" -JarPath "service-patients\target\service-patients-1.0.0-SNAPSHOT.jar" -Port 8082 -WaitSec 10

# 4. Rendez-vous Service
Start-Microservice -Name "service-rendezvous" -JarPath "service-rendezvous\target\service-rendezvous-1.0.0-SNAPSHOT.jar" -Port 8083 -WaitSec 12

# 5. Notification Service
Start-Microservice -Name "service-notification" -JarPath "service-notification\target\service-notification-1.0.0-SNAPSHOT.jar" -Port 8084 -WaitSec 8

# 6. Batch Analytics Service
Start-Microservice -Name "service-batch-analytics" -JarPath "service-batch-analytics\target\service-batch-analytics-1.0.0-SNAPSHOT.jar" -Port 8085 -WaitSec 8

# 7. API Gateway
Start-Microservice -Name "api-gateway" -JarPath "api-gateway\target\api-gateway-1.0.0-SNAPSHOT.jar" -Port 8080 -WaitSec 10

Write-Host "`n✓ Tous les microservices ont été lancés !" -ForegroundColor Green
