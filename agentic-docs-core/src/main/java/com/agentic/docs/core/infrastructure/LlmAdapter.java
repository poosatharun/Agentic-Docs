package com.agentic.docs.core.infrastructure;

import com.agentic.docs.core.port.LlmPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Adapts Spring AI's {@link ChatClient} to the {@link LlmPort} interface.
 * To switch LLM providers (Ollama ↔ OpenAI), provide a different {@code ChatClient.Builder} bean
 * or write a new {@code @Primary} {@link LlmPort} implementation.
 */
@Component
public class LlmAdapter implements LlmPort {

    private final ChatClient chatClient;

    public LlmAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String systemPromptTemplate, String context, String question) {
        return chatClient.prompt()
                .system(s -> s.text(systemPromptTemplate).param("context", context))
                .user(question)
                .call()
                .content();
    }

    @Override
    public Flux<String> stream(String systemPromptTemplate, String context, String question) {
        return chatClient.prompt()
                .system(s -> s.text(systemPromptTemplate).param("context", context))
                .user(question)
                .stream()
                .content();
    }
}
