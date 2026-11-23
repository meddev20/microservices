package com.medrdv.patients.service;

import com.medrdv.patients.entity.MedicalRecord;
import com.medrdv.patients.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public Optional<MedicalRecord> getMedicalRecordByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId);
    }

    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll();
    }

    public Optional<MedicalRecord> getMedicalRecordById(Long id) {
        return medicalRecordRepository.findById(id);
    }

    @Transactional
    public void deleteMedicalRecord(Long id) {
        medicalRecordRepository.deleteById(id);
    }

    @Transactional
    public MedicalRecord createOrUpdateMedicalRecord(MedicalRecord record) {
        Optional<MedicalRecord> existing = medicalRecordRepository.findByPatientId(record.getPatientId());
        if (existing.isPresent()) {
            MedicalRecord current = existing.get();
            current.setAllergies(record.getAllergies());
            current.setAntecedents(record.getAntecedents());
            return medicalRecordRepository.save(current);
        }
        return medicalRecordRepository.save(record);
    }

    /**
     * Actualise MedicalRecord avec le motif et la date de consultation,
     * SANS écraser une consultation plus récente (respect strict de la chronologie).
     */
    @Transactional
    public void updateConsultationHistory(Long patientId, LocalDateTime consultationDate, String motif) {
        MedicalRecord record = medicalRecordRepository.findByPatientId(patientId)
                .orElseGet(() -> MedicalRecord.builder()
                        .patientId(patientId)
                        .allergies("Aucune allergie connue renseignée")
                        .antecedents("Aucun antécédent renseigné")
                        .build());

        if (record.getDerniereConsultation() == null || consultationDate.isAfter(record.getDerniereConsultation())) {
            record.setDerniereConsultation(consultationDate);
            record.setDernierMotif(motif);
            medicalRecordRepository.save(record);
            log.info("Dossier médical du patient {} mis à jour avec la consultation du {}", patientId, consultationDate);
        } else {
            log.warn("La consultation du {} est antérieure à la dernière consultation enregistrée ({}) pour le patient {}. Mise à jour ignorée.",
                    consultationDate, record.getDerniereConsultation(), patientId);
        }
    }
}

