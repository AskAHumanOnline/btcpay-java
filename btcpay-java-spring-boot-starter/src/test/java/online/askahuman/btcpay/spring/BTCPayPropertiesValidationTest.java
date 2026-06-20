package online.askahuman.btcpay.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link BTCPayProperties} fails fast on misconfiguration so consumers
 * discover missing credentials at application startup rather than on the first API call.
 */
class BTCPayPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class,
                    BTCPayAutoConfiguration.class));

    @Test
    void contextFailsToStart_whenApiKeyIsMissing() {
        runner.withPropertyValues("btcpay.host=https://btcpay.example.com")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .isInstanceOf(BeanCreationException.class)
                            .hasStackTraceContaining("apiKey")
                            .hasStackTraceContaining("must not be blank");
                });
    }

    @Test
    void contextFailsToStart_whenApiKeyIsBlank() {
        runner.withPropertyValues(
                        "btcpay.host=https://btcpay.example.com",
                        "btcpay.api-key=   ")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasStackTraceContaining("apiKey")
                            .hasStackTraceContaining("must not be blank");
                });
    }

    @Test
    void contextStarts_whenApiKeyIsProvided() {
        runner.withPropertyValues(
                        "btcpay.host=https://btcpay.example.com",
                        "btcpay.api-key=test-api-key")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(BTCPayProperties.class);
                    assertThat(ctx.getBean(BTCPayProperties.class).getApiKey()).isEqualTo("test-api-key");
                });
    }

    @Test
    void autoConfigurationDoesNotActivate_whenHostUnset() {
        // host is the activation condition; without it, no validation runs and no client is created.
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(BTCPayProperties.class);
        });
    }
}
