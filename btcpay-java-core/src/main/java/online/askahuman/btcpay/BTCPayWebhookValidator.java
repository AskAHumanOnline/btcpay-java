package online.askahuman.btcpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Validates BTCPay Server webhook signatures.
 *
 * <p>BTCPay signs webhook payloads with HMAC-SHA256 and includes the signature
 * in the {@code BTCPay-Sig} header as {@code sha256=<hex>}.</p>
 *
 * <p>Signature validation uses constant-time comparison to prevent timing attacks.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BTCPayWebhookValidator validator = new BTCPayWebhookValidator();
 * boolean valid = validator.isValidSignature(rawBodyBytes, webhookSecret, btcPaySigHeader);
 * }</pre>
 */
public class BTCPayWebhookValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIG_PREFIX = "sha256=";

    /**
     * Validates a webhook signature against a single secret.
     *
     * @param rawBody   the raw request body bytes (never re-serialized)
     * @param secret    the webhook secret configured in BTCPay
     * @param sigHeader the value of the {@code BTCPay-Sig} header
     * @return true if the signature is valid
     */
    public boolean isValidSignature(byte[] rawBody, String secret, String sigHeader) {
        if (secret == null || secret.isEmpty()) {
            // Fail fast on misconfiguration rather than silently swallowing the NPE/IAE
            // that SecretKeySpec would otherwise raise inside the catch block below.
            return false;
        }
        if (sigHeader == null || !sigHeader.startsWith(SIG_PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] computed = mac.doFinal(rawBody);
            String expected = SIG_PREFIX + HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    sigHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates a webhook signature against a list of secrets (for zero-downtime rotation).
     * Returns true if the signature is valid for ANY of the provided secrets.
     *
     * @param rawBody   the raw request body bytes
     * @param secrets   list of webhook secrets to try
     * @param sigHeader the value of the {@code BTCPay-Sig} header
     * @return true if the signature matches any secret
     */
    public boolean isValidSignature(byte[] rawBody, List<String> secrets, String sigHeader) {
        if (secrets == null || secrets.isEmpty()) {
            return false;
        }
        for (String secret : secrets) {
            if (isValidSignature(rawBody, secret, sigHeader)) {
                return true;
            }
        }
        return false;
    }
}
