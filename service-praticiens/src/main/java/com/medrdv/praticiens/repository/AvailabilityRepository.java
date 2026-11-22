package com.medrdv.praticiens.repository;

import com.medrdv.common.enums.SlotStatus;
import com.medrdv.praticiens.entity.Availability;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByDoctorIdAndStatut(Long doctorId, SlotStatus statut);

    List<Availability> findByDoctorIdAndDateHeureBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Availability a WHERE a.id = :id")
    Optional<Availability> findByIdWithLock(@Param("id") Long id);
}
