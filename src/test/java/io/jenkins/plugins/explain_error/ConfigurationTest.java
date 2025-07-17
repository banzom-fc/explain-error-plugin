package io.jenkins.plugins.explain_error;

import static org.junit.Assert.*;

import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test the configuration validation functionality.
 */
public class ConfigurationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigurationValidation() throws Exception {
        ExplainErrorPlugin.GlobalConfigurationImpl config = new ExplainErrorPlugin.GlobalConfigurationImpl();

        // Test with empty API key
        FormValidation result =
                config.doTestConfiguration("", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue("Should contain API Key error message", result.toString().contains("API Key is required"));

        // Test with empty API URL
        result = config.doTestConfiguration("test-key", "", "gpt-3.5-turbo");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue("Should contain API URL error message", result.toString().contains("API URL is required"));

        // Test with empty model
        result = config.doTestConfiguration("test-key", "https://api.openai.com/v1/chat/completions", "");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue("Should contain Model error message", result.toString().contains("Model is required"));
    }

    @Test
    public void testConfigurationWithInvalidKey() throws Exception {
        ExplainErrorPlugin.GlobalConfigurationImpl config = new ExplainErrorPlugin.GlobalConfigurationImpl();

        // Test with invalid API key (will fail but should handle gracefully)
        FormValidation result = config.doTestConfiguration(
                "invalid-key", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");

        // Should return an error but not crash
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(
                "Should contain configuration test failed message",
                result.toString().contains("Configuration test failed"));
    }
}
