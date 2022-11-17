package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code SERVICE_UNAVAILABLE} status in REST response. */
public class GatewayTimeoutException extends ErrorReportException {
    private static final HttpStatus thisStatus = HttpStatus.GATEWAY_TIMEOUT;

    public GatewayTimeoutException(String message) {
        super(message, null, thisStatus);
    }

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, cause, null, thisStatus);
    }

    public GatewayTimeoutException(Throwable cause) {
        super(cause, thisStatus);
    }

    public GatewayTimeoutException(String message, List<String> causes) {
        super(message, causes, thisStatus);
    }

    public GatewayTimeoutException(String message, Throwable cause, List<String> causes) {
        super(message, cause, causes, thisStatus);
    }
}
