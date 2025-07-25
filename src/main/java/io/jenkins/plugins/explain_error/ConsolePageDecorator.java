package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.util.Secret;

/**
 * Page decorator to add "Explain Error" functionality to console output pages.
 */
@Extension
public class ConsolePageDecorator extends PageDecorator {

    public ConsolePageDecorator() {
        super();
    }

    public boolean isExplainErrorEnabled() {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        return config.isEnableExplanation() && 
               !Secret.toString(config.getApiKey()).isBlank() && 
               config.getApiUrl() != null && !config.getApiUrl().trim().isEmpty();
    }
}
