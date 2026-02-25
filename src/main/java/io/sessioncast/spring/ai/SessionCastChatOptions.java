package io.sessioncast.spring.ai;

import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;

/**
 * {@link ChatOptions} implementation for SessionCast.
 * <p>
 * These options are forwarded to the CLI agent's local LLM via the relay.
 */
public class SessionCastChatOptions implements ChatOptions {

    private String model;
    private Double frequencyPenalty;
    private Integer maxTokens;
    private Double presencePenalty;
    private List<String> stopSequences;
    private Double temperature;
    private Integer topK;
    private Double topP;

    private SessionCastChatOptions() {
    }

    private SessionCastChatOptions(SessionCastChatOptions other) {
        this.model = other.model;
        this.frequencyPenalty = other.frequencyPenalty;
        this.maxTokens = other.maxTokens;
        this.presencePenalty = other.presencePenalty;
        this.stopSequences = other.stopSequences != null ? List.copyOf(other.stopSequences) : null;
        this.temperature = other.temperature;
        this.topK = other.topK;
        this.topP = other.topP;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    @Override
    public Integer getMaxTokens() {
        return maxTokens;
    }

    @Override
    public Double getPresencePenalty() {
        return presencePenalty;
    }

    @Override
    public List<String> getStopSequences() {
        return stopSequences;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    @Override
    public Double getTopP() {
        return topP;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SessionCastChatOptions copy() {
        return new SessionCastChatOptions(this);
    }

    public static class Builder {

        private final SessionCastChatOptions options = new SessionCastChatOptions();

        public Builder model(String model) {
            options.model = model;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            options.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            options.maxTokens = maxTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            options.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            options.stopSequences = stopSequences;
            return this;
        }

        public Builder temperature(Double temperature) {
            options.temperature = temperature;
            return this;
        }

        public Builder topK(Integer topK) {
            options.topK = topK;
            return this;
        }

        public Builder topP(Double topP) {
            options.topP = topP;
            return this;
        }

        public SessionCastChatOptions build() {
            return new SessionCastChatOptions(options);
        }
    }
}
