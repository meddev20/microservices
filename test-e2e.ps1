# Script de Test End-to-End pour Med-RDV (via API Gateway port 8080)

$GATEWAY = "http://localhost:8080"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "       🏥 Med-RDV — Scénario E2E Complet     " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# 1. Création d'un praticien
Write-Host "`n1. Création d'un Médecin..." -ForegroundColor Yellow
$doctorPayload = @{
    nom = "Dr. Martin"
    specialite = "Cardiologie"
    telephone = "0102030405"
    email = "dr.martin@medrdv.fr"
} | ConvertTo-Json

$doctor = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors" -Method Post -Body $doctorPayload -ContentType "application/json"
$doctorId = $doctor.id
Write-Host "✓ Praticien créé avec l'ID: $doctorId ($($doctor.nom) - $($doctor.specialite))" -ForegroundColor Green

# 2. Création d'un créneau disponible
Write-Host "`n2. Création d'un créneau disponible..." -ForegroundColor Yellow
$slotPayload = @{
    doctorId = $doctorId
    dateHeure = "2026-11-15T14:30:00"
    dureeMinutes = 30
    statut = "DISPONIBLE"
} | ConvertTo-Json

$slot = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities" -Method Post -Body $slotPayload -ContentType "application/json"
$slotId = $slot.id
Write-Host "✓ Créneau créé avec l'ID: $slotId (Date: $($slot.dateHeure), Statut: $($slot.statut))" -ForegroundColor Green

# 3. Création d'un patient et de son dossier médical initial
Write-Host "`n3. Création d'un Patient..." -ForegroundColor Yellow
$patientPayload = @{
    nom = "Alice Dupont"
    email = "alice.dupont@test.com"
    dateNaissance = "1990-05-12"
    telephone = "0600000001"
    numeroSecu = "1900575123456"
} | ConvertTo-Json

$patient = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients" -Method Post -Body $patientPayload -ContentType "application/json"
$patientId = $patient.id
Write-Host "✓ Patient créé avec l'ID: $patientId ($($patient.nom))" -ForegroundColor Green

# Dossier médical initial
$recordPayload = @{
    patientId = $patientId
    allergies = "Pénicilline, Arachides"
    antecedents = "Hypertension légère"
} | ConvertTo-Json
$record = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records" -Method Post -Body $recordPayload -ContentType "application/json"
Write-Host "✓ Dossier médical initialisé (Allergies: $($record.allergies))" -ForegroundColor Green

# 4. Prise de rendez-vous (Initie la Saga Kafka)
Write-Host "`n4. Demande de prise de rendez-vous..." -ForegroundColor Yellow
$appointmentPayload = @{
    patientId = $patientId
    doctorId = $doctorId
    availabilityId = $slotId
    motif = "Contrôle annuel et bilan tensionnel"
} | ConvertTo-Json

$appointment = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments" -Method Post -Body $appointmentPayload -ContentType "application/json"
$appointmentId = $appointment.id
Write-Host "✓ Demande de RDV acceptée (ID: $appointmentId, Statut initial: $($appointment.statut))" -ForegroundColor Green

# Pause pour laisser Kafka consommer l'événement et passer à CONFIRME
Write-Host "`nAttente du traitement asynchrone Kafka (2 secondes)..." -ForegroundColor Cyan
Start-Sleep -Seconds 2

# 5. Consultation enrichie (RDV + Patient + Dossier + Praticien + Créneau)
Write-Host "`n5. Récupération de la consultation enrichie..." -ForegroundColor Yellow
$enriched = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments/$appointmentId/enriched" -Method Get
Write-Host "✓ RDV Statut actuel: $($enriched.statut)" -ForegroundColor Green
Write-Host "  - Patient: $($enriched.patient.nom) (Allergies: $($enriched.dossierMedical.allergies))"
Write-Host "  - Praticien: $($enriched.praticien.nom) ($($enriched.praticien.specialite))"
Write-Host "  - Créneau: $($enriched.creneau.dateHeure) (Statut: $($enriched.creneau.statut))"

# 6. Clôture de la consultation (REALISE -> MAJ automatique MedicalRecord)
Write-Host "`n6. Clôture de la consultation par le médecin..." -ForegroundColor Yellow
$completePayload = @{
    notes = "Tension normale 12/8. Pas de traitement requis."
} | ConvertTo-Json

$completed = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments/$appointmentId/complete" -Method Put -Body $completePayload -ContentType "application/json"
Write-Host "✓ Consultation passée au statut: $($completed.statut)" -ForegroundColor Green

Start-Sleep -Seconds 2

# 7. Vérification de la mise à jour du dossier médical
Write-Host "`n7. Vérification de la mise à jour du dossier médical..." -ForegroundColor Yellow
$updatedRecord = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records/patient/$patientId" -Method Get
Write-Host "✓ Dernière consultation: $($updatedRecord.derniereConsultation)" -ForegroundColor Green
Write-Host "✓ Dernier motif: $($updatedRecord.dernierMotif)" -ForegroundColor Green

# 8. Vérification des notifications envoyées
Write-Host "`n8. Historique des notifications..." -ForegroundColor Yellow
$notifications = Invoke-RestMethod -Uri "$GATEWAY/api/v1/notifications" -Method Get
foreach ($notif in $notifications) {
    Write-Host "  - Type: $($notif.type) | À: $($notif.recipient) | Sujet: $($notif.subject)" -ForegroundColor White
}

# 9. Test du Service Batch Bonus : Déclenchement des Rappels J-1
Write-Host "`n9. Exécution du Batch Bonus : Rappels J-1..." -ForegroundColor Yellow
$reminderResult = Invoke-RestMethod -Uri "$GATEWAY/api/v1/batch/reminders/trigger" -Method Post
Write-Host "✓ Statut du Job : $($reminderResult.status) (Rappels traités : $($reminderResult.remindersSent))" -ForegroundColor Green

# 10. Test du Service Batch Bonus : Export CSV des Statistiques par Spécialité
Write-Host "`n10. Récupération du CSV des statistiques par spécialité..." -ForegroundColor Yellow
$csvStats = Invoke-RestMethod -Uri "$GATEWAY/api/v1/batch/stats/csv" -Method Get
Write-Host "✓ Contenu du rapport CSV :" -ForegroundColor Green
Write-Host $csvStats -ForegroundColor Gray

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host "       🎉 Test E2E Terminé avec Succès !       " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

