package online.askahuman.btcpay.spring;

import online.askahuman.btcpay.BTCPayClient;
import online.askahuman.btcpay.BTCPayWebhookValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for {@link BTCPayClient} and {@link BTCPayWebhookValidator}.
 *
 * <p>Activates when {@code btcpay.host} is set. The {@link BTCPayWebhookValidator} bean
 * is only created when {@code btcpay.webhook-secret} is also configured.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(BTCPayProperties.class)
@ConditionalOnProperty(prefix = "btcpay", name = "host")
public class BTCPayAutoConfiguration {

    /**
     * Creates a {@link BTCPayClient} bean using the configured properties.
     *
     * @param props the BTCPay configuration properties
     * @return a configured BTCPayClient
     */
    @Bean
    @ConditionalOnMissingBean
    public BTCPayClient btcPayClient(BTCPayProperties props) {
        return new BTCPayClient(
                props.getHost(),
                props.getApiKey(),
                props.getStoreId(),
                props.getConnectTimeoutSeconds(),
                props.getReadTimeoutSeconds());
    }

    /**
     * Creates a {@link BTCPayWebhookValidator} bean when a webhook secret is configured.
     *
     * @return a BTCPayWebhookValidator
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "btcpay", name = "webhook-secret")
    public BTCPayWebhookValidator btcPayWebhookValidator() {
        return new BTCPayWebhookValidator();
    }
}
