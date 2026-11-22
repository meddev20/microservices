package com.medrdv.praticiens.controller;

import com.medrdv.common.enums.SlotStatus;
import com.medrdv.praticiens.entity.Availability;
import com.medrdv.praticiens.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<List<Availability>> getAvailabilities(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) SlotStatus statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        if (doctorId != null && start != null && end != null) {
            return ResponseEntity.ok(availabilityService.getAvailabilitiesByDoctorAndRange(doctorId, start, end));
        }
        if (doctorId != null && statut != null) {
            return ResponseEntity.ok(availabilityService.getAvailabilitiesByDoctorAndStatus(doctorId, statut));
        }
        return ResponseEntity.ok(availabilityService.getAllAvailabilities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Availability> getAvailabilityById(@PathVariable Long id) {
        return availabilityService.getAvailabilityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Availability> createAvailability(@Valid @RequestBody Availability availability) {
        Availability created = availabilityService.createAvailability(availability);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Availability> reserveSlot(
            @PathVariable Long id,
            @RequestParam Long doctorId) {
        try {
            Availability reserved = availabilityService.reserveSlotAtomic(id, doctorId);
            return ResponseEntity.ok(reserved);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Availability> releaseSlot(@PathVariable Long id) {
        Availability released = availabilityService.releaseSlot(id);
        if (released != null) {
            return ResponseEntity.ok(released);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }
}
