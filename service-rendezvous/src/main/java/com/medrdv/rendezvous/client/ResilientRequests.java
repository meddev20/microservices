package com.medrdv.rendezvous.client;

import com.medrdv.common.dto.AvailabilityDTO;
import com.medrdv.common.dto.DoctorDTO;
import com.medrdv.common.dto.MedicalRecordDTO;
import com.medrdv.common.dto.PatientDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientRequests {

    private final PatientsClient patientsClient;
    private final PraticiensClient praticiensClient;
    private final CacheManager cacheManager;

    // --- Appels Patients (Strict pour la création, Dégradé pour la consultation) ---

    @Retry(name = "service-patients", fallbackMethod = "patientIndisponible")
    @CircuitBreaker(name = "service-patients")
    public PatientDTO getPatientByIdStrict(Long id) {
        PatientDTO patient = patientsClient.getPatientById(id);
        cachePatient(id, patient);
        return patient;
    }

    @Retry(name = "service-patients", fallbackMethod = "patientDeRepli")
    @CircuitBreaker(name = "service-patients")
    public PatientDTO getPatientByIdFb(Long id) {
        PatientDTO patient = patientsClient.getPatientById(id);
        cachePatient(id, patient);
        return patient;
    }

    @Retry(name = "service-patients", fallbackMethod = "dossierDeRepli")
    @CircuitBreaker(name = "service-patients")
    public MedicalRecordDTO getMedicalRecordByPatientIdFb(Long patientId) {
        return patientsClient.getMedicalRecordByPatientId(patientId);
    }

    // --- Appels Praticiens (Strict pour la création, Dégradé pour la consultation) ---

    @Retry(name = "service-praticiens", fallbackMethod = "praticienIndisponible")
    @CircuitBreaker(name = "service-praticiens")
    public DoctorDTO getDoctorByIdStrict(Long id) {
        DoctorDTO doctor = praticiensClient.getDoctorById(id);
        cacheDoctor(id, doctor);
        return doctor;
    }

    @Retry(name = "service-praticiens", fallbackMethod = "praticienDeRepli")
    @CircuitBreaker(name = "service-praticiens")
    public DoctorDTO getDoctorByIdFb(Long id) {
        DoctorDTO doctor = praticiensClient.getDoctorById(id);
        cacheDoctor(id, doctor);
        return doctor;
    }

    @Retry(name = "service-praticiens", fallbackMethod = "slotIndisponible")
    @CircuitBreaker(name = "service-praticiens")
    public AvailabilityDTO getAvailabilityByIdStrict(Long id) {
        return praticiensClient.getAvailabilityById(id);
    }

    // --- Méthodes de Repli (Fallbacks) ---

    public PatientDTO patientIndisponible(Long id, Throwable t) {
        log.error("Échec strict appel service-patients pour ID {}: {}", id, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service-patients temporairement indisponible");
    }

    public PatientDTO patientDeRepli(Long id, Throwable t) {
        log.warn("Repli consultation : lecture cache pour patient {}", id);
        Cache cache = cacheManager.getCache("patients");
        if (cache != null && cache.get(id) != null) {
            return (PatientDTO) cache.get(id).get();
        }
        return null;
    }

    public MedicalRecordDTO dossierDeRepli(Long patientId, Throwable t) {
        log.warn("Repli consultation : dossier médical indisponible pour patient {}", patientId);
        return null;
    }

    public DoctorDTO praticienIndisponible(Long id, Throwable t) {
        log.error("Échec strict appel service-praticiens pour ID {}: {}", id, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service-praticiens temporairement indisponible");
    }

    public DoctorDTO praticienDeRepli(Long id, Throwable t) {
        log.warn("Repli consultation : lecture cache pour praticien {}", id);
        Cache cache = cacheManager.getCache("praticiens");
        if (cache != null && cache.get(id) != null) {
            return (DoctorDTO) cache.get(id).get();
        }
        return null;
    }

    public AvailabilityDTO slotIndisponible(Long id, Throwable t) {
        log.error("Échec strict créneau ID {}: {}", id, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service des créneaux temporairement indisponible");
    }

    private void cachePatient(Long id, PatientDTO p) {
        if (p != null) {
            Cache c = cacheManager.getCache("patients");
            if (c != null) c.put(id, p);
        }
    }

    private void cacheDoctor(Long id, DoctorDTO d) {
        if (d != null) {
            Cache c = cacheManager.getCache("praticiens");
            if (c != null) c.put(id, d);
        }
    }
}
