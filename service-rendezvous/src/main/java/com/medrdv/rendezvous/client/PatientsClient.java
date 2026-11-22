package com.medrdv.rendezvous.client;

import com.medrdv.common.dto.MedicalRecordDTO;
import com.medrdv.common.dto.PatientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-patients", url = "${clients.patients.url:http://localhost:8082}")
public interface PatientsClient {

    @GetMapping("/api/v1/patients/{id}")
    PatientDTO getPatientById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/medical-records/patient/{patientId}")
    MedicalRecordDTO getMedicalRecordByPatientId(@PathVariable("patientId") Long patientId);

    @PostMapping("/api/v1/medical-records")
    MedicalRecordDTO updateMedicalRecord(@RequestBody MedicalRecordDTO record);
}
