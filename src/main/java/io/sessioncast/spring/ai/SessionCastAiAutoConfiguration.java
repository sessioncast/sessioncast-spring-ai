package io.sessioncast.spring.ai;

import io.sessioncast.core.SessionCastClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for SessionCast Spring AI integration.
 * <p>
 * Activated when both {@code SessionCastClient} and Spring AI {@code ChatModel}
 * are on the classpath and a {@code SessionCastClient} bean exists
 * (i.e., the user also has {@code sessioncast-spring-boot-starter}).
 */
@AutoConfiguration
@ConditionalOnClass({SessionCastClient.class, ChatModel.class})
@EnableConfigurationProperties(SessionCastAiProperties.class)
public class SessionCastAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SessionCastChatModel.class)
    @ConditionalOnBean(SessionCastClient.class)
    @ConditionalOnProperty(prefix = "spring.ai.sessioncast.chat", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public SessionCastChatModel sessionCastChatModel(
            SessionCastClient client, SessionCastAiProperties props) {

        SessionCastChatOptions defaultOptions = SessionCastChatOptions.builder()
                .model(props.getModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .topP(props.getTopP())
                .build();

        return new SessionCastChatModel(client, defaultOptions);
    }
}
