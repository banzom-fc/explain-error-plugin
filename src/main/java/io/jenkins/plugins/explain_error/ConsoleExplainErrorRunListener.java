package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import java.util.logging.Logger;

/**
 * Run listener to add ConsoleExplainErrorAction to all builds.
 */
@Extension
public class ConsoleExplainErrorRunListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(ConsoleExplainErrorRunListener.class.getName());

    @Override
    public void onStarted(Run<?, ?> run, hudson.model.TaskListener listener) {
        try {
            // Add console explain error action to all builds
            if (run.getAction(ConsoleExplainErrorAction.class) == null) {
                ConsoleExplainErrorAction action = new ConsoleExplainErrorAction(run);
                run.addAction(action);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to add ConsoleExplainErrorAction to build: " + run.getFullDisplayName() + ". Error: " + e.getMessage());
        }
    }
    
    @Override
    public void onCompleted(Run<?, ?> run, hudson.model.TaskListener listener) {
        try {
            // Ensure action is still available after completion
            if (run.getAction(ConsoleExplainErrorAction.class) == null) {
                ConsoleExplainErrorAction action = new ConsoleExplainErrorAction(run);
                run.addAction(action);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to add ConsoleExplainErrorAction to completed build: " + run.getFullDisplayName() + ". Error: " + e.getMessage());
        }
    }
}
