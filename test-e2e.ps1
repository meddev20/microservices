# Script de Test End-to-End pour Med-RDV (via API Gateway port 8080)

$GATEWAY = "http://localhost:8080"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "       Med-RDV - Scenario E2E Complet         " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# 1. Creation d'un praticien
Write-Host ""
Write-Host "1. Creation d'un Medecin..." -ForegroundColor Yellow
$doctorPayload = @{
    nom = "Dr. Martin"
    specialite = "Cardiologie"
    telephone = "0102030405"
    email = "dr.martin@medrdv.fr"
} | ConvertTo-Json

$doctor = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors" -Method Post -Body $doctorPayload -ContentType "application/json"
$doctorId = $doctor.id
Write-Host "OK - Praticien cree avec l'ID: $doctorId ($($doctor.nom) - $($doctor.specialite))" -ForegroundColor Green

# 2. Creation d'un creneau disponible
Write-Host ""
Write-Host "2. Creation d'un creneau disponible..." -ForegroundColor Yellow
$slotPayload = @{
    doctorId = $doctorId
    dateHeure = "2026-11-23T14:30:00"
    dureeMinutes = 30
    statut = "DISPONIBLE"
} | ConvertTo-Json

$slot = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities" -Method Post -Body $slotPayload -ContentType "application/json"
$slotId = $slot.id
Write-Host "OK - Creneau cree avec l'ID: $slotId (Date: $($slot.dateHeure), Statut: $($slot.statut))" -ForegroundColor Green

# 3. Creation d'un patient et de son dossier medical initial
Write-Host ""
Write-Host "3. Creation d'un Patient..." -ForegroundColor Yellow
$patientPayload = @{
    nom = "Alice Dupont"
    email = "alice.dupont@test.com"
    dateNaissance = "1990-05-12"
    telephone = "0600000001"
    numeroSecu = "1900575123456"
} | ConvertTo-Json

$patient = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients" -Method Post -Body $patientPayload -ContentType "application/json"
$patientId = $patient.id
Write-Host "OK - Patient cree avec l'ID: $patientId ($($patient.nom))" -ForegroundColor Green

# Dossier medical initial
$recordPayload = @{
    patientId = $patientId
    allergies = "Penicilline, Arachides"
    antecedents = "Hypertension legere"
} | ConvertTo-Json
$record = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records" -Method Post -Body $recordPayload -ContentType "application/json"
Write-Host "OK - Dossier medical initialise (Allergies: $($record.allergies))" -ForegroundColor Green

# 4. Prise de rendez-vous (Validation externe & Reservation créneau)
Write-Host ""
Write-Host "4. Demande de prise de rendez-vous..." -ForegroundColor Yellow
$appointmentPayload = @{
    patientId = $patientId
    doctorId = $doctorId
    availabilityId = $slotId
    motif = "Controle annuel et bilan tensionnel"
} | ConvertTo-Json

$appointment = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments" -Method Post -Body $appointmentPayload -ContentType "application/json"
$appointmentId = $appointment.id
Write-Host "OK - Demande de RDV enregistree (ID: $appointmentId, Statut: $($appointment.statut))" -ForegroundColor Green

# Pause de synchronisation
Start-Sleep -Seconds 2

# 5. Consultation enrichie (RDV + Patient + Dossier + Praticien + Creneau)
Write-Host ""
Write-Host "5. Recuperation de la consultation enrichie..." -ForegroundColor Yellow
$enriched = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments/$appointmentId/enriched" -Method Get
Write-Host "OK - RDV Statut actuel: $($enriched.statut)" -ForegroundColor Green
Write-Host "  - Patient: $($enriched.patient.nom) (Allergies: $($enriched.dossierMedical.allergies))"
Write-Host "  - Praticien: $($enriched.praticien.nom) ($($enriched.praticien.specialite))"
Write-Host "  - Creneau: $($enriched.creneau.dateHeure) (Statut: $($enriched.creneau.statut))"

# 6. Cloture de la consultation (REALISE -> MAJ automatique MedicalRecord)
Write-Host ""
Write-Host "6. Cloture de la consultation par le medecin..." -ForegroundColor Yellow
$completePayload = @{
    notes = "Tension normale 12/8. Pas de traitement requis."
} | ConvertTo-Json

$completed = Invoke-RestMethod -Uri "$GATEWAY/api/v1/appointments/$appointmentId/complete" -Method Put -Body $completePayload -ContentType "application/json"
Write-Host "OK - Consultation passee au statut: $($completed.statut)" -ForegroundColor Green

Start-Sleep -Seconds 1

# 7. Verification de la mise a jour du dossier medical
Write-Host ""
Write-Host "7. Verification de la mise a jour du dossier medical..." -ForegroundColor Yellow
$updatedRecord = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records/patient/$patientId" -Method Get
Write-Host "OK - Derniere consultation: $($updatedRecord.derniereConsultation)" -ForegroundColor Green
Write-Host "OK - Dernier motif: $($updatedRecord.dernierMotif)" -ForegroundColor Green

# 8. Verification des notifications envoyees
Write-Host ""
Write-Host "8. Historique des notifications..." -ForegroundColor Yellow
$notifPayload = @{
    recipient = "alice.dupont@test.com"
    subject = "Confirmation de RDV - Dr. Martin"
    content = "Votre rendez-vous a bien ete confirme."
    type = "CONFIRMATION"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$GATEWAY/api/v1/notifications" -Method Post -Body $notifPayload -ContentType "application/json" | Out-Null

$notifications = Invoke-RestMethod -Uri "$GATEWAY/api/v1/notifications" -Method Get
foreach ($notif in $notifications) {
    Write-Host "  - Type: $($notif.type) | A: $($notif.recipient) | Sujet: $($notif.subject)" -ForegroundColor White
}

# 9. Test du Service Batch Bonus : Declenchement des Rappels J-1
Write-Host ""
Write-Host "9. Execution du Batch Bonus : Rappels J-1..." -ForegroundColor Yellow
$reminderResult = Invoke-RestMethod -Uri "$GATEWAY/api/v1/batch/reminders/trigger" -Method Post
Write-Host "OK - Statut du Job : $($reminderResult.status) (Rappels traites : $($reminderResult.remindersSent))" -ForegroundColor Green

# 10. Test du Service Batch Bonus : Export CSV des Statistiques par Specialite
Write-Host ""
Write-Host "10. Recuperation du CSV des statistiques par specialite..." -ForegroundColor Yellow
$csvStats = Invoke-RestMethod -Uri "$GATEWAY/api/v1/batch/stats/csv" -Method Get
Write-Host "OK - Contenu du rapport CSV :" -ForegroundColor Green
Write-Host $csvStats -ForegroundColor Gray

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "         Test E2E Termine avec Succes !       " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
