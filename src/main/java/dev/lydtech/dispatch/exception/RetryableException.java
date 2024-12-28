package dev.lydtech.dispatch.exception;

import org.springframework.web.client.RestClientException;

public class RetryableException extends RuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(Exception e) {
        super(e);
    }
}
