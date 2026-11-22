package com.medrdv.batch.client;

import com.medrdv.common.dto.DoctorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "service-praticiens", url = "${clients.praticiens.url:http://localhost:8081}")
public interface PraticiensBatchClient {

    @GetMapping("/api/v1/doctors")
    List<DoctorDTO> getAllDoctors();

    @GetMapping("/api/v1/doctors/{id}")
    DoctorDTO getDoctorById(@PathVariable("id") Long id);
}

