package com.medrdv.rendezvous.exception;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global et centralisé (Jalon V6 - Résilience Dr. Salim KHALIL)
 * Intercepte et formate toutes les erreurs de l'application sous forme de réponses JSON cohérentes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Erreur de validation des champs DTO / Entity (@Valid) -> HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erreur de validation");
        body.put("details", fieldErrors);

        log.warn("Validation échouée : {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Repli strict : Service distant indisponible (Jalon V6 TP §3.3) -> HTTP 503 Service Unavailable
     */
    @ExceptionHandler(ServiceExterneIndisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleServiceExterneIndisponible(ServiceExterneIndisponibleException ex) {
        log.error("Service distant indisponible (repli strict V6) : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Indisponible");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Circuit Breaker ouvert (Resilience4j CallNotPermittedException) -> HTTP 503 Service Unavailable
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit Breaker ouvert : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Circuit Breaker Ouvert");
        body.put("message", "Le service distant cible est temporairement coupé pour protection.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * RateLimiter dépassé (Resilience4j RequestNotPermitted TP §3.6) -> HTTP 429 Too Many Requests
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, Object>> handleRateLimiter(RequestNotPermitted ex) {
        log.warn("Dépassement du Rate Limiter : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Trop de requêtes");
        body.put("message", "Quota de consultations enrichies dépassé. Veuillez patienter avant de réessayer.");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    /**
     * Erreur 404 Feign (Ressource distante introuvable TP §3.7 & §4.5) -> HTTP 404 Not Found
     */
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleFeignNotFound(FeignException.NotFound ex) {
        log.warn("Ressource distante introuvable via Feign (404) : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Ressource Distante Non Trouvée");
        body.put("message", "Le patient ou le praticien demandé est introuvable sur le service distant.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Erreur 400 Feign (Mauvaise requête distante TP §4.5) -> HTTP 400 Bad Request
     */
    @ExceptionHandler(FeignException.BadRequest.class)
    public ResponseEntity<Map<String, Object>> handleFeignBadRequest(FeignException.BadRequest ex) {
        log.warn("Erreur métier service distant via Feign (400) : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Requête Distante Invalide");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Conflit d'état (ex: créneau déjà réservé) -> HTTP 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflit métier : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflit");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Argument incorrect ou ressource introuvable -> HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argument invalide : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Mauvaise requête");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Gestion des exceptions ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getReason());
        body.put("message", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Erreur générique inattendue -> HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erreur serveur inattendue : ", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Erreur Interne du Serveur");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
