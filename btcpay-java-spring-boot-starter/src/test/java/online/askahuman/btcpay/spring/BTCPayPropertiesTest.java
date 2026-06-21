package online.askahuman.btcpay.spring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BTCPayPropertiesTest {

    @Test
    void defaultsAreApplied() {
        BTCPayProperties props = new BTCPayProperties();
        assertThat(props.getConnectTimeoutSeconds()).isEqualTo(3);
        assertThat(props.getReadTimeoutSeconds()).isEqualTo(5);
        assertThat(props.getHost()).isNull();
        assertThat(props.getApiKey()).isNull();
        assertThat(props.getStoreId()).isNull();
        assertThat(props.getWebhookSecret()).isNull();
    }

    @Test
    void settersRoundTrip() {
        BTCPayProperties props = new BTCPayProperties();
        props.setHost("https://btcpay.example.com");
        props.setApiKey("key");
        props.setStoreId("store");
        props.setWebhookSecret("secret");
        props.setConnectTimeoutSeconds(10);
        props.setReadTimeoutSeconds(20);

        assertThat(props.getHost()).isEqualTo("https://btcpay.example.com");
        assertThat(props.getApiKey()).isEqualTo("key");
        assertThat(props.getStoreId()).isEqualTo("store");
        assertThat(props.getWebhookSecret()).isEqualTo("secret");
        assertThat(props.getConnectTimeoutSeconds()).isEqualTo(10);
        assertThat(props.getReadTimeoutSeconds()).isEqualTo(20);
    }
}
