package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.model.PageDecorator;

/**
 * Page decorator to add "Explain Error" functionality to console output pages.
 */
@Extension
public class ConsolePageDecorator extends PageDecorator {

    public ConsolePageDecorator() {
        super();
    }

    public boolean isExplainErrorEnabled() {
        io.jenkins.plugins.explain_error.GlobalConfigurationImpl config = jenkins.model.Jenkins.get().getDescriptorByType(io.jenkins.plugins.explain_error.GlobalConfigurationImpl.class);
        return config != null && config.isEnableExplanation();
    }
}