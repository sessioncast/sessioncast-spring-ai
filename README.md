# SessionCast Spring AI

Spring AI `ChatModel` integration for [SessionCast](https://sessioncast.io).

Use SessionCast as a standard Spring AI `ChatModel` — your application sends prompts through Spring AI's API, and SessionCast routes them to a CLI agent running on a user's machine, which forwards to their local LLM (Bring Your Own AI).

```
Spring AI Prompt → SessionCastClient.llmChat() → relay → CLI agent → local LLM → response
```

## Installation

### JitPack

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    // SessionCast base + Spring Boot starter
    implementation("com.github.sessioncast.sessioncast-java:sessioncast-spring-boot-starter:v1.1.0")

    // Spring AI integration (this module)
    implementation("com.github.sessioncast:sessioncast-spring-ai:v1.0.0")

    // Spring AI (manage your own BOM)
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.0"))
    implementation("org.springframework.ai:spring-ai-model")
}
```

```xml
<!-- Maven -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.sessioncast</groupId>
    <artifactId>sessioncast-spring-ai</artifactId>
    <version>v1.0.0</version>
</dependency>
```

## Configuration

```yaml
# application.yml
sessioncast:
  relay:
    url: wss://relay.sessioncast.io/ws
    token: ${SESSIONCAST_TOKEN}
  agent:
    machine-id: my-service

# Spring AI options (all optional — CLI agent defaults used if omitted)
spring:
  ai:
    sessioncast:
      chat:
        enabled: true          # default: true
        model: gpt-4o          # forwarded to CLI agent
        temperature: 0.7
        max-tokens: 4096
```

## Usage

### With ChatClient (recommended)

```java
@Service
public class MyService {

    private final ChatClient chatClient;

    public MyService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String question) {
        return chatClient.prompt(question).call().content();
    }
}
```

### With ChatModel directly

```java
@Service
public class AdvancedService {

    private final ChatModel chatModel;

    public AdvancedService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String question) {
        // Override defaults per-request
        ChatOptions options = SessionCastChatOptions.builder()
            .model("claude-3-opus")
            .temperature(0.0)
            .build();

        Prompt prompt = new Prompt(List.of(
            new SystemMessage("You are a code reviewer."),
            new UserMessage(question)
        ), options);

        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
```

## How It Works

1. `SessionCastChatModel` implements Spring AI's `ChatModel` interface
2. When `call(Prompt)` is invoked, it converts Spring AI messages to `LlmChatRequest`
3. `SessionCastClient.llmChat()` sends the request through the relay to the CLI agent
4. The CLI agent forwards to the user's local LLM and returns the response
5. `LlmChatResponse` is converted back to Spring AI's `ChatResponse`

## Requirements

- Java 17+
- Spring Boot 3.4+
- Spring AI 1.0+
- [sessioncast-spring-boot-starter](https://github.com/sessioncast/sessioncast-java) (provides `SessionCastClient` bean)

## License

MIT License
