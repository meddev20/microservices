package com.medrdv.patients.controller;

import com.medrdv.patients.entity.MedicalRecord;
import com.medrdv.patients.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<MedicalRecord> getRecordByPatientId(@PathVariable Long patientId) {
        return medicalRecordService.getMedicalRecordByPatientId(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MedicalRecord> saveOrUpdateRecord(@Valid @RequestBody MedicalRecord record) {
        MedicalRecord saved = medicalRecordService.createOrUpdateMedicalRecord(record);
        return ResponseEntity.ok(saved);
    }
}

