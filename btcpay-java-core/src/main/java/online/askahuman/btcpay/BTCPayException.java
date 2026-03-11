package online.askahuman.btcpay;

/**
 * Thrown when the BTCPay Server Greenfield API returns an error or is unreachable.
 *
 * <p>When caused by a non-2xx HTTP response, the raw response body is accessible via
 * {@link #getResponseBody()} for diagnostic purposes. <strong>Callers must not expose
 * the raw response body to external clients</strong> — it may contain internal server
 * details from the BTCPay Server. Use {@link #getMessage()} for safe, truncated logging.</p>
 */
public class BTCPayException extends RuntimeException {

    /** Maximum characters from the API response body included in the exception message. */
    static final int MAX_BODY_IN_MESSAGE = 256;

    private final int statusCode;
    private final String responseBody;

    /**
     * Creates a new exception with the given message and no HTTP status code.
     *
     * @param message the error message
     */
    public BTCPayException(String message) {
        super(message);
        this.statusCode = 0;
        this.responseBody = null;
    }

    /**
     * Creates a new exception for a non-2xx HTTP response.
     *
     * @param statusCode   the HTTP status code
     * @param responseBody the raw response body from BTCPay Server
     */
    public BTCPayException(int statusCode, String responseBody) {
        super(buildMessage(statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BTCPayException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    /**
     * Returns the HTTP status code from the BTCPay Server response, or 0 if this
     * exception was not caused by an HTTP error.
     *
     * @return the HTTP status code, or 0
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw response body from the BTCPay Server, or null if this exception
     * was not caused by an HTTP error.
     *
     * <p><strong>Do not expose this value in API responses or client-facing logs.</strong>
     * It may contain internal server details. Use it only for server-side diagnostics.</p>
     *
     * @return the raw response body, or null
     */
    public String getResponseBody() {
        return responseBody;
    }

    private static String buildMessage(int statusCode, String body) {
        if (body == null || body.isEmpty()) {
            return "BTCPay API error " + statusCode;
        }
        String truncated = body.length() > MAX_BODY_IN_MESSAGE
                ? body.substring(0, MAX_BODY_IN_MESSAGE) + "... [truncated]"
                : body;
        return "BTCPay API error " + statusCode + ": " + truncated;
    }
}
