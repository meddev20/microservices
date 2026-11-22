package com.medrdv.praticiens.service;

import com.medrdv.praticiens.entity.Doctor;
import com.medrdv.praticiens.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    public List<Doctor> getDoctorsBySpecialite(String specialite) {
        return doctorRepository.findBySpecialiteIgnoreCase(specialite);
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor updateDoctor(Long id, Doctor doctorDetails) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Médecin non trouvé avec l'id: " + id));

        doctor.setNom(doctorDetails.getNom());
        doctor.setSpecialite(doctorDetails.getSpecialite());
        doctor.setTelephone(doctorDetails.getTelephone());
        doctor.setEmail(doctorDetails.getEmail());

        return doctorRepository.save(doctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }
}
