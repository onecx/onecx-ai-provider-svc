package org.tkit.onecx.ai.provider.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "onecx.ai.provider")
public interface AiProviderConfig {

    /**
     * The URL of the AI Runtime service.
     */
    @WithName("runtime")
    RuntimeClientConfig runtimeClient();

    interface RuntimeClientConfig {

        /**
         * The URL of the AI provider runtime service.
         */
        @WithName("url")
        @WithDefault("http://onecx-ai-provider-runtime:8080")
        String url();
    }
}
