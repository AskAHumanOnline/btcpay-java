package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of paying a Lightning invoice via the BTCPay Server store Lightning API.
 *
 * <p>Returned by {@code POST /api/v1/stores/{storeId}/lightning/BTC/invoices/pay}.
 * {@code status} will be {@code "complete"} on success, {@code "pending"} if the payment
 * response timed out (resolve with the get-payment endpoint), or {@code "failed"}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightningPaymentResult {

    @JsonProperty("paymentHash")
    private String paymentHash;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalAmount")
    private String totalAmount;

    @JsonProperty("feeAmount")
    private String feeAmount;

    @JsonProperty("BOLT11")
    private String bolt11;

    /**
     * Returns the Lightning payment hash.
     *
     * @return the payment hash
     */
    public String getPaymentHash() {
        return paymentHash;
    }

    /**
     * Sets the Lightning payment hash.
     *
     * @param paymentHash the payment hash
     */
    public void setPaymentHash(String paymentHash) {
        this.paymentHash = paymentHash;
    }

    /**
     * Returns the payment status (e.g., {@code "complete"}, {@code "pending"}, {@code "failed"}).
     *
     * @return the status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the payment status.
     *
     * @param status the status string
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the total amount paid in millisatoshis.
     *
     * @return the total amount, or null if not provided
     */
    public String getTotalAmount() {
        return totalAmount;
    }

    /**
     * Sets the total amount paid.
     *
     * @param totalAmount the total amount
     */
    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Returns the routing fee in millisatoshis.
     *
     * @return the fee amount, or null if not provided
     */
    public String getFeeAmount() {
        return feeAmount;
    }

    /**
     * Sets the routing fee amount.
     *
     * @param feeAmount the fee amount
     */
    public void setFeeAmount(String feeAmount) {
        this.feeAmount = feeAmount;
    }

    /**
     * Returns the BOLT11 invoice string that was paid.
     *
     * @return the BOLT11 string, or null if not provided
     */
    public String getBolt11() {
        return bolt11;
    }

    /**
     * Sets the BOLT11 invoice string.
     *
     * @param bolt11 the BOLT11 string
     */
    public void setBolt11(String bolt11) {
        this.bolt11 = bolt11;
    }
}
