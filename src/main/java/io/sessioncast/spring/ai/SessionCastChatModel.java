package io.sessioncast.spring.ai;

import io.sessioncast.core.SessionCastClient;
import io.sessioncast.core.api.LlmChatRequest;
import io.sessioncast.core.api.LlmChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI {@link ChatModel} implementation that delegates to a SessionCast CLI agent.
 * <p>
 * The request flow is:
 * <pre>
 *   Spring AI Prompt
 *     → LlmChatRequest
 *       → SessionCastClient.llmChat()
 *         → relay → CLI agent → local LLM
 *           → LlmChatResponse
 *             → Spring AI ChatResponse
 * </pre>
 */
public class SessionCastChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(SessionCastChatModel.class);

    private final SessionCastClient client;
    private final SessionCastChatOptions defaultOptions;

    public SessionCastChatModel(SessionCastClient client, SessionCastChatOptions defaultOptions) {
        this.client = client;
        this.defaultOptions = defaultOptions != null ? defaultOptions : SessionCastChatOptions.builder().build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        LlmChatRequest request = toRequest(prompt);
        log.debug("Sending LLM chat request via SessionCast: model={}", request.model());

        LlmChatResponse response;
        try {
            response = client.llmChat(request).join();
        } catch (Exception e) {
            throw new RuntimeException("SessionCast LLM chat failed", e);
        }

        if (response.hasError()) {
            throw new RuntimeException("SessionCast LLM chat error: " + response.error().message());
        }

        return toChatResponse(response);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // Streaming is not supported — fall back to a single-element Flux
        return Flux.just(call(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return defaultOptions;
    }

    // ---- Conversion: Spring AI → SessionCast ----

    private LlmChatRequest toRequest(Prompt prompt) {
        ChatOptions mergedOptions = mergeOptions(prompt.getOptions());

        LlmChatRequest.Builder builder = LlmChatRequest.builder();

        // Model & parameters
        if (mergedOptions.getModel() != null) {
            builder.model(mergedOptions.getModel());
        }
        if (mergedOptions.getTemperature() != null) {
            builder.temperature(mergedOptions.getTemperature());
        }
        if (mergedOptions.getMaxTokens() != null) {
            builder.maxTokens(mergedOptions.getMaxTokens());
        }

        // Messages
        for (Message message : prompt.getInstructions()) {
            String role = toRole(message.getMessageType());
            builder.addMessage(role, message.getText());
        }

        return builder.build();
    }

    private ChatOptions mergeOptions(ChatOptions runtimeOptions) {
        if (runtimeOptions == null) {
            return defaultOptions;
        }
        // Runtime options override defaults where non-null
        return SessionCastChatOptions.builder()
                .model(runtimeOptions.getModel() != null ? runtimeOptions.getModel() : defaultOptions.getModel())
                .temperature(runtimeOptions.getTemperature() != null ? runtimeOptions.getTemperature() : defaultOptions.getTemperature())
                .maxTokens(runtimeOptions.getMaxTokens() != null ? runtimeOptions.getMaxTokens() : defaultOptions.getMaxTokens())
                .topP(runtimeOptions.getTopP() != null ? runtimeOptions.getTopP() : defaultOptions.getTopP())
                .topK(runtimeOptions.getTopK() != null ? runtimeOptions.getTopK() : defaultOptions.getTopK())
                .frequencyPenalty(runtimeOptions.getFrequencyPenalty() != null ? runtimeOptions.getFrequencyPenalty() : defaultOptions.getFrequencyPenalty())
                .presencePenalty(runtimeOptions.getPresencePenalty() != null ? runtimeOptions.getPresencePenalty() : defaultOptions.getPresencePenalty())
                .stopSequences(runtimeOptions.getStopSequences() != null ? runtimeOptions.getStopSequences() : defaultOptions.getStopSequences())
                .build();
    }

    private static String toRole(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    // ---- Conversion: SessionCast → Spring AI ----

    private ChatResponse toChatResponse(LlmChatResponse response) {
        List<Generation> generations = new ArrayList<>();

        if (response.choices() != null) {
            for (LlmChatResponse.Choice choice : response.choices()) {
                String content = choice.message() != null ? choice.message().content() : "";
                AssistantMessage assistantMessage = new AssistantMessage(content);

                ChatGenerationMetadata metadata = ChatGenerationMetadata.builder()
                        .finishReason(choice.finishReason())
                        .build();

                generations.add(new Generation(assistantMessage, metadata));
            }
        }

        ChatResponseMetadata.Builder metadataBuilder = ChatResponseMetadata.builder();
        if (response.model() != null) {
            metadataBuilder.model(response.model());
        }
        if (response.id() != null) {
            metadataBuilder.id(response.id());
        }
        if (response.usage() != null) {
            metadataBuilder.usage(new DefaultUsage(
                    response.usage().promptTokens(),
                    response.usage().completionTokens(),
                    response.usage().totalTokens()
            ));
        }

        return new ChatResponse(generations, metadataBuilder.build());
    }
}
