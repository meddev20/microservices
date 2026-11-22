package com.medrdv.rendezvous.repository;

import com.medrdv.common.enums.AppointmentStatus;
import com.medrdv.rendezvous.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByDoctorIdAndStatut(Long doctorId, AppointmentStatus statut);
}
