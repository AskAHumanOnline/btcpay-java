package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for creating a BTCPay Server invoice via the Greenfield API.
 *
 * <p>By default the currency is {@code "SATS"} (satoshis). The {@code orderId} and
 * {@code description} fields are optional and map to BTCPay metadata and checkout
 * settings respectively.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CreateInvoiceRequest request = new CreateInvoiceRequest();
 * request.setAmount(25);
 * request.setOrderId("order-123");
 * request.setDescription("Payment for service");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateInvoiceRequest {

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("currency")
    private String currency = "SATS";

    private String orderId;
    private String description;

    /**
     * Returns the invoice amount in the specified currency (default: satoshis).
     *
     * @return the amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the invoice amount.
     *
     * @param amount the amount in the specified currency (default: satoshis)
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }

    /**
     * Returns the currency code.
     *
     * @return the currency (default: "SATS")
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code. Default is "SATS".
     *
     * @param currency the currency code
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the optional order ID for metadata tracking.
     *
     * @return the order ID, or null
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Sets an optional order ID that will be stored in the invoice metadata.
     *
     * @param orderId the order ID
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Returns the optional checkout description.
     *
     * @return the description, or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets an optional description shown during checkout.
     *
     * @param description the checkout description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns this request as a JSON-serializable map matching the BTCPay Greenfield API
     * invoice creation schema. Optional fields ({@code metadata.orderId},
     * {@code checkout.description}) are included only when set.
     *
     * @return a map suitable for Jackson serialization
     */
    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
        if (orderId == null) {
            return null;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("orderId", orderId);
        return metadata;
    }

    /**
     * Returns checkout configuration, or null if no description is set.
     *
     * @return checkout map or null
     */
    @JsonProperty("checkout")
    public Map<String, Object> getCheckout() {
        if (description == null) {
            return null;
        }
        Map<String, Object> checkout = new LinkedHashMap<>();
        checkout.put("description", description);
        return checkout;
    }
}
