package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a BTCPay Server store invoice as returned by the Greenfield API.
 *
 * <p>The {@link #id} field is the BTCPay-assigned invoice identifier (e.g.,
 * {@code "GjBd5TtU5VsEBVBNmRmANy"}). This is <em>not</em> the same as the
 * Lightning payment hash.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreInvoice {

    @JsonProperty("id")
    private String id;

    private InvoiceStatus status;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    /**
     * Returns the BTCPay invoice ID.
     *
     * @return the invoice ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the BTCPay invoice ID.
     *
     * @param id the invoice ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the invoice status.
     *
     * @return the status
     */
    public InvoiceStatus getStatus() {
        return status;
    }

    /**
     * Sets the invoice status from the API string value.
     *
     * @param status the status string (e.g., "New", "Settled")
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = InvoiceStatus.fromString(status);
    }

    /**
     * Returns the invoice amount as a string.
     *
     * @return the amount
     */
    public String getAmount() {
        return amount;
    }

    /**
     * Sets the invoice amount.
     *
     * @param amount the amount as a string
     */
    public void setAmount(String amount) {
        this.amount = amount;
    }

    /**
     * Returns the invoice currency.
     *
     * @return the currency code
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the invoice currency.
     *
     * @param currency the currency code
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
