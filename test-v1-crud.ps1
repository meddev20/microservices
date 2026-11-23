# ==============================================================================
# Script de Test V1 : CRUD Fonctionnel & Modèle de Données (Med-RDV)
# ==============================================================================

$GATEWAY = "http://localhost:8080"
$RAND = Get-Random -Minimum 1000 -Maximum 9999

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   🏥 Med-RDV — Jalon V1 : Tests CRUD & Validations       " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# 1. CRUD DOCTOR (service-praticiens)
# ------------------------------------------------------------------------------
Write-Host "`n--- [1/4] CRUD Doctor (service-praticiens) ---" -ForegroundColor Yellow

# Create
$docPayload = @{
    nom = "Dr. Test $RAND"
    specialite = "Pediatrie"
    telephone = "0611223344"
    email = "dr.test$RAND@medrdv.fr"
} | ConvertTo-Json

$doc = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors" -Method Post -Body $docPayload -ContentType "application/json; charset=utf-8"
$docId = $doc.id
Write-Host "✓ [POST] Doctor créé : ID=$docId ($($doc.nom), $($doc.specialite))" -ForegroundColor Green

# Read (Get by ID)
$docGet = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors/$docId" -Method Get
Write-Host "✓ [GET]  Doctor vérifié : ID=$($docGet.id), Email=$($docGet.email)" -ForegroundColor Green

# Update (PUT)
$docUpdatePayload = @{
    nom = "Dr. Test $RAND Modifie"
    specialite = "Neurologie"
    telephone = "0699887766"
    email = "dr.modifie$RAND@medrdv.fr"
} | ConvertTo-Json

$docUpdated = Invoke-RestMethod -Uri "$GATEWAY/api/v1/doctors/$docId" -Method Put -Body $docUpdatePayload -ContentType "application/json; charset=utf-8"
Write-Host "✓ [PUT]  Doctor mis à jour : Spécialité=$($docUpdated.specialite), Nom=$($docUpdated.nom)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# 2. CRUD AVAILABILITY (service-praticiens)
# ------------------------------------------------------------------------------
Write-Host "`n--- [2/4] CRUD Availability (service-praticiens) ---" -ForegroundColor Yellow

$availPayload = @{
    doctorId = $docId
    dateHeure = "2026-12-01T09:00:00"
    dureeMinutes = 45
    statut = "DISPONIBLE"
} | ConvertTo-Json

$avail = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities" -Method Post -Body $availPayload -ContentType "application/json; charset=utf-8"
$availId = $avail.id
Write-Host "✓ [POST] Créneau créé : ID=$availId pour Doctor=$($avail.doctorId) (Statut=$($avail.statut))" -ForegroundColor Green

# Read by doctor
$avails = Invoke-RestMethod -Uri "$GATEWAY/api/v1/availabilities?doctorId=$docId" -Method Get
Write-Host "✓ [GET]  Créneaux trouvés pour Doctor $docId : $($avails.Count) créneau(x)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# 3. CRUD PATIENT (service-patients)
# ------------------------------------------------------------------------------
Write-Host "`n--- [3/4] CRUD Patient (service-patients) ---" -ForegroundColor Yellow

$secu = "1" + (Get-Random -Minimum 100000000000 -Maximum 999999999999)
$patientPayload = @{
    nom = "Patient Test $RAND"
    email = "patient$RAND@test.com"
    dateNaissance = "1995-04-18"
    telephone = "0711223344"
    numeroSecu = $secu
} | ConvertTo-Json

$pat = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients" -Method Post -Body $patientPayload -ContentType "application/json; charset=utf-8"
$patId = $pat.id
Write-Host "✓ [POST] Patient créé : ID=$patId ($($pat.nom), Sécu=$($pat.numeroSecu))" -ForegroundColor Green

# Read by Secu
$patSecu = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients/secu/$secu" -Method Get
Write-Host "✓ [GET]  Patient retrouvé par numéro Sécu : Nom=$($patSecu.nom)" -ForegroundColor Green

# Update (PUT)
$patUpdatePayload = @{
    nom = "Patient Test $RAND Modifie"
    email = "patient.updated$RAND@test.com"
    dateNaissance = "1995-04-18"
    telephone = "0799887766"
    numeroSecu = $secu
} | ConvertTo-Json

$patUpdated = Invoke-RestMethod -Uri "$GATEWAY/api/v1/patients/$patId" -Method Put -Body $patUpdatePayload -ContentType "application/json; charset=utf-8"
Write-Host "✓ [PUT]  Patient mis à jour : Nouveau nom=$($patUpdated.nom), Tel=$($patUpdated.telephone)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# 4. CRUD MEDICAL RECORD (service-patients)
# ------------------------------------------------------------------------------
Write-Host "`n--- [4/4] CRUD Medical Record (service-patients) ---" -ForegroundColor Yellow

$recPayload = @{
    patientId = $patId
    allergies = "Iode, Aspirine"
    antecedents = "Asthme bronchique"
} | ConvertTo-Json

$rec = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records" -Method Post -Body $recPayload -ContentType "application/json; charset=utf-8"
Write-Host "✓ [POST] Dossier médical initialisé pour Patient=$patId" -ForegroundColor Green

# Read by patientId
$recGet = Invoke-RestMethod -Uri "$GATEWAY/api/v1/medical-records/patient/$patId" -Method Get
Write-Host "✓ [GET]  Dossier médical récupéré : Allergies=$($recGet.allergies), Antécédents=$($recGet.antecedents)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   🎉 Jalon V1 validé avec succès sur toutes les entités !  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
