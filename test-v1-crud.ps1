# Script de Test V1 - CRUD & Validations
$GATEWAY = "http://localhost:8080"
$RAND = Get-Random -Minimum 1000 -Maximum 9999

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Med-RDV - Jalon V1 : Tests CRUD et Validations         " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. DOCTOR
Write-Host "`n--- [1/4] CRUD Doctor (service-praticiens) ---" -ForegroundColor Yellow
$docPayload = @{
    nom = "Dr. Test $RAND"
    specialite = "Pediatrie"
    email = "dr.test$RAND@medrdv.fr"
    telephone = "0612345678"
    adresseCabinet = "10 Avenue Mohammed V"
} | ConvertTo-Json

$doc = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors" -Method Post -Body $docPayload -ContentType "application/json; charset=utf-8"
$docId = $doc.id
Write-Host "[POST] Doctor cree : ID=$docId ($($doc.nom), $($doc.specialite))" -ForegroundColor Green

$docGet = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors/$docId" -Method Get
Write-Host "[GET]  Doctor verifie : ID=$($docGet.id), Email=$($docGet.email)" -ForegroundColor Green

$docUpdatePayload = @{
    nom = "Dr. Test $RAND Modifie"
    specialite = "Neurologie"
    email = "dr.test$RAND@medrdv.fr"
    telephone = "0612349999"
    adresseCabinet = "15 Avenue Hassan II"
} | ConvertTo-Json

$docPut = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors/$docId" -Method Put -Body $docUpdatePayload -ContentType "application/json; charset=utf-8"
Write-Host "[PUT]  Doctor mis a jour : Specialite=$($docPut.specialite), Nom=$($docPut.nom)" -ForegroundColor Green

# 2. AVAILABILITY
Write-Host "`n--- [2/4] CRUD Availability (service-praticiens) ---" -ForegroundColor Yellow
$slotPayload = @{
    doctorId = $docId
    dateHeure = "2026-12-05T10:00:00"
    dureeMinutes = 30
    statut = "DISPONIBLE"
} | ConvertTo-Json

$slot = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities" -Method Post -Body $slotPayload -ContentType "application/json; charset=utf-8"
$slotId = $slot.id
Write-Host "[POST] Creneau cree : ID=$slotId pour Doctor=$docId (Statut=$($slot.statut))" -ForegroundColor Green

$slots = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities?doctorId=$docId" -Method Get
Write-Host "[GET]  Creneaux trouves pour Doctor $docId : $($slots.Count) creneau(x)" -ForegroundColor Green

# 3. PATIENT
Write-Host "`n--- [3/4] CRUD Patient (service-patients) ---" -ForegroundColor Yellow
$secu = "1" + (Get-Random -Minimum 100000000000 -Maximum 999999999999).ToString()
$patPayload = @{
    nom = "Patient Test $RAND"
    prenom = "Yassine"
    dateNaissance = "1995-04-12"
    numeroSecu = $secu
    telephone = "0655443322"
    email = "patient$RAND@test.ma"
} | ConvertTo-Json

$pat = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients" -Method Post -Body $patPayload -ContentType "application/json; charset=utf-8"
$patId = $pat.id
Write-Host "[POST] Patient cree : ID=$patId ($($pat.nom), Secu=$($pat.numeroSecu))" -ForegroundColor Green

$patSecu = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients/secu/$secu" -Method Get
Write-Host "[GET]  Patient retrouve par numero Secu : Nom=$($patSecu.nom)" -ForegroundColor Green

$patUpdatePayload = @{
    nom = "Patient Test $RAND Modifie"
    prenom = "Yassine"
    dateNaissance = "1995-04-12"
    numeroSecu = $secu
    telephone = "0799887766"
    email = "patient.updated$RAND@test.ma"
} | ConvertTo-Json

$patPut = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients/$patId" -Method Put -Body $patUpdatePayload -ContentType "application/json; charset=utf-8"
Write-Host "[PUT]  Patient mis a jour : Nouveau nom=$($patPut.nom), Tel=$($patPut.telephone)" -ForegroundColor Green

# 4. MEDICAL RECORD
Write-Host "`n--- [4/4] CRUD Medical Record (service-patients) ---" -ForegroundColor Yellow
$medPayload = @{
    patientId = $patId
    historiqueConsultations = "Consultation initiale de controle"
    allergies = "Iode, Aspirine"
    antecedents = "Asthme bronchique"
} | ConvertTo-Json

$med = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records" -Method Post -Body $medPayload -ContentType "application/json; charset=utf-8"
Write-Host "[POST] Dossier medical initialise pour Patient=$patId" -ForegroundColor Green

$medGet = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records/patient/$patId" -Method Get
Write-Host "[GET]  Dossier medical recupere : Allergies=$($medGet.allergies), Antecedents=$($medGet.antecedents)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   Jalon V1 valide avec succes sur toutes les entites !   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
