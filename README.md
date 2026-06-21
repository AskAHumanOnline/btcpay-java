# btcpay-java

Java library for the [BTCPay Server Greenfield API](https://docs.btcpayserver.org/API/Greenfield/v1/) -- invoice creation, Lightning payment, webhook signature validation, and a Spring Boot starter for auto-configuration.

> **Built to power [AskAHuman](https://askahuman.online)** — a human verification marketplace where AI agents pay via Lightning to get answers from real people. Platform launching soon.

## Modules

| Module | Description |
|--------|-------------|
| `btcpay-java-core` | Pure Java client -- no Spring dependencies. Uses `java.net.http.HttpClient` and Jackson. |
| `btcpay-java-spring-boot-starter` | Spring Boot auto-configuration for `BTCPayClient` and `BTCPayWebhookValidator`. |
| `btcpay-java-examples` | Usage examples. |

## Requirements

- Java 25+
- BTCPay Server with Greenfield API enabled
- API key with `createinvoice`, `viewinvoices`, and (optionally) `sendlightning` permissions

## Maven Dependency

**Core only (no Spring):**

```xml
<dependency>
    <groupId>online.askahuman</groupId>
    <artifactId>btcpay-java-core</artifactId>
    <version>0.2.3</version>
</dependency>
```

**Spring Boot starter (includes core):**

```xml
<dependency>
    <groupId>online.askahuman</groupId>
    <artifactId>btcpay-java-spring-boot-starter</artifactId>
    <version>0.2.3</version>
</dependency>
```

## Quick Start

### Creating an Invoice

```java
BTCPayClient client = new BTCPayClient("https://btcpay.example.com", "your-api-key");

CreateInvoiceRequest request = new CreateInvoiceRequest();
request.setAmount(25); // 25 satoshis
request.setOrderId("order-12345");

StoreInvoice invoice = client.createStoreInvoice("your-store-id", request);
System.out.println("Invoice ID: " + invoice.getId());
```

### Getting the Lightning BOLT11 Invoice

```java
Optional<InvoicePaymentMethod> lightning =
    client.getLightningPaymentMethod("your-store-id", invoice.getId());

if (lightning.isPresent()) {
    String bolt11 = lightning.get().getDestination();
    System.out.println("Pay this invoice: " + bolt11);
}
```

### Checking Payment Status

```java
boolean paid = client.isInvoicePaid("your-store-id", invoice.getId());
```

### Paying a Lightning Invoice

```java
LightningPaymentResult result = client.payLightningInvoice("your-store-id", bolt11);
System.out.println("Payment result: " + result.getResult());
```

### Validating Webhook Signatures

BTCPay Server signs webhook payloads with HMAC-SHA256. The signature is in the `BTCPay-Sig` header.

```java
BTCPayWebhookValidator validator = new BTCPayWebhookValidator();

boolean valid = validator.isValidSignature(
    rawRequestBodyBytes,
    "your-webhook-secret",
    request.getHeader("BTCPay-Sig")
);
```

For zero-downtime secret rotation, pass a list of secrets:

```java
boolean valid = validator.isValidSignature(
    rawRequestBodyBytes,
    List.of("new-secret", "old-secret"),
    btcPaySigHeader
);
```

## Spring Boot Auto-Configuration

Add `btcpay-java-spring-boot-starter` to your dependencies and configure in `application.yml`:

```yaml
btcpay:
  host: https://btcpay.example.com
  api-key: ${BTCPAY_API_KEY}
  webhook-secret: ${BTCPAY_WEBHOOK_SECRET}
  connect-timeout-seconds: 3
  read-timeout-seconds: 5
```

The starter auto-creates:
- `BTCPayClient` bean (when `btcpay.host` is set)
- `BTCPayWebhookValidator` bean (when `btcpay.webhook-secret` is also set)

Both beans are `@ConditionalOnMissingBean`, so you can provide your own if needed.

## Security

- API keys are **never logged**. All log statements redact the `Authorization` header value.
- Webhook signature validation uses **constant-time comparison** (`MessageDigest.isEqual`) to prevent timing attacks.
- Always pass the **raw request body bytes** to the webhook validator -- never re-serialize the parsed JSON.
- `btcpay.api-key` is enforced as non-blank at application startup via Bean Validation. Missing credentials surface as a startup failure instead of a runtime 401.
- **Spring Boot Actuator exposure:** `btcpay.api-key` and `btcpay.webhook-secret` are sensitive. Spring Boot 3.x sanitizes properties matching `password`, `secret`, `key`, `token`, etc. on the `/env` and `/configprops` endpoints by default, so both keys are redacted out of the box. If you customize `management.endpoint.env.show-values` / `show-keys` or supply a custom `SanitizingFunction`, make sure `btcpay.*` properties remain sanitized.

## License

[Apache License 2.0](LICENSE)
