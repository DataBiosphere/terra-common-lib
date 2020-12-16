package bio.terra.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * This exception maps to HttpStatus.INTERNAL_SERVER_ERROR in the GlobalExceptionHandler.
 */
public abstract class InternalServerErrorException extends ErrorReportException {
    private static final HttpStatus thisStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    public InternalServerErrorException(String message) {
        super(message, null, thisStatus);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause, null, thisStatus);
    }

    public InternalServerErrorException(Throwable cause) {
        super(null, cause, null, thisStatus);
    }

    public InternalServerErrorException(String message, List<String> causes) {
        super(message, causes, thisStatus);
    }

    public InternalServerErrorException(String message, Throwable cause, List<String> causes) {
        super(message, cause, causes, thisStatus);
    }
}
