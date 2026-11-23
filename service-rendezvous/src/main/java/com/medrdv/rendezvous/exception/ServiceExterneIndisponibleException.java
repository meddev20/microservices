package com.medrdv.rendezvous.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lors d'un repli strict (Jalon V6)
 * lorsqu'un microservice distant indispensable est indisponible.
 * Traduit automatiquement en code HTTP 503 Service Unavailable.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceExterneIndisponibleException extends RuntimeException {

    public ServiceExterneIndisponibleException(String message) {
        super(message);
    }

    public ServiceExterneIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
