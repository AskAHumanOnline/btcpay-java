package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for GET /api/v1/stores/{storeId}/lightning/BTC/payments/{paymentHash}.
 * <p>Status values from BTCPay are PascalCase: Unknown, Processing, Complete, Failed.
 * All comparisons must be case-insensitive.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightningPayment {

    @JsonProperty("id")
    private String paymentHash;

    @JsonProperty("status")
    private String status;

    public String getPaymentHash() {
        return paymentHash;
    }

    public void setPaymentHash(String paymentHash) {
        this.paymentHash = paymentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
