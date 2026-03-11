package online.askahuman.btcpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.askahuman.btcpay.model.CreateInvoiceRequest;
import online.askahuman.btcpay.model.InvoicePaymentMethod;
import online.askahuman.btcpay.model.InvoiceStatus;
import online.askahuman.btcpay.model.LightningPaymentResult;
import online.askahuman.btcpay.model.StoreInvoice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Client for the BTCPay Server
 * <a href="https://docs.btcpayserver.org/API/Greenfield/v1/">Greenfield API</a>.
 *
 * <p>This is a pure Java client with no Spring dependencies. It uses
 * {@link java.net.http.HttpClient} for HTTP communication and Jackson for
 * JSON serialization.</p>
 *
 * <p><strong>Thread safety:</strong> This class is thread-safe and safe to use as a
 * shared singleton. {@link java.net.http.HttpClient} and {@link ObjectMapper} are both
 * thread-safe after construction, and all fields are immutable (final). Instances may
 * be shared freely across threads without external synchronization.</p>
 *
 * <p><strong>Configurable store ID:</strong> A default store ID can be set at construction
 * time, enabling the no-arg overloads (e.g., {@link #createStoreInvoice(CreateInvoiceRequest)}).
 * The explicit {@code storeId} overloads remain available for multi-store use cases.</p>
 *
 * <p><strong>Two-phase invoice creation:</strong> Creating a Lightning invoice requires
 * two API calls — {@link #createStoreInvoice} followed by {@link #getLightningPaymentMethod}.
 * The BTCPay invoice ID returned by {@code createStoreInvoice} is the durable external
 * reference. Callers <em>must</em> persist this ID before calling
 * {@code getLightningPaymentMethod}. If the second call fails, the invoice already
 * exists in BTCPay and can be recovered by retrying {@link #getInvoicePaymentMethods}
 * with the stored ID.</p>
 *
 * <p>Example usage with a configured store ID:</p>
 * <pre>{@code
 * BTCPayClient client = new BTCPayClient(
 *     "https://btcpay.example.com", "your-api-key", "your-store-id");
 *
 * CreateInvoiceRequest request = new CreateInvoiceRequest();
 * request.setAmount(25L);
 * StoreInvoice invoice = client.createStoreInvoice(request);
 * // persist invoice.getId() before the next call
 *
 * Optional<InvoicePaymentMethod> lightning =
 *     client.getLightningPaymentMethod(invoice.getId());
 * }</pre>
 */
public class BTCPayClient {

    private static final System.Logger log = System.getLogger(BTCPayClient.class.getName());

    private final String host;
    private final String apiKey;
    private final String storeId;
    private final Duration readTimeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new BTCPay client with a configured store ID and explicit timeouts.
     *
     * @param host                  BTCPay Server hostname or URL (e.g., "btcpay.example.com"
     *                              or "https://btcpay.example.com")
     * @param apiKey                Greenfield API key
     * @param storeId               default store ID used by the no-arg method overloads;
     *                              may be {@code null} if you always supply it explicitly
     * @param connectTimeoutSeconds connection timeout in seconds
     * @param readTimeoutSeconds    read (request) timeout in seconds
     * @throws NullPointerException     if host or apiKey is null
     * @throws IllegalArgumentException if timeouts are not positive
     */
    public BTCPayClient(String host, String apiKey, String storeId,
                        int connectTimeoutSeconds, int readTimeoutSeconds) {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        if (connectTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("connectTimeoutSeconds must be positive");
        }
        if (readTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("readTimeoutSeconds must be positive");
        }
        this.host = host.startsWith("http") ? host : "https://" + host;
        this.apiKey = apiKey;
        this.storeId = storeId;
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new BTCPay client with a configured store ID and default timeouts (3s connect, 5s read).
     *
     * @param host    BTCPay Server hostname or URL
     * @param apiKey  Greenfield API key
     * @param storeId default store ID used by the no-arg method overloads
     */
    public BTCPayClient(String host, String apiKey, String storeId) {
        this(host, apiKey, storeId, 3, 5);
    }

    /**
     * Creates a new BTCPay client without a configured store ID and with explicit timeouts.
     * All method calls must supply a {@code storeId} explicitly.
     *
     * @param host                  BTCPay Server hostname or URL
     * @param apiKey                Greenfield API key
     * @param connectTimeoutSeconds connection timeout in seconds
     * @param readTimeoutSeconds    read (request) timeout in seconds
     */
    public BTCPayClient(String host, String apiKey, int connectTimeoutSeconds, int readTimeoutSeconds) {
        this(host, apiKey, null, connectTimeoutSeconds, readTimeoutSeconds);
    }

    /**
     * Creates a new BTCPay client without a configured store ID and default timeouts (3s connect, 5s read).
     * All method calls must supply a {@code storeId} explicitly.
     *
     * @param host   BTCPay Server hostname or URL
     * @param apiKey Greenfield API key
     */
    public BTCPayClient(String host, String apiKey) {
        this(host, apiKey, null, 3, 5);
    }

    // -------------------------------------------------------------------------
    // No-arg overloads (use the configured storeId)
    // -------------------------------------------------------------------------

    /**
     * Creates a store invoice using the configured store ID.
     *
     * @param request invoice creation parameters
     * @return the created invoice
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the API returns an error
     * @see #createStoreInvoice(String, CreateInvoiceRequest)
     */
    public StoreInvoice createStoreInvoice(CreateInvoiceRequest request) {
        return createStoreInvoice(requireStoreId(), request);
    }

    /**
     * Returns all payment methods for an invoice using the configured store ID.
     *
     * @param invoiceId the BTCPay invoice ID
     * @return list of payment methods
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the API returns an error
     * @see #getInvoicePaymentMethods(String, String)
     */
    public List<InvoicePaymentMethod> getInvoicePaymentMethods(String invoiceId) {
        return getInvoicePaymentMethods(requireStoreId(), invoiceId);
    }

    /**
     * Returns the Lightning payment method for an invoice using the configured store ID.
     *
     * @param invoiceId the BTCPay invoice ID
     * @return Lightning payment method, or empty if none exists
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the API returns an error
     * @see #getLightningPaymentMethod(String, String)
     */
    public Optional<InvoicePaymentMethod> getLightningPaymentMethod(String invoiceId) {
        return getLightningPaymentMethod(requireStoreId(), invoiceId);
    }

    /**
     * Returns the invoice by ID using the configured store ID.
     *
     * @param invoiceId the BTCPay invoice ID
     * @return the invoice
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the API returns an error
     * @see #getInvoice(String, String)
     */
    public StoreInvoice getInvoice(String invoiceId) {
        return getInvoice(requireStoreId(), invoiceId);
    }

    /**
     * Returns true if the invoice has status {@link InvoiceStatus#SETTLED}, using the configured store ID.
     *
     * @param invoiceId the BTCPay invoice ID
     * @return true if paid and settled
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the API returns an error
     * @see #isInvoicePaid(String, String)
     */
    public boolean isInvoicePaid(String invoiceId) {
        return isInvoicePaid(requireStoreId(), invoiceId);
    }

    /**
     * Pays a Lightning invoice using the configured store ID.
     *
     * @param bolt11 the BOLT11 invoice to pay
     * @return the payment result
     * @throws IllegalStateException if no store ID was configured at construction time
     * @throws BTCPayException       if the payment fails or the API returns an error
     * @see #payLightningInvoice(String, String)
     */
    public LightningPaymentResult payLightningInvoice(String bolt11) {
        return payLightningInvoice(requireStoreId(), bolt11);
    }

    /**
     * Checks Lightning node connectivity using the configured store ID.
     *
     * @return true if the server responds with HTTP 200; false if unreachable or error
     * @throws IllegalStateException if no store ID was configured at construction time
     * @see #isConnected(String)
     */
    public boolean isConnected() {
        return isConnected(requireStoreId());
    }

    // -------------------------------------------------------------------------
    // Explicit storeId overloads (multi-store and backward-compatible)
    // -------------------------------------------------------------------------

    /**
     * Creates a store invoice via the Greenfield API.
     *
     * <p>Endpoint: {@code POST /api/v1/stores/{storeId}/invoices}</p>
     *
     * <p><strong>Two-phase safety:</strong> Persist the returned {@link StoreInvoice#getId()}
     * before calling {@link #getLightningPaymentMethod}. The invoice exists in BTCPay as soon
     * as this method returns. If the subsequent payment-method fetch fails, use the persisted
     * ID to retry via {@link #getInvoicePaymentMethods}.</p>
     *
     * @param storeId the BTCPay store ID
     * @param request invoice creation parameters
     * @return the created invoice (contains the BTCPay invoice ID)
     * @throws BTCPayException if the API returns an error
     */
    public StoreInvoice createStoreInvoice(String storeId, CreateInvoiceRequest request) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        URI uri = URI.create(host + "/api/v1/stores/" + storeId + "/invoices");
        log.log(System.Logger.Level.DEBUG, "POST {0} Authorization: Token [REDACTED]", uri);

        try {
            byte[] body = objectMapper.writeValueAsBytes(request);
            HttpRequest httpRequest = authorizedRequest(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            return execute(httpRequest, StoreInvoice.class);
        } catch (BTCPayException e) {
            throw e;
        } catch (Exception e) {
            throw new BTCPayException("Failed to serialize invoice request: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all payment methods for an invoice.
     *
     * <p>Endpoint: {@code GET /api/v1/stores/{storeId}/invoices/{invoiceId}/payment-methods}</p>
     *
     * <p>If this call fails after a successful {@link #createStoreInvoice}, retry with the
     * stored invoice ID — the invoice persists in BTCPay regardless of this call's outcome.</p>
     *
     * @param storeId   the BTCPay store ID
     * @param invoiceId the BTCPay invoice ID (as returned by {@link #createStoreInvoice})
     * @return list of payment methods
     * @throws BTCPayException if the API returns an error
     */
    public List<InvoicePaymentMethod> getInvoicePaymentMethods(String storeId, String invoiceId) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(invoiceId, "invoiceId must not be null");

        URI uri = URI.create(host + "/api/v1/stores/" + storeId + "/invoices/" + invoiceId + "/payment-methods");
        log.log(System.Logger.Level.DEBUG, "GET {0} Authorization: Token [REDACTED]", uri);

        HttpRequest httpRequest = authorizedRequest(uri)
                .GET()
                .build();
        return execute(httpRequest, new TypeReference<>() {});
    }

    /**
     * Returns the Lightning payment method for an invoice, filtering explicitly for
     * {@code paymentType=="LightningLike"} and {@code cryptoCode=="BTC"}.
     *
     * <p><strong>Two-phase safety:</strong> This method calls {@link #getInvoicePaymentMethods}
     * internally. If it throws {@link BTCPayException}, the invoice created by
     * {@link #createStoreInvoice} still exists in BTCPay. Callers should persist the invoice ID
     * from {@code createStoreInvoice} before calling this method and retry on failure.</p>
     *
     * @param storeId   the BTCPay store ID
     * @param invoiceId the BTCPay invoice ID
     * @return Lightning payment method (with BOLT11 in {@link InvoicePaymentMethod#getDestination()}),
     *         or empty if no Lightning method exists for this invoice
     * @throws BTCPayException if the API returns an error
     */
    public Optional<InvoicePaymentMethod> getLightningPaymentMethod(String storeId, String invoiceId) {
        return getInvoicePaymentMethods(storeId, invoiceId).stream()
                .filter(m -> "LightningLike".equals(m.getPaymentType()) && "BTC".equals(m.getCryptoCode()))
                .findFirst();
    }

    /**
     * Returns the invoice by ID.
     *
     * <p>Endpoint: {@code GET /api/v1/stores/{storeId}/invoices/{invoiceId}}</p>
     *
     * @param storeId   the BTCPay store ID
     * @param invoiceId the BTCPay invoice ID
     * @return the invoice
     * @throws BTCPayException if the API returns an error
     */
    public StoreInvoice getInvoice(String storeId, String invoiceId) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(invoiceId, "invoiceId must not be null");

        URI uri = URI.create(host + "/api/v1/stores/" + storeId + "/invoices/" + invoiceId);
        log.log(System.Logger.Level.DEBUG, "GET {0} Authorization: Token [REDACTED]", uri);

        HttpRequest httpRequest = authorizedRequest(uri)
                .GET()
                .build();
        return execute(httpRequest, StoreInvoice.class);
    }

    /**
     * Returns true if the invoice has status {@link InvoiceStatus#SETTLED}.
     *
     * @param storeId   the BTCPay store ID
     * @param invoiceId the BTCPay invoice ID
     * @return true if the invoice is paid and settled
     * @throws BTCPayException if the API returns an error
     */
    public boolean isInvoicePaid(String storeId, String invoiceId) {
        return getInvoice(storeId, invoiceId).getStatus() == InvoiceStatus.SETTLED;
    }

    /**
     * Pays a Lightning invoice via the BTCPay store's Lightning node.
     *
     * <p>Endpoint: {@code POST /api/v1/stores/{storeId}/lightning/BTC/invoices/pay}</p>
     *
     * @param storeId the BTCPay store ID
     * @param bolt11  the BOLT11 invoice to pay
     * @return the payment result
     * @throws BTCPayException if the payment fails or API returns an error
     */
    public LightningPaymentResult payLightningInvoice(String storeId, String bolt11) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(bolt11, "bolt11 must not be null");

        URI uri = URI.create(host + "/api/v1/stores/" + storeId + "/lightning/BTC/invoices/pay");
        log.log(System.Logger.Level.DEBUG, "POST {0} Authorization: Token [REDACTED]", uri);

        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of("BOLT11", bolt11));
            HttpRequest httpRequest = authorizedRequest(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            return execute(httpRequest, LightningPaymentResult.class);
        } catch (BTCPayException e) {
            throw e;
        } catch (Exception e) {
            throw new BTCPayException("Failed to serialize pay request: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if this client can reach the BTCPay Server's Lightning node.
     *
     * <p>Endpoint: {@code GET /api/v1/stores/{storeId}/lightning/BTC/info}</p>
     *
     * @param storeId the BTCPay store ID
     * @return true if the server responds with HTTP 200; false if unreachable or error
     */
    public boolean isConnected(String storeId) {
        Objects.requireNonNull(storeId, "storeId must not be null");

        URI uri = URI.create(host + "/api/v1/stores/" + storeId + "/lightning/BTC/info");
        log.log(System.Logger.Level.DEBUG, "GET {0} Authorization: Token [REDACTED]", uri);

        try {
            HttpRequest httpRequest = authorizedRequest(uri)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.log(System.Logger.Level.WARNING, "BTCPay Server connectivity check interrupted");
            return false;
        } catch (Exception e) {
            log.log(System.Logger.Level.WARNING, "BTCPay Server connectivity check failed: {0}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String requireStoreId() {
        if (storeId == null) {
            throw new IllegalStateException(
                    "No storeId configured on this BTCPayClient. " +
                    "Either construct the client with a storeId, or use the explicit storeId overload.");
        }
        return storeId;
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Token " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(readTimeout);
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BTCPayException(response.statusCode(), response.body());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (BTCPayException e) {
            throw e;
        } catch (Exception e) {
            throw new BTCPayException("Failed to call BTCPay API: " + e.getMessage(), e);
        }
    }

    private <T> T execute(HttpRequest request, TypeReference<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BTCPayException(response.statusCode(), response.body());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (BTCPayException e) {
            throw e;
        } catch (Exception e) {
            throw new BTCPayException("Failed to call BTCPay API: " + e.getMessage(), e);
        }
    }
}
