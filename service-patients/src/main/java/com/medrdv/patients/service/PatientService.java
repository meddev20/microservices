package com.medrdv.patients.service;

import com.medrdv.patients.entity.Patient;
import com.medrdv.patients.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Optional<Patient> getPatientByNumeroSecu(String numeroSecu) {
        return patientRepository.findByNumeroSecu(numeroSecu);
    }

    @Transactional
    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient introuvable avec l'id: " + id));

        patient.setNom(patientDetails.getNom());
        patient.setEmail(patientDetails.getEmail());
        patient.setDateNaissance(patientDetails.getDateNaissance());
        patient.setTelephone(patientDetails.getTelephone());
        patient.setNumeroSecu(patientDetails.getNumeroSecu());

        return patientRepository.save(patient);
    }

    @Transactional
    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }
}

