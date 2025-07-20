package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject json) throws Descriptor.FormException {
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
         * Method to test the AI API configuration.
         * This is called when the "Test Configuration" button is clicked.
         */
        @RequirePOST
        public FormValidation doTestConfiguration(@QueryParameter("apiKey") String apiKey,
                                                  @QueryParameter("apiUrl") String apiUrl,
                                                  @QueryParameter("model") String model) {
            // Permission check to restrict access
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            // Use provided parameters or fall back to saved configuration
            String testApiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : getApiKey();
            String testApiUrl = (apiUrl != null && !apiUrl.trim().isEmpty()) ? apiUrl : getApiUrl();
            String testModel = (model != null && !model.trim().isEmpty()) ? model : getModel();

            // Validate required fields
            if (testApiKey == null || testApiKey.trim().isEmpty()) {
                return FormValidation.error("API Key is required. Please enter your OpenAI API key.");
            }

            if (testApiUrl == null || testApiUrl.trim().isEmpty()) {
                return FormValidation.error("API URL is required.");
            }

            if (testModel == null || testModel.trim().isEmpty()) {
                return FormValidation.error("Model is required.");
            }

            try {
                // Create a temporary configuration for testing
                GlobalConfigurationImpl tempConfig = new GlobalConfigurationImpl();
                tempConfig.setApiKey(testApiKey);
                tempConfig.setApiUrl(testApiUrl);
                tempConfig.setModel(testModel);

                // Create AIService instance with test configuration
                AIService aiService = new AIService(tempConfig);
                
                // Make a simple test call
                String testResponse = aiService.explainError("Test configuration call - please respond with 'Configuration test successful'");
                
                if (testResponse != null && testResponse.contains("Configuration test successful")) {
                    return FormValidation.ok("Configuration test successful! API connection is working properly.");
                } else if (testResponse != null && testResponse.contains("AI API Error:")) {
                    return FormValidation.error("" + testResponse);
                } else if (testResponse != null && testResponse.contains("Failed to get explanation from AI service")) {
                    return FormValidation.error("" + testResponse);
                } else {
                    return FormValidation.ok("API connection established, but got unexpected response: " + testResponse);
                }
                
            } catch (IOException e) {
                Logger.getLogger(GlobalConfigurationImpl.class.getName()).log(Level.WARNING, "API test failed", e);
                return FormValidation.error("Connection failed: " + e.getMessage() + ". Please check your API URL and network connection.");
            } catch (Exception e) {
                Logger.getLogger(GlobalConfigurationImpl.class.getName()).log(Level.WARNING, "Configuration test failed", e);
                return FormValidation.error("Test failed: " + e.getMessage());
            }
        }
    }
}
