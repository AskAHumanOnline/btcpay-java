package online.askahuman.btcpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of paying a Lightning invoice via the BTCPay Server Lightning API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightningPaymentResult {

    @JsonProperty("result")
    private String result;

    /**
     * Returns the payment result string (e.g., "ok").
     *
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * Sets the payment result string.
     *
     * @param result the result
     */
    public void setResult(String result) {
        this.result = result;
    }
}
