package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a payment method for a BTCPay Server invoice.
 *
 * <p>For Lightning payments, the {@link #destination} field contains the BOLT11
 * invoice string and the {@link #paymentType} is {@code "LightningLike"}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoicePaymentMethod {

    @JsonProperty("paymentMethodId")
    private String paymentMethodId;

    @JsonProperty("cryptoCode")
    private String cryptoCode;

    @JsonProperty("paymentType")
    private String paymentType;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("paymentLink")
    private String paymentLink;

    @JsonProperty("amount")
    private String amount;

    /**
     * Returns the payment method identifier (e.g., "BTC-LN", "BTC").
     *
     * @return the payment method ID
     */
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    /**
     * Sets the payment method identifier.
     *
     * @param paymentMethodId the payment method ID
     */
    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    /**
     * Returns the cryptocurrency code (e.g., "BTC").
     *
     * @return the crypto code
     */
    public String getCryptoCode() {
        return cryptoCode;
    }

    /**
     * Sets the cryptocurrency code.
     *
     * @param cryptoCode the crypto code
     */
    public void setCryptoCode(String cryptoCode) {
        this.cryptoCode = cryptoCode;
    }

    /**
     * Returns the payment type (e.g., "LightningLike", "BTCLike").
     *
     * @return the payment type
     */
    public String getPaymentType() {
        return paymentType;
    }

    /**
     * Sets the payment type.
     *
     * @param paymentType the payment type
     */
    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    /**
     * Returns the payment destination. For Lightning, this is the BOLT11 invoice string.
     *
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Sets the payment destination.
     *
     * @param destination the destination
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * Returns the payment link URI (e.g., "lightning:lnbc...").
     *
     * @return the payment link
     */
    public String getPaymentLink() {
        return paymentLink;
    }

    /**
     * Sets the payment link URI.
     *
     * @param paymentLink the payment link
     */
    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    /**
     * Returns the payment amount as a string.
     *
     * @return the amount
     */
    public String getAmount() {
        return amount;
    }

    /**
     * Sets the payment amount.
     *
     * @param amount the amount as a string
     */
    public void setAmount(String amount) {
        this.amount = amount;
    }
}
