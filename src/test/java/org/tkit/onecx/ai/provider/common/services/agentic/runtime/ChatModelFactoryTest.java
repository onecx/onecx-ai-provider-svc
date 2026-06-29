package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.model.chat.ChatModel;

class ChatModelFactoryTest {

    ProviderAdapter ollamaAdapter;
    ProviderAdapter openAiAdapter;
    ChatModelFactory factory;

    @BeforeEach
    void setUp() {
        ollamaAdapter = mock(ProviderAdapter.class);
        when(ollamaAdapter.supports(ProviderType.OLLAMA)).thenReturn(true);
        openAiAdapter = mock(ProviderAdapter.class);
        when(openAiAdapter.supports(ProviderType.OPENAI)).thenReturn(true);

        factory = new ChatModelFactory();
        factory.providerAdapters = instance(List.of(ollamaAdapter, openAiAdapter));
    }

    @Test
    void createChatModel_delegatesToMatchingAdapter() {
        Agent agent = agent(ProviderType.OLLAMA);
        ChatModel chatModel = mock(ChatModel.class);
        when(ollamaAdapter.createChatModel(agent)).thenReturn(chatModel);

        assertThat(factory.createChatModel(agent)).isSameAs(chatModel);
    }

    @Test
    void healthCheck_delegatesToMatchingAdapter() {
        Provider provider = provider(ProviderType.OLLAMA);
        when(ollamaAdapter.healthCheck(provider)).thenReturn(ProviderAdapter.HEALTHY);

        assertThat(factory.healthCheck(provider)).isEqualTo(ProviderAdapter.HEALTHY);
    }

    @Test
    void createChatModel_resolvesOpenAiAdapter() {
        Agent agent = agent(ProviderType.OPENAI);
        ChatModel chatModel = mock(ChatModel.class);
        when(openAiAdapter.createChatModel(agent)).thenReturn(chatModel);

        assertThat(factory.createChatModel(agent)).isSameAs(chatModel);
    }

    @Test
    void createChatModel_unsupportedProvider_failsClearly() {
        assertThatThrownBy(() -> factory.createChatModel(agent(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider has no type configured");
    }

    private Agent agent(ProviderType providerType) {
        Model model = new Model();
        model.setProvider(provider(providerType));
        Agent agent = new Agent();
        agent.setModel(model);
        return agent;
    }

    private Provider provider(ProviderType type) {
        Provider provider = new Provider();
        provider.setType(type);
        return provider;
    }

    private Instance<ProviderAdapter> instance(List<ProviderAdapter> adapters) {
        return new Instance<>() {
            @Override
            public Iterator<ProviderAdapter> iterator() {
                return adapters.iterator();
            }

            @Override
            public ProviderAdapter get() {
                return adapters.getFirst();
            }

            @Override
            public Instance<ProviderAdapter> select(jakarta.enterprise.util.TypeLiteral subtype,
                    java.lang.annotation.Annotation... qualifiers) {
                return this;
            }

            @Override
            public Instance<ProviderAdapter> select(Class subtype, java.lang.annotation.Annotation... qualifiers) {
                return this;
            }

            @Override
            public Instance<ProviderAdapter> select(java.lang.annotation.Annotation... qualifiers) {
                return this;
            }

            @Override
            public boolean isUnsatisfied() {
                return adapters.isEmpty();
            }

            @Override
            public boolean isAmbiguous() {
                return adapters.size() > 1;
            }

            @Override
            public void destroy(ProviderAdapter instance) {
            }

            @Override
            public Handle<ProviderAdapter> getHandle() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<? extends Handle<ProviderAdapter>> handles() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
