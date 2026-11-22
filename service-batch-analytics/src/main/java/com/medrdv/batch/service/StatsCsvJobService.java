package com.medrdv.batch.service;

import com.medrdv.batch.client.PraticiensBatchClient;
import com.medrdv.batch.client.RendezVousBatchClient;
import com.medrdv.common.dto.DoctorDTO;
import com.medrdv.common.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsCsvJobService {

    private final RendezVousBatchClient rendezVousClient;
    private final PraticiensBatchClient praticiensClient;

    /**
     * Tâche planifiée exécutée le 1er de chaque mois à minuit (ou appelable à la demande).
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public String generateMonthlyStatsCsv() {
        log.info("Génération du rapport CSV mensuel des consultations par spécialité...");

        // 1. Charger tous les praticiens pour connaître leur spécialité
        List<DoctorDTO> doctors = praticiensClient.getAllDoctors();
        Map<Long, String> doctorSpecialtyMap = doctors.stream()
                .collect(Collectors.toMap(DoctorDTO::getId, DoctorDTO::getSpecialite, (s1, s2) -> s1));

        // 2. Charger tous les rendez-vous
        List<Map<String, Object>> appointments = rendezVousClient.getAllAppointments();

        // 3. Agréger par spécialité
        Map<String, int[]> statsPerSpecialty = new HashMap<>(); // [total, realisees, confirmees]

        for (Map<String, Object> app : appointments) {
            Long doctorId = Long.valueOf(app.get("doctorId").toString());
            String status = app.get("statut") != null ? app.get("statut").toString() : "";
            String specialty = doctorSpecialtyMap.getOrDefault(doctorId, "Inconnue");

            statsPerSpecialty.putIfAbsent(specialty, new int[3]);
            int[] counters = statsPerSpecialty.get(specialty);

            counters[0]++; // Total
            if (AppointmentStatus.REALISE.name().equalsIgnoreCase(status)) {
                counters[1]++; // Réalisées
            } else if (AppointmentStatus.CONFIRME.name().equalsIgnoreCase(status)) {
                counters[2]++; // Confirmées
            }
        }

        // 4. Générer le contenu CSV
        StringWriter writer = new StringWriter();
        writer.append("Mois,Specialite,TotalRendezVous,ConsultationsRealisees,ConsultationsConfirmees\n");

        String currentMonth = YearMonth.now().toString();
        for (Map.Entry<String, int[]> entry : statsPerSpecialty.entrySet()) {
            writer.append(String.format("%s,%s,%d,%d,%d\n",
                    currentMonth,
                    escapeCsv(entry.getKey()),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2]
            ));
        }

        String csvOutput = writer.toString();
        log.info("Rapport CSV généré avec succès :\n{}", csvOutput);
        return csvOutput;
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

