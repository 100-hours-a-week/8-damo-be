package com.team8.damo.config;

import com.team8.damo.security.handler.StompJwtChannelInterceptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler ->
            new WebSocketHandlerDecorator(handler) {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    activeWebSocketConnections.incrementAndGet();
                    super.afterConnectionEstablished(session);
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
                    throws Exception {
                    activeWebSocketConnections.decrementAndGet();
                    super.afterConnectionClosed(session, closeStatus);
                }
            }
        );
    }

    @Bean
    public MeterBinder webSocketSessionMetrics() {
        return registry -> Gauge.builder("websocket.connections.active",
                activeWebSocketConnections, AtomicInteger::get)
            .description("Current active WebSocket connections")
            .register(registry);
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/pub");
        registry
            .enableSimpleBroker("/sub")
            .setTaskScheduler(webSocketTaskScheduler())
            .setHeartbeatValue(new long[]{10000, 10000});
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws-stomp")
            .setAllowedOriginPatterns("https://localhost:3000", "http://localhost:5173", "https://damo.today");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
            .corePoolSize(20)
            .maxPoolSize(40)
            .queueCapacity(200);

        registration.interceptors(
            stompJwtChannelInterceptor,
            new SecurityContextChannelInterceptor()
        );
    }
}
