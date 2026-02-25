package io.sessioncast.spring.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for SessionCast Spring AI integration.
 *
 * <pre>
 * spring:
 *   ai:
 *     sessioncast:
 *       chat:
 *         enabled: true
 *         model: gpt-4o
 *         temperature: 0.7
 *         max-tokens: 4096
 * </pre>
 */
@ConfigurationProperties(prefix = "spring.ai.sessioncast.chat")
public class SessionCastAiProperties {

    private boolean enabled = true;

    private String model;

    private Double temperature;

    private Integer maxTokens;

    private Double topP;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }
}
