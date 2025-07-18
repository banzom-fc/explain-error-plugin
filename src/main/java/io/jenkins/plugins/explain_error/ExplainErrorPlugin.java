package io.jenkins.plugins.explain_error;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

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
    }
}
