package online.askahuman.btcpay.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for btcpay-java. Bind to {@code btcpay.*} in
 * {@code application.yml} or {@code application.properties}.
 *
 * <p>Example YAML configuration:</p>
 * <pre>{@code
 * btcpay:
 *   host: https://btcpay.example.com
 *   api-key: ${BTCPAY_API_KEY}
 *   store-id: ${BTCPAY_STORE_ID}
 *   webhook-secret: ${BTCPAY_WEBHOOK_SECRET}
 *   connect-timeout-seconds: 3
 *   read-timeout-seconds: 5
 * }</pre>
 */
@ConfigurationProperties(prefix = "btcpay")
public class BTCPayProperties {

    /**
     * BTCPay Server hostname or URL (e.g., "umbrel.local" or "https://mybtcpay.example.com").
     */
    private String host;

    /**
     * BTCPay Server API key with createinvoice + viewinvoices + sendlightning permissions.
     */
    private String apiKey;

    /**
     * Default BTCPay store ID. When set, enables the no-arg method overloads on
     * {@link online.askahuman.btcpay.BTCPayClient} (e.g., {@code createStoreInvoice(request)}).
     */
    private String storeId;

    /**
     * Optional webhook secret for validating BTCPay webhook signatures.
     */
    private String webhookSecret;

    /**
     * Connection timeout in seconds. Default: 3.
     */
    private int connectTimeoutSeconds = 3;

    /**
     * Read timeout in seconds. Default: 5.
     */
    private int readTimeoutSeconds = 5;

    /**
     * Returns the BTCPay Server host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the BTCPay Server host.
     *
     * @param host the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the API key.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key.
     *
     * @param apiKey the API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns the default store ID.
     *
     * @return the store ID, or null if not configured
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Sets the default store ID.
     *
     * @param storeId the store ID
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    /**
     * Returns the webhook secret.
     *
     * @return the webhook secret, or null if not configured
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Sets the webhook secret.
     *
     * @param webhookSecret the webhook secret
     */
    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * Returns the connection timeout in seconds.
     *
     * @return the connection timeout
     */
    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /**
     * Sets the connection timeout in seconds.
     *
     * @param connectTimeoutSeconds the connection timeout
     */
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    /**
     * Returns the read timeout in seconds.
     *
     * @return the read timeout
     */
    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    /**
     * Sets the read timeout in seconds.
     *
     * @param readTimeoutSeconds the read timeout
     */
    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
