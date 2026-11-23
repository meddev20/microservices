package com.medrdv.rendezvous.client;

import com.medrdv.common.dto.AvailabilityDTO;
import com.medrdv.common.dto.DoctorDTO;
import com.medrdv.common.dto.MedicalRecordDTO;
import com.medrdv.common.dto.PatientDTO;
import com.medrdv.rendezvous.exception.ServiceExterneIndisponibleException;
import feign.FeignException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Enveloppe résiliente des appels Feign (Jalon V6 - Dr. Salim KHALIL)
 * Intègre : Timeouts, Retry, Circuit Breaker, Bulkhead, Cache Caffeine et Repli strict (503).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientRequests {

    private final PatientsClient patientsClient;
    private final PraticiensClient praticiensClient;
    private final CacheManager cacheManager;

    // =========================================================================
    // 1. Appels Patients
    // =========================================================================

    /**
     * Création : repli strict (lève ServiceExterneIndisponibleException traduit en HTTP 503)
     */
    @Retry(name = "service-patients", fallbackMethod = "patientIndisponible")
    @CircuitBreaker(name = "service-patients")
    @Bulkhead(name = "service-patients")
    public PatientDTO getPatientByIdStrict(Long id) {
        PatientDTO patient = patientsClient.getPatientById(id);
        cachePatient(id, patient);
        return patient;
    }

    /**
     * Consultation enrichie : repli tolérant (cache ou champs à null)
     */
    @Retry(name = "service-patients", fallbackMethod = "patientDeRepli")
    @CircuitBreaker(name = "service-patients")
    @Bulkhead(name = "service-patients")
    public PatientDTO getPatientByIdFb(Long id) {
        PatientDTO patient = patientsClient.getPatientById(id);
        cachePatient(id, patient);
        return patient;
    }

    @Retry(name = "service-patients", fallbackMethod = "dossierDeRepli")
    @CircuitBreaker(name = "service-patients")
    @Bulkhead(name = "service-patients")
    public MedicalRecordDTO getMedicalRecordByPatientIdFb(Long patientId) {
        return patientsClient.getMedicalRecordByPatientId(patientId);
    }

    // =========================================================================
    // 2. Appels Praticiens & Créneaux
    // =========================================================================

    /**
     * Création : repli strict praticien (lève ServiceExterneIndisponibleException -> HTTP 503)
     */
    @Retry(name = "service-praticiens", fallbackMethod = "praticienIndisponible")
    @CircuitBreaker(name = "service-praticiens")
    @Bulkhead(name = "service-praticiens")
    public DoctorDTO getDoctorByIdStrict(Long id) {
        DoctorDTO doctor = praticiensClient.getDoctorById(id);
        cacheDoctor(id, doctor);
        return doctor;
    }

    /**
     * Consultation enrichie : repli tolérant praticien (cache ou champs à null)
     */
    @Retry(name = "service-praticiens", fallbackMethod = "praticienDeRepli")
    @CircuitBreaker(name = "service-praticiens")
    @Bulkhead(name = "service-praticiens")
    public DoctorDTO getDoctorByIdFb(Long id) {
        DoctorDTO doctor = praticiensClient.getDoctorById(id);
        cacheDoctor(id, doctor);
        return doctor;
    }

    /**
     * Création : repli strict créneau (lève ServiceExterneIndisponibleException -> HTTP 503)
     */
    @Retry(name = "service-praticiens", fallbackMethod = "slotIndisponible")
    @CircuitBreaker(name = "service-praticiens")
    @Bulkhead(name = "service-praticiens")
    public AvailabilityDTO getAvailabilityByIdStrict(Long id) {
        return praticiensClient.getAvailabilityById(id);
    }

    /**
     * Consultation enrichie : repli tolérant créneau (null si absent)
     */
    @Retry(name = "service-praticiens", fallbackMethod = "creneauDeRepli")
    @CircuitBreaker(name = "service-praticiens")
    @Bulkhead(name = "service-praticiens")
    public AvailabilityDTO getAvailabilityByIdFb(Long id) {
        return praticiensClient.getAvailabilityById(id);
    }

    // =========================================================================
    // 3. Méthodes de Repli Strict (Création -> HTTP 503)
    // =========================================================================

    public PatientDTO patientIndisponible(Long id, Throwable t) {
        log.error("Échec appel service-patients pour patient ID {}: {}", id, t.getMessage());
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        throw new ServiceExterneIndisponibleException("Service-patients est momentanément indisponible. Réessayez dans quelques instants.");
    }

    public DoctorDTO praticienIndisponible(Long id, Throwable t) {
        log.error("Échec appel service-praticiens pour praticien ID {}: {}", id, t.getMessage());
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        throw new ServiceExterneIndisponibleException("Service-praticiens est momentanément indisponible. Réessayez dans quelques instants.");
    }

    public AvailabilityDTO slotIndisponible(Long id, Throwable t) {
        log.error("Échec appel service-praticiens pour créneau ID {}: {}", id, t.getMessage());
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        throw new ServiceExterneIndisponibleException("Service des créneaux est momentanément indisponible. Réessayez dans quelques instants.");
    }

    // =========================================================================
    // 4. Méthodes de Repli Tolérant (Consultation -> Cache ou Null)
    // =========================================================================

    public PatientDTO patientDeRepli(Long id, Throwable t) {
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        log.warn("Repli consultation : lecture cache pour patient {}", id);
        Cache cache = cacheManager.getCache("patients");
        if (cache != null && cache.get(id) != null) {
            return (PatientDTO) cache.get(id).get();
        }
        return new PatientDTO(id, null, null, null, null, null, null);
    }

    public MedicalRecordDTO dossierDeRepli(Long patientId, Throwable t) {
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        log.warn("Repli consultation : dossier médical indisponible pour patient {}", patientId);
        return null;
    }

    public DoctorDTO praticienDeRepli(Long id, Throwable t) {
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        log.warn("Repli consultation : lecture cache pour praticien {}", id);
        Cache cache = cacheManager.getCache("praticiens");
        if (cache != null && cache.get(id) != null) {
            return (DoctorDTO) cache.get(id).get();
        }
        return new DoctorDTO(id, null, null, null, null, null);
    }

    public AvailabilityDTO creneauDeRepli(Long id, Throwable t) {
        if (!estIndisponible(t)) {
            propagerSiErreurMetier(t);
        }
        log.warn("Repli consultation : créneau indisponible pour ID {}", id);
        return null;
    }

    // =========================================================================
    // 5. Détection des Pannes vs Erreurs Métier (TP V6 Dr. Salim KHALIL)
    // =========================================================================

    /**
     * Vérifie si l'exception correspond à une indisponibilité (Circuit ouvert, Timeout, Feign 503 ou connexion coupée)
     */
    private boolean estIndisponible(Throwable cause) {
        return cause instanceof CallNotPermittedException
                || (cause instanceof FeignException feign && (feign.status() == -1 || feign.status() == 503));
    }

    private void propagerSiErreurMetier(Throwable t) {
        if (t instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(t);
    }

    // =========================================================================
    // 6. Appels Directs & Gestion Cache
    // =========================================================================

    public AvailabilityDTO reserveSlotDirect(Long slotId, Long doctorId) {
        return praticiensClient.reserveSlot(slotId, doctorId);
    }

    public MedicalRecordDTO updateMedicalRecordDirect(MedicalRecordDTO record) {
        return patientsClient.updateMedicalRecord(record);
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
