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
}