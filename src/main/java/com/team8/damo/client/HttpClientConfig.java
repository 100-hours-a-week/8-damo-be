package com.team8.damo.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory() {
        RestClient restClient = RestClient.builder()
            .baseUrl("https://damo.today")
            .build();

        return HttpServiceProxyFactory.builder()
            .exchangeAdapter(RestClientAdapter.create(restClient))
            .build();
    }

    @Bean
    public AiClient aiClient(HttpServiceProxyFactory factory) {
        return factory.createClient(AiClient.class);
    }
}
