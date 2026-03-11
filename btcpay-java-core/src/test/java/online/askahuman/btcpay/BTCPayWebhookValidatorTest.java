package online.askahuman.btcpay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BTCPayWebhookValidatorTest {

    private BTCPayWebhookValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BTCPayWebhookValidator();
    }

    private String computeHmac(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(body);
        return "sha256=" + HexFormat.of().formatHex(hash);
    }

    @Test
    void validSignature_returnsTrue() throws Exception {
        byte[] body = "{\"event\":\"invoice_settled\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "webhook-secret-123";
        String sig = computeHmac(secret, body);

        assertThat(validator.isValidSignature(body, secret, sig)).isTrue();
    }

    @Test
    void missingHeader_returnsFalse() {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, "secret", null)).isFalse();
    }

    @Test
    void emptyHeader_returnsFalse() {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, "secret", "")).isFalse();
    }

    @Test
    void wrongPrefix_returnsFalse() {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, "secret", "md5=abc")).isFalse();
    }

    @Test
    void tamperedBody_returnsFalse() throws Exception {
        byte[] originalBody = "original".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody = "tampered".getBytes(StandardCharsets.UTF_8);
        String secret = "webhook-secret";
        String sig = computeHmac(secret, originalBody);

        assertThat(validator.isValidSignature(tamperedBody, secret, sig)).isFalse();
    }

    @Test
    void wrongSecret_returnsFalse() throws Exception {
        byte[] body = "test-body".getBytes(StandardCharsets.UTF_8);
        String sig = computeHmac("correct-secret", body);

        assertThat(validator.isValidSignature(body, "wrong-secret", sig)).isFalse();
    }

    @Test
    void emptySecret_handled() {
        // Empty string secret causes IllegalArgumentException in SecretKeySpec,
        // so the validator should return false gracefully (not throw)
        byte[] body = "test-body".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, "", "sha256=abc")).isFalse();
    }

    @Test
    void listOverload_trueIfAnyMatches() throws Exception {
        byte[] body = "{\"event\":\"paid\"}".getBytes(StandardCharsets.UTF_8);
        String correctSecret = "secret-2";
        String sig = computeHmac(correctSecret, body);

        assertThat(validator.isValidSignature(body, List.of("wrong-secret", correctSecret), sig)).isTrue();
    }

    @Test
    void listOverload_falseIfNoneMatch() throws Exception {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);
        String sig = computeHmac("real-secret", body);

        assertThat(validator.isValidSignature(body, List.of("wrong-1", "wrong-2"), sig)).isFalse();
    }

    @Test
    void listOverload_emptyList_returnsFalse() {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, List.of(), "sha256=abc")).isFalse();
    }

    @Test
    void listOverload_nullList_returnsFalse() {
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, (List<String>) null, "sha256=abc")).isFalse();
    }

    @Test
    void nullBody_returnsFalse_singleSecret() {
        // null rawBody triggers NullPointerException in Mac.doFinal — must be caught and return false
        assertThat(validator.isValidSignature(null, "secret", "sha256=abc")).isFalse();
    }

    @Test
    void nullBody_returnsFalse_listOverload() {
        assertThat(validator.isValidSignature(null, List.of("secret"), "sha256=abc")).isFalse();
    }

    @Test
    void constantTimeComparison_noException() {
        // Different length strings should not throw — MessageDigest.isEqual handles them
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValidSignature(body, "secret", "sha256=short")).isFalse();
        assertThat(validator.isValidSignature(body, "secret",
                "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")).isFalse();
    }
}
