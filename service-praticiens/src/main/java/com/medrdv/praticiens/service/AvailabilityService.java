package com.medrdv.praticiens.service;

import com.medrdv.common.enums.SlotStatus;
import com.medrdv.praticiens.entity.Availability;
import com.medrdv.praticiens.repository.AvailabilityRepository;
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
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public List<Availability> getAllAvailabilities() {
        return availabilityRepository.findAll();
    }

    public Optional<Availability> getAvailabilityById(Long id) {
        return availabilityRepository.findById(id);
    }

    public List<Availability> getAvailabilitiesByDoctorAndStatus(Long doctorId, SlotStatus statut) {
        return availabilityRepository.findByDoctorIdAndStatut(doctorId, statut);
    }

    public List<Availability> getAvailabilitiesByDoctorAndRange(Long doctorId, LocalDateTime start, LocalDateTime end) {
        return availabilityRepository.findByDoctorIdAndDateHeureBetween(doctorId, start, end);
    }

    @Transactional
    public Availability createAvailability(Availability availability) {
        return availabilityRepository.save(availability);
    }

    /**
     * Réservation atomique du créneau avec verrouillage pessimiste/optimiste.
     * Vérifie que le créneau appartient bien au praticien et qu'il est DISPONIBLE.
     */
    @Transactional
    public Availability reserveSlotAtomic(Long slotId, Long doctorId) {
        Availability slot = availabilityRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new IllegalStateException("Créneau introuvable: " + slotId));

        if (!slot.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("Le créneau " + slotId + " n'appartient pas au praticien " + doctorId);
        }

        if (slot.getStatut() != SlotStatus.DISPONIBLE) {
            throw new IllegalStateException(
                    "Créneau " + slotId + " indisponible (statut actuel: " + slot.getStatut() + ")");
        }

        slot.setStatut(SlotStatus.RESERVE);
        log.info("Créneau {} réservé avec succès pour le praticien {}", slotId, doctorId);
        return availabilityRepository.save(slot);
    }

    /**
     * Libère un créneau préalablement réservé (ex: annulation ou compensation
     * Saga).
     */
    @Transactional
    public Availability releaseSlot(Long slotId) {
        return availabilityRepository.findById(slotId).map(slot -> {
            slot.setStatut(SlotStatus.DISPONIBLE);
            log.info("Créneau {} libéré et repassé au statut DISPONIBLE", slotId);
            return availabilityRepository.save(slot);
        }).orElse(null);
    }

    @Transactional
    public void deleteAvailability(Long id) {
        availabilityRepository.deleteById(id);
    }
}
