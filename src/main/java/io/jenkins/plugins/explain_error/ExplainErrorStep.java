package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Pipeline step to explain errors using AI.
 */
public class ExplainErrorStep extends Step {

    private String logPattern;
    private int maxLines;

    @DataBoundConstructor
    public ExplainErrorStep() {
        this.logPattern = "";
        this.maxLines = 100;
    }

    public String getLogPattern() {
        return logPattern;
    }

    @DataBoundSetter
    public void setLogPattern(String logPattern) {
        this.logPattern = logPattern != null ? logPattern : "";
    }

    public int getMaxLines() {
        return maxLines;
    }

    @DataBoundSetter
    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines > 0 ? maxLines : 100;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExplainErrorStepExecution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class, FilePath.class, Launcher.class);
        }

        @Override
        public String getFunctionName() {
            return "explainError";
        }

        @Override
        public String getDisplayName() {
            return "Explain Error with AI";
        }
    }

    private static class ExplainErrorStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;
        private final transient ExplainErrorStep step;

        ExplainErrorStepExecution(StepContext context, ExplainErrorStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            TaskListener listener = getContext().get(TaskListener.class);

            // Add console explain error action to build (if not already present)
            if (run.getAction(ConsoleExplainErrorAction.class) == null) {
                run.addAction(new ConsoleExplainErrorAction(run));
            }

            ErrorExplainer explainer = new ErrorExplainer();
            explainer.explainError(run, listener, step.getLogPattern(), step.getMaxLines());

            return null;
        }
    }
}
