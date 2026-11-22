package com.medrdv.rendezvous.client;

import com.medrdv.common.dto.AvailabilityDTO;
import com.medrdv.common.dto.DoctorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-praticiens", url = "${clients.praticiens.url:http://localhost:8081}")
public interface PraticiensClient {

    @GetMapping("/api/v1/doctors/{id}")
    DoctorDTO getDoctorById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/availabilities/{id}")
    AvailabilityDTO getAvailabilityById(@PathVariable("id") Long id);

    @PostMapping("/api/v1/availabilities/{id}/reserve")
    AvailabilityDTO reserveSlot(@PathVariable("id") Long id, @RequestParam("doctorId") Long doctorId);

    @PostMapping("/api/v1/availabilities/{id}/release")
    AvailabilityDTO releaseSlot(@PathVariable("id") Long id);
}
