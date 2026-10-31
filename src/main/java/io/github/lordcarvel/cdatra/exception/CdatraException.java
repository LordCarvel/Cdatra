package io.github.lordcarvel.cdatra.exception;

public class CdatraException extends RuntimeException {

    public CdatraException (String message) {

        super(message);
    }

    public CdatraException (String message, Throwable cause) {

        super(message, cause);
    }
}
