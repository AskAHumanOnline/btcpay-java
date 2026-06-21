package online.askahuman.btcpay;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import online.askahuman.btcpay.model.CreateInvoiceRequest;
import online.askahuman.btcpay.model.InvoicePaymentMethod;
import online.askahuman.btcpay.model.LightningPayment;
import online.askahuman.btcpay.model.LightningPaymentResult;
import online.askahuman.btcpay.model.StoreInvoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BTCPayClientTest {

    private static final String STORE_ID = "test-store-id";
    private static final String INVOICE_ID = "GjBd5TtU5VsEBVBNmRmANy";
    private static final String API_KEY = "test-api-key-secret";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private BTCPayClient client;

    @BeforeEach
    void setUp() {
        client = new BTCPayClient("http://localhost:" + wireMock.getPort(), API_KEY, 3, 5);
    }

    @Test
    void createStoreInvoice_returnsInvoiceWithId() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .willReturn(okJson("""
                        {"id":"abc123","status":"New","amount":"25","currency":"SATS"}
                        """)));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);

        StoreInvoice invoice = client.createStoreInvoice(STORE_ID, request);

        assertThat(invoice.getId()).isEqualTo("abc123");
        assertThat(invoice.getAmount()).isEqualTo("25");
        assertThat(invoice.getCurrency()).isEqualTo("SATS");
    }

    @Test
    void createStoreInvoice_throwsOnServerError() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);

        assertThatThrownBy(() -> client.createStoreInvoice(STORE_ID, request))
                .isInstanceOf(BTCPayException.class)
                .satisfies(ex -> assertThat(((BTCPayException) ex).getStatusCode()).isEqualTo(500));
    }

    @Test
    void getInvoicePaymentMethods_returnsLightningMethod() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID + "/payment-methods"))
                .willReturn(okJson("""
                        [{"paymentMethodId":"BTC-LN","cryptoCode":"BTC","paymentType":"LightningLike",
                          "destination":"lnbc250n1...","paymentLink":"lightning:lnbc250n1...","amount":"25"}]
                        """)));

        List<InvoicePaymentMethod> methods = client.getInvoicePaymentMethods(STORE_ID, INVOICE_ID);

        assertThat(methods).hasSize(1);
        assertThat(methods.getFirst().getPaymentType()).isEqualTo("LightningLike");
        assertThat(methods.getFirst().getCryptoCode()).isEqualTo("BTC");
    }

    @Test
    void getLightningPaymentMethod_returnsEmpty_whenNoLightningMethod() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID + "/payment-methods"))
                .willReturn(okJson("""
                        [{"paymentMethodId":"BTC","cryptoCode":"BTC","paymentType":"BTCLike",
                          "destination":"bc1q...","amount":"25"}]
                        """)));

        Optional<InvoicePaymentMethod> lightning = client.getLightningPaymentMethod(STORE_ID, INVOICE_ID);

        assertThat(lightning).isEmpty();
    }

    @Test
    void getLightningPaymentMethod_filtersCorrectly() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID + "/payment-methods"))
                .willReturn(okJson("""
                        [
                          {"paymentMethodId":"BTC","cryptoCode":"BTC","paymentType":"BTCLike",
                           "destination":"bc1q...","amount":"25"},
                          {"paymentMethodId":"BTC-LN","cryptoCode":"BTC","paymentType":"LightningLike",
                           "destination":"lnbc250n1pjtest...","paymentLink":"lightning:lnbc250n1pjtest...","amount":"25"}
                        ]
                        """)));

        Optional<InvoicePaymentMethod> lightning = client.getLightningPaymentMethod(STORE_ID, INVOICE_ID);

        assertThat(lightning).isPresent();
        assertThat(lightning.get().getPaymentType()).isEqualTo("LightningLike");
        assertThat(lightning.get().getDestination()).startsWith("lnbc");
    }

    @Test
    void isInvoicePaid_returnsTrueForSettled() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .willReturn(okJson("""
                        {"id":"%s","status":"Settled","amount":"25","currency":"SATS"}
                        """.formatted(INVOICE_ID))));

        assertThat(client.isInvoicePaid(STORE_ID, INVOICE_ID)).isTrue();
    }

    @Test
    void isInvoicePaid_returnsFalseForNew() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .willReturn(okJson("""
                        {"id":"%s","status":"New","amount":"25","currency":"SATS"}
                        """.formatted(INVOICE_ID))));

        assertThat(client.isInvoicePaid(STORE_ID, INVOICE_ID)).isFalse();
    }

    @Test
    void payLightningInvoice_success() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/invoices/pay"))
                .willReturn(okJson("""
                        {"paymentHash":"abc123def","status":"complete","totalAmount":"25","feeAmount":"1","BOLT11":"lnbc250n1pjtest..."}
                        """)));

        LightningPaymentResult result = client.payLightningInvoice(STORE_ID, "lnbc250n1pjtest...");

        assertThat(result.getPaymentHash()).isEqualTo("abc123def");
        assertThat(result.getStatus()).isEqualTo("complete");
        assertThat(result.getTotalAmount()).isEqualTo("25");
        assertThat(result.getFeeAmount()).isEqualTo("1");
        assertThat(result.getBolt11()).isEqualTo("lnbc250n1pjtest...");
    }

    @Test
    void payLightningInvoice_throwsOnError() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/invoices/pay"))
                .willReturn(aResponse().withStatus(422).withBody("Unprocessable Entity")));

        assertThatThrownBy(() -> client.payLightningInvoice(STORE_ID, "lnbc250n1pjtest..."))
                .isInstanceOf(BTCPayException.class)
                .satisfies(ex -> assertThat(((BTCPayException) ex).getStatusCode()).isEqualTo(422));
    }

    @Test
    void isConnected_returnsTrue_on200() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/info"))
                .willReturn(okJson("{}")));

        assertThat(client.isConnected(STORE_ID)).isTrue();
    }

    @Test
    void isConnected_returnsFalse_on503() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/info"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(client.isConnected(STORE_ID)).isFalse();
    }

    @Test
    void authorizationHeader_isSentCorrectly() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .withHeader("Authorization", equalTo("Token " + API_KEY))
                .willReturn(okJson("""
                        {"id":"%s","status":"New","amount":"25","currency":"SATS"}
                        """.formatted(INVOICE_ID))));

        StoreInvoice invoice = client.getInvoice(STORE_ID, INVOICE_ID);

        assertThat(invoice.getId()).isEqualTo(INVOICE_ID);
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .withHeader("Authorization", equalTo("Token " + API_KEY)));
    }

    @Test
    void externalInvoiceId_isNotEqualTo_paymentHash() {
        // BTCPay invoice IDs (e.g., "GjBd5TtU5VsEBVBNmRmANy") are short alphanumeric strings.
        // Lightning payment hashes are 64 hex characters. They must never be confused.
        String btcpayInvoiceId = "GjBd5TtU5VsEBVBNmRmANy";
        String paymentHash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .willReturn(okJson("""
                        {"id":"%s","status":"New","amount":"25","currency":"SATS"}
                        """.formatted(btcpayInvoiceId))));

        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + btcpayInvoiceId + "/payment-methods"))
                .willReturn(okJson("""
                        [{"paymentMethodId":"BTC-LN","cryptoCode":"BTC","paymentType":"LightningLike",
                          "destination":"lnbc250n1pj%s...","paymentLink":"lightning:lnbc250n1pj...","amount":"25"}]
                        """.formatted(paymentHash))));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);
        StoreInvoice invoice = client.createStoreInvoice(STORE_ID, request);

        List<InvoicePaymentMethod> methods = client.getInvoicePaymentMethods(STORE_ID, invoice.getId());

        // The invoice ID and the destination (containing the payment hash) are fundamentally different identifiers
        assertThat(invoice.getId()).isNotEqualTo(paymentHash);
        assertThat(invoice.getId()).isEqualTo(btcpayInvoiceId);
        assertThat(methods.getFirst().getDestination()).contains(paymentHash);
    }

    @Test
    void createStoreInvoice_sendsAuthorizationHeader() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .withHeader("Authorization", equalTo("Token " + API_KEY))
                .willReturn(okJson("""
                        {"id":"abc","status":"New","amount":"25","currency":"SATS"}
                        """)));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);
        client.createStoreInvoice(STORE_ID, request);

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .withHeader("Authorization", equalTo("Token " + API_KEY))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void getInvoice_returnsCorrectStatus() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .willReturn(okJson("""
                        {"id":"%s","status":"Processing","amount":"25","currency":"SATS"}
                        """.formatted(INVOICE_ID))));

        StoreInvoice invoice = client.getInvoice(STORE_ID, INVOICE_ID);

        assertThat(invoice.getStatus()).isEqualTo(online.askahuman.btcpay.model.InvoiceStatus.PROCESSING);
    }

    @Test
    void constructor_prependsHttps_whenNoScheme() {
        // Client should prepend https:// when host doesn't start with "http"
        BTCPayClient httpsClient = new BTCPayClient("localhost:" + wireMock.getPort(), API_KEY, 3, 5);

        // This will fail to connect via HTTPS to an HTTP WireMock, proving the scheme was prepended
        assertThatThrownBy(() -> httpsClient.getInvoice(STORE_ID, INVOICE_ID))
                .isInstanceOf(BTCPayException.class);
    }

    @Test
    void payLightningInvoice_sendsUpperCaseBolt11Key() {
        // BTCPay Greenfield API requires the field name "BOLT11" (uppercase) in the request body
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/invoices/pay"))
                .withRequestBody(matchingJsonPath("$.BOLT11"))
                .willReturn(okJson("""
                        {"paymentHash":"abc123def","status":"complete","totalAmount":"25","feeAmount":"1","BOLT11":"lnbc250n1pjtest..."}
                        """)));

        LightningPaymentResult result = client.payLightningInvoice(STORE_ID, "lnbc250n1pjtest...");
        assertThat(result.getStatus()).isEqualTo("complete");

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/invoices/pay"))
                .withRequestBody(matchingJsonPath("$.BOLT11", equalTo("lnbc250n1pjtest..."))));
    }

    @Test
    void noArgOverloads_useConfiguredStoreId() {
        BTCPayClient clientWithStore = new BTCPayClient(
                "http://localhost:" + wireMock.getPort(), API_KEY, STORE_ID, 3, 5);

        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices/" + INVOICE_ID))
                .willReturn(okJson("""
                        {"id":"%s","status":"Settled","amount":"25","currency":"SATS"}
                        """.formatted(INVOICE_ID))));

        assertThat(clientWithStore.isInvoicePaid(INVOICE_ID)).isTrue();
    }

    @Test
    void noArgOverload_throwsIllegalState_whenNoStoreIdConfigured() {
        // client was built without a storeId
        assertThatThrownBy(() -> client.isInvoicePaid(INVOICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void configuredStoreId_createInvoice_noArgOverload() {
        BTCPayClient clientWithStore = new BTCPayClient(
                "http://localhost:" + wireMock.getPort(), API_KEY, STORE_ID, 3, 5);

        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .willReturn(okJson("""
                        {"id":"abc123","status":"New","amount":"25","currency":"SATS"}
                        """)));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);
        StoreInvoice invoice = clientWithStore.createStoreInvoice(request);

        assertThat(invoice.getId()).isEqualTo("abc123");
    }

    @Test
    void getLightningPayment_complete_returnsPayment() {
        String hash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/payments/" + hash))
                .willReturn(okJson("""
                        {"id":"%s","status":"Complete"}
                        """.formatted(hash))));

        LightningPayment payment = client.getLightningPayment(STORE_ID, hash);

        assertThat(payment.getPaymentHash()).isEqualTo(hash);
        assertThat(payment.getStatus()).isEqualTo("Complete");
    }

    @Test
    void getLightningPayment_processing_returnsPayment() {
        String hash = "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3";
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/payments/" + hash))
                .willReturn(okJson("""
                        {"id":"%s","status":"Processing"}
                        """.formatted(hash))));

        LightningPayment payment = client.getLightningPayment(STORE_ID, hash);

        assertThat(payment.getPaymentHash()).isEqualTo(hash);
        assertThat(payment.getStatus()).isEqualTo("Processing");
    }

    @Test
    void getLightningPayment_notFound_throwsBTCPayException404() {
        String hash = "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/lightning/BTC/payments/" + hash))
                .willReturn(aResponse().withStatus(404).withBody("Not Found")));

        assertThatThrownBy(() -> client.getLightningPayment(STORE_ID, hash))
                .isInstanceOf(BTCPayException.class)
                .satisfies(ex -> assertThat(((BTCPayException) ex).getStatusCode()).isEqualTo(404));
    }

    @Test
    void getLightningPayment_noStoreId_throwsIllegalState() {
        // client was built without a storeId
        assertThatThrownBy(() -> client.getLightningPayment("somehash"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void createStoreInvoice_includesMetadataAndCheckout() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/stores/" + STORE_ID + "/invoices"))
                .withRequestBody(matchingJsonPath("$.metadata.orderId", equalTo("order-123")))
                .withRequestBody(matchingJsonPath("$.checkout.description", equalTo("Test payment")))
                .willReturn(okJson("""
                        {"id":"abc","status":"New","amount":"25","currency":"SATS"}
                        """)));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);
        request.setOrderId("order-123");
        request.setDescription("Test payment");

        StoreInvoice invoice = client.createStoreInvoice(STORE_ID, request);
        assertThat(invoice.getId()).isEqualTo("abc");
    }

    // -------------------------------------------------------------------------
    // Path-segment validation (issue #3)
    // -------------------------------------------------------------------------

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "",                                 // empty
            "foo/bar",                          // raw separator
            "..",                               // parent dir
            "store/../other",                   // traversal
            "store%2F..%2Fother",               // percent-encoded separator
            "store id",                         // whitespace
            "store?admin",                      // query injection
            "store#frag"                        // fragment
    })
    void createStoreInvoice_rejectsInvalidStoreId(String badId) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);

        assertThatThrownBy(() -> client.createStoreInvoice(badId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void createStoreInvoice_rejectsNullStoreId() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);

        assertThatThrownBy(() -> client.createStoreInvoice(null, request))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void getInvoicePaymentMethods_rejectsTraversalInInvoiceId() {
        assertThatThrownBy(() -> client.getInvoicePaymentMethods(STORE_ID, "abc/../../admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invoiceId");
    }

    @Test
    void getInvoice_rejectsTraversalInInvoiceId() {
        assertThatThrownBy(() -> client.getInvoice(STORE_ID, ".."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invoiceId");
    }

    @Test
    void payLightningInvoice_rejectsTraversalInStoreId() {
        assertThatThrownBy(() -> client.payLightningInvoice("store/../x", "lnbc..."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void getLightningPayment_rejectsInvalidPaymentHash() {
        assertThatThrownBy(() -> client.getLightningPayment(STORE_ID, "abc/../def"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentHash");
    }

    @Test
    void isConnected_rejectsTraversalInStoreId() {
        assertThatThrownBy(() -> client.isConnected("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void constructor_rejectsInvalidConfiguredStoreId() {
        assertThatThrownBy(() -> new BTCPayClient(
                "http://localhost:" + wireMock.getPort(), API_KEY, "store/../x", 3, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storeId");
    }

    @Test
    void constructor_acceptsValidStoreIdCharacters() {
        // alphanumeric, hyphen, and underscore are all legal in BTCPay identifiers
        BTCPayClient ok = new BTCPayClient(
                "http://localhost:" + wireMock.getPort(), API_KEY, "Store-1_abc", 3, 5);
        assertThat(ok).isNotNull();
    }

    @Test
    void validation_runsBeforeUrlConstruction_noHttpCall() {
        // If validation rejects, no HTTP request must be sent.
        wireMock.resetRequests();
        assertThatThrownBy(() -> client.getInvoice("../bad", INVOICE_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(wireMock.getAllServeEvents()).isEmpty();
    }
}
