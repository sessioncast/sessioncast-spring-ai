package io.sessioncast.spring.ai;

import io.sessioncast.core.SessionCastClient;
import io.sessioncast.core.api.LlmChatRequest;
import io.sessioncast.core.api.LlmChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionCastChatModelTest {

    private SessionCastClient mockClient;
    private SessionCastChatOptions defaultOptions;
    private SessionCastChatModel chatModel;

    @BeforeEach
    void setUp() {
        mockClient = mock(SessionCastClient.class);
        defaultOptions = SessionCastChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.7)
                .maxTokens(4096)
                .build();
        chatModel = new SessionCastChatModel(mockClient, defaultOptions);
    }

    @Test
    void callShouldConvertPromptToRequestAndResponseBack() {
        // Given
        LlmChatResponse llmResponse = new LlmChatResponse(
                "chatcmpl-123",
                "gpt-4o",
                List.of(new LlmChatResponse.Choice(
                        0,
                        new LlmChatResponse.ChoiceMessage("assistant", "Hello from CLI agent!"),
                        "stop"
                )),
                new LlmChatResponse.Usage(10, 5, 15),
                null
        );

        ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
        when(mockClient.llmChat(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(llmResponse));

        // When
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("You are a helpful assistant."),
                new UserMessage("What is SessionCast?")
        ));
        ChatResponse response = chatModel.call(prompt);

        // Then: verify request conversion
        LlmChatRequest captured = requestCaptor.getValue();
        assertThat(captured.model()).isEqualTo("gpt-4o");
        assertThat(captured.temperature()).isEqualTo(0.7);
        assertThat(captured.maxTokens()).isEqualTo(4096);
        assertThat(captured.messages()).hasSize(2);
        assertThat(captured.messages().get(0).role()).isEqualTo("system");
        assertThat(captured.messages().get(0).content()).isEqualTo("You are a helpful assistant.");
        assertThat(captured.messages().get(1).role()).isEqualTo("user");
        assertThat(captured.messages().get(1).content()).isEqualTo("What is SessionCast?");

        // Then: verify response conversion
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);

        Generation generation = response.getResult();
        assertThat(generation.getOutput().getText()).isEqualTo("Hello from CLI agent!");
        assertThat(response.getMetadata().getModel()).isEqualTo("gpt-4o");
        assertThat(response.getMetadata().getUsage().getPromptTokens()).isEqualTo(10);
        assertThat(response.getMetadata().getUsage().getCompletionTokens()).isEqualTo(5);
        assertThat(response.getMetadata().getUsage().getTotalTokens()).isEqualTo(15);
    }

    @Test
    void callShouldMergeRuntimeOptionsOverDefaults() {
        // Given
        LlmChatResponse llmResponse = new LlmChatResponse(
                "id", "claude-3", List.of(new LlmChatResponse.Choice(
                0, new LlmChatResponse.ChoiceMessage("assistant", "ok"), "stop"
        )), null, null);

        ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
        when(mockClient.llmChat(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(llmResponse));

        // Runtime options override model and temperature, but not maxTokens
        ChatOptions runtimeOptions = SessionCastChatOptions.builder()
                .model("claude-3-opus")
                .temperature(0.0)
                .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("test")), runtimeOptions);
        chatModel.call(prompt);

        LlmChatRequest captured = requestCaptor.getValue();
        assertThat(captured.model()).isEqualTo("claude-3-opus");
        assertThat(captured.temperature()).isEqualTo(0.0);
        assertThat(captured.maxTokens()).isEqualTo(4096); // from defaults
    }

    @Test
    void callShouldThrowOnErrorResponse() {
        LlmChatResponse errorResponse = new LlmChatResponse(
                null, null, null, null,
                new LlmChatResponse.Error("CLI agent is not connected", "agent_offline")
        );
        when(mockClient.llmChat(any()))
                .thenReturn(CompletableFuture.completedFuture(errorResponse));

        Prompt prompt = new Prompt("hello");

        assertThatThrownBy(() -> chatModel.call(prompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CLI agent is not connected");
    }

    @Test
    void callShouldHandleNullUsage() {
        LlmChatResponse llmResponse = new LlmChatResponse(
                "id", "gpt-4o",
                List.of(new LlmChatResponse.Choice(
                        0, new LlmChatResponse.ChoiceMessage("assistant", "response"), "stop"
                )),
                null, null  // no usage data
        );
        when(mockClient.llmChat(any()))
                .thenReturn(CompletableFuture.completedFuture(llmResponse));

        ChatResponse response = chatModel.call(new Prompt("test"));

        assertThat(response.getResult().getOutput().getText()).isEqualTo("response");
        assertThat(response.getMetadata()).isNotNull();
    }

    @Test
    void defaultOptionsShouldReturnConfiguredOptions() {
        ChatOptions options = chatModel.getDefaultOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4o");
        assertThat(options.getTemperature()).isEqualTo(0.7);
        assertThat(options.getMaxTokens()).isEqualTo(4096);
    }

    @Test
    void chatOptionsCopyShouldBeIndependent() {
        SessionCastChatOptions original = SessionCastChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.5)
                .build();

        SessionCastChatOptions copy = original.copy();
        assertThat(copy.getModel()).isEqualTo("gpt-4o");
        assertThat(copy.getTemperature()).isEqualTo(0.5);
        assertThat(copy).isNotSameAs(original);
    }

    @Test
    void callWithNoDefaultOptionsShouldWork() {
        SessionCastChatModel noDefaults = new SessionCastChatModel(mockClient, null);

        LlmChatResponse llmResponse = new LlmChatResponse(
                "id", null,
                List.of(new LlmChatResponse.Choice(
                        0, new LlmChatResponse.ChoiceMessage("assistant", "hi"), "stop"
                )),
                null, null
        );
        when(mockClient.llmChat(any()))
                .thenReturn(CompletableFuture.completedFuture(llmResponse));

        ChatResponse response = noDefaults.call(new Prompt("hello"));
        assertThat(response.getResult().getOutput().getText()).isEqualTo("hi");
    }
}
