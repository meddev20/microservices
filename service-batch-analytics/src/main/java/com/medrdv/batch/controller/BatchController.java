package com.medrdv.batch.controller;

import com.medrdv.batch.service.ReminderJobService;
import com.medrdv.batch.service.StatsCsvJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final ReminderJobService reminderJobService;
    private final StatsCsvJobService statsCsvJobService;

    /**
     * Déclencheur manuel du job de rappels J-1
     */
    @PostMapping("/reminders/trigger")
    public ResponseEntity<Map<String, Object>> triggerReminders() {
        int count = reminderJobService.runDailyReminders();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "remindersSent", count,
                "message", "Traitement des rappels J-1 exécuté avec succès."
        ));
    }

    /**
     * Téléchargement / Consultation du CSV des statistiques par spécialité
     */
    @GetMapping(value = "/stats/csv", produces = "text/csv")
    public ResponseEntity<String> getStatsCsv() {
        String csv = statsCsvJobService.generateMonthlyStatsCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"consultations-stats.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}

