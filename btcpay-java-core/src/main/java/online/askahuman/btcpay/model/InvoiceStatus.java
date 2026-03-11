package online.askahuman.btcpay.model;

/**
 * Status of a BTCPay Server invoice.
 *
 * <p>Values correspond to the statuses returned by the
 * <a href="https://docs.btcpayserver.org/API/Greenfield/v1/">Greenfield API</a>.</p>
 */
public enum InvoiceStatus {

    /** Invoice created and awaiting payment. */
    NEW,

    /** Payment detected but not yet fully confirmed. */
    PROCESSING,

    /** Invoice expired without full payment. */
    EXPIRED,

    /** Invoice is invalid (e.g., underpaid after expiry). */
    INVALID,

    /** Invoice fully paid and settled. */
    SETTLED,

    /** Status not recognized by this library version. */
    UNKNOWN;

    /**
     * Maps a string value to an {@link InvoiceStatus}, case-insensitively.
     * Returns {@link #UNKNOWN} for unrecognized values or null input.
     *
     * @param value the status string from the BTCPay API (e.g., "New", "Settled")
     * @return the corresponding enum value, or {@link #UNKNOWN}
     */
    public static InvoiceStatus fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
