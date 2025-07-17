package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

/**
 * Main plugin class for the Explain Error Plugin.
 */
public class ExplainErrorPlugin extends Plugin {

    @Override
    public void start() throws Exception {
        super.start();
        // Plugin initialization logic
    }

    /**
     * Global configuration for the plugin.
     */
    @Extension
    public static class GlobalConfigurationImpl extends GlobalConfiguration {

        private Secret apiKey;
        private String apiUrl = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-3.5-turbo";
        private boolean enableExplanation = true;

        public GlobalConfigurationImpl() {
            load();
        }

        @DataBoundConstructor
        public GlobalConfigurationImpl(String apiKey, String apiUrl, String model, boolean enableExplanation) {
            this.apiKey = Secret.fromString(apiKey);
            this.apiUrl = apiUrl;
            this.model = model;
            this.enableExplanation = enableExplanation;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            req.bindJSON(this, json);
            save();
            return true;
        }

        // Getters and setters
        public String getApiKey() {
            return Secret.toString(apiKey);
        }

        @DataBoundSetter
        public void setApiKey(String apiKey) {
            this.apiKey = Secret.fromString(apiKey);
        }

        public String getApiUrl() {
            return apiUrl;
        }

        @DataBoundSetter
        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        @DataBoundSetter
        public void setModel(String model) {
            this.model = model;
        }

        public boolean isEnableExplanation() {
            return enableExplanation;
        }

        @DataBoundSetter
        public void setEnableExplanation(boolean enableExplanation) {
            this.enableExplanation = enableExplanation;
        }

        @Override
        public String getDisplayName() {
            return "Explain Error Plugin Configuration";
        }

        /**
         * Test the current configuration by making a simple API call.
         */
        @POST
        public FormValidation doTestConfiguration(
                @QueryParameter("apiKey") String apiKey,
                @QueryParameter("apiUrl") String apiUrl,
                @QueryParameter("model") String model)
                throws IOException, ServletException {

            // Use provided values or fall back to saved values
            String testApiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : Secret.toString(this.apiKey);
            String testApiUrl = (apiUrl != null && !apiUrl.trim().isEmpty()) ? apiUrl : this.apiUrl;
            String testModel = (model != null && !model.trim().isEmpty()) ? model : this.model;

            if (testApiKey == null || testApiKey.trim().isEmpty()) {
                return FormValidation.error("API Key is required for testing");
            }

            if (testApiUrl == null || testApiUrl.trim().isEmpty()) {
                return FormValidation.error("API URL is required for testing");
            }

            if (testModel == null || testModel.trim().isEmpty()) {
                return FormValidation.error("Model is required for testing");
            }

            try {
                // Create a temporary configuration for testing
                GlobalConfigurationImpl testConfig = new GlobalConfigurationImpl();
                testConfig.setApiKey(testApiKey);
                testConfig.setApiUrl(testApiUrl);
                testConfig.setModel(testModel);

                // Test with a simple error message
                AIService aiService = new AIService(testConfig);
                String testErrorLog =
                        "BUILD FAILED\nError: Command failed with exit code 1\nTest error for configuration validation";

                String result = aiService.explainError(testErrorLog);

                // Check if the result indicates an error
                if (result.startsWith("Failed to get explanation from AI service")
                        || result.startsWith("AI API Error:")
                        || result.startsWith("Authentication failed")
                        || result.startsWith("Rate limit exceeded")
                        || result.startsWith("OpenAI API quota exceeded")) {
                    return FormValidation.error("Configuration test failed: " + result);
                }

                // Success case
                return FormValidation.ok("✅ Configuration test successful! AI service is working correctly.");

            } catch (Exception e) {
                return FormValidation.error("Configuration test failed with exception: " + e.getMessage());
            }
        }
    }
}
