package com.medrdv.batch.client;

import com.medrdv.common.dto.EnrichedAppointmentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "service-rendezvous", url = "${clients.rendezvous.url:http://localhost:8083}")
public interface RendezVousBatchClient {

    @GetMapping("/api/v1/appointments")
    List<Map<String, Object>> getAllAppointments();

    @GetMapping("/api/v1/appointments/{id}/enriched")
    EnrichedAppointmentDTO getEnrichedAppointment(@PathVariable("id") Long id);
}

