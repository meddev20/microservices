package com.medrdv.praticiens.repository;

import com.medrdv.praticiens.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialiteIgnoreCase(String specialite);
}
