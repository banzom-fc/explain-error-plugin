package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.jenkinsci.Symbol;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Global configuration for the plugin.
 */
@Extension
@Symbol("explainError")
public class GlobalConfigurationImpl extends GlobalConfiguration {

    private Secret apiKey;
    private String apiUrl = "https://api.openai.com/v1/chat/completions";
    private String model = "gpt-3.5-turbo";
    private boolean enableExplanation = true;

    public GlobalConfigurationImpl() {
        load();
    }

    /**
     * Get the singleton instance of GlobalConfigurationImpl.
     * @return the GlobalConfigurationImpl instance
     */
    public static GlobalConfigurationImpl get() {
        return Jenkins.get().getDescriptorByType(GlobalConfigurationImpl.class);
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    // Getters and setters
    public Secret getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
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
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        // Validate only the provided parameters
        Secret testApiKeySecret = (apiKey != null) ? Secret.fromString(apiKey) : null;
        String testApiUrl = apiUrl != null ? apiUrl : "";
        String testModel = model != null ? model : "";

        try {
            GlobalConfigurationImpl tempConfig = new GlobalConfigurationImpl();
            tempConfig.setApiKey(testApiKeySecret);
            tempConfig.setApiUrl(testApiUrl);
            tempConfig.setModel(testModel);

            AIService aiService = new AIService(tempConfig);
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
