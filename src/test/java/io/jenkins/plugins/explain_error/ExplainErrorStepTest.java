package io.jenkins.plugins.explain_error;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ExplainErrorStepTest {

    @Test
    void testExplainErrorStep(JenkinsRule jenkins) throws Exception {
        // Create a test pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-explain-error");

        // Define a pipeline that will fail and then explain the error
        String pipelineScript = "pipeline {\n" + "    agent any\n"
                + "    stages {\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                script {\n"
                + "                    // This will fail\n"
                + "                    sh 'nonexistent-command'\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "    post {\n"
                + "        failure {\n"
                + "            script {\n"
                + "                // This would call the AI API in a real scenario\n"
                + "                // For test, we'll just call the step without API key\n"
                + "                try {\n"
                + "                    explainError()\n"
                + "                } catch (Exception e) {\n"
                + "                    echo \"Expected failure due to missing API key: ${e.message}\"\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the job - it should fail but not crash
        WorkflowRun run = jenkins.assertBuildStatus(hudson.model.Result.FAILURE, job.scheduleBuild2(0));

        // Check that the explain error step was called
        jenkins.assertLogContains("ERROR: API key is not configured", run);
    }

    @Test
    void testGlobalConfiguration(JenkinsRule jenkins) throws Exception {
        // Test that global configuration can be accessed
        ExplainErrorPlugin.GlobalConfigurationImpl config =
                jenkins.getInstance().getDescriptorByType(ExplainErrorPlugin.GlobalConfigurationImpl.class);

        // Default values should be set
        assert config.getApiUrl().equals("https://api.openai.com/v1/chat/completions");
        assert config.getModel().equals("gpt-3.5-turbo");
        assert config.isEnableExplanation() == true;
    }
}
