package io.jenkins.plugins.explain_error;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * Build action to store and display error explanations.
 */
public class ErrorExplanationAction implements RunAction2 {

    private final String explanation;
    private final String originalErrorLogs;
    private final long timestamp;
    private transient Run<?, ?> run;

    public ErrorExplanationAction(String explanation, String originalErrorLogs) {
        this.explanation = explanation;
        this.originalErrorLogs = originalErrorLogs;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getIconFileName() {
        return "symbol-cube";
    }

    @Override
    public String getDisplayName() {
        return "AI Error Explanation";
    }

    @Override
    public String getUrlName() {
        return "error-explanation";
    }

    public String getExplanation() {
        return explanation;
    }

    public String getOriginalErrorLogs() {
        return originalErrorLogs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    /**
     * Get the associated run.
     * @return the run this action is attached to
     */
    public Run<?, ?> getRun() {
        return run;
    }
}
