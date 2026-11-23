# Script pour arrêter tous les processus microservices Med-RDV
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -match "medrdv" -or $_.CommandLine -match "eureka" -or $_.CommandLine -match "config-server" -or $_.CommandLine -match "service-" -or $_.CommandLine -match "api-gateway"
} | Stop-Process -Force

Write-Host "Services Med-RDV arrêtés." -ForegroundColor Yellow
