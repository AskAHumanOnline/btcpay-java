package online.askahuman.btcpay.examples;

import online.askahuman.btcpay.BTCPayClient;
import online.askahuman.btcpay.BTCPayWebhookValidator;
import online.askahuman.btcpay.model.CreateInvoiceRequest;
import online.askahuman.btcpay.model.InvoicePaymentMethod;
import online.askahuman.btcpay.model.StoreInvoice;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Demonstrates basic usage of the btcpay-java library.
 *
 * <p>This example shows how to:</p>
 * <ul>
 *   <li>Create a BTCPay Server invoice</li>
 *   <li>Retrieve the Lightning payment method (BOLT11 invoice)</li>
 *   <li>Check invoice payment status</li>
 *   <li>Validate webhook signatures</li>
 * </ul>
 *
 * <p>Replace the placeholder values with your actual BTCPay Server configuration.</p>
 */
public class BasicUsageExample {

    /**
     * Runs the example. Replace placeholder values with real configuration before running.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        // --- 1. Create a BTCPayClient ---
        // Replace with your BTCPay Server host and API key
        BTCPayClient client = new BTCPayClient(
                "https://your-btcpay-host.example.com",
                "your-api-key"
        );

        String storeId = "your-store-id";

        // --- 2. Create an invoice ---
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setAmount(25);                          // 25 satoshis
        request.setOrderId("order-12345");              // optional: for your tracking
        request.setDescription("Payment for service");  // optional: shown at checkout

        StoreInvoice invoice = client.createStoreInvoice(storeId, request);

        // The BTCPay invoice ID (e.g., "GjBd5TtU5VsEBVBNmRmANy")
        // This is NOT the Lightning payment hash — store it as your external reference
        String externalInvoiceId = invoice.getId();
        System.out.println("Created invoice: " + externalInvoiceId);
        System.out.println("Status: " + invoice.getStatus());

        // --- 3. Get the Lightning payment method ---
        Optional<InvoicePaymentMethod> lightning = client.getLightningPaymentMethod(storeId, externalInvoiceId);

        if (lightning.isPresent()) {
            // The BOLT11 invoice string — give this to the payer
            String bolt11 = lightning.get().getDestination();
            System.out.println("BOLT11 invoice: " + bolt11);
            System.out.println("Payment link: " + lightning.get().getPaymentLink());
        } else {
            System.out.println("No Lightning payment method available for this invoice.");
        }

        // --- 4. Check if invoice is paid ---
        boolean paid = client.isInvoicePaid(storeId, externalInvoiceId);
        System.out.println("Invoice paid: " + paid);

        // --- 5. Validate a webhook signature ---
        BTCPayWebhookValidator validator = new BTCPayWebhookValidator();
        byte[] webhookBody = "{\"type\":\"InvoiceSettled\"}".getBytes(StandardCharsets.UTF_8);
        String webhookSecret = "your-webhook-secret";
        String btcPaySigHeader = "sha256=..."; // from the BTCPay-Sig header

        boolean validSignature = validator.isValidSignature(webhookBody, webhookSecret, btcPaySigHeader);
        System.out.println("Webhook signature valid: " + validSignature);
    }
}
