package com.team8.damo.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(150));

        RestClient restClient = RestClient.builder()
            .baseUrl("https://damo.today")
            .requestFactory(requestFactory)
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
