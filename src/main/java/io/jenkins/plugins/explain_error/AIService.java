package io.jenkins.plugins.explain_error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hudson.ProxyConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * Service class for communicating with AI APIs.
 * Uses Jenkins' ProxyConfiguration for proper proxy support.
 */
public class AIService {

    private static final Logger LOGGER = Logger.getLogger(AIService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GlobalConfigurationImpl config;

    public AIService(GlobalConfigurationImpl config) {
        this.config = config;
    }

    public String explainError(String errorLogs) throws IOException {
        
        if (StringUtils.isBlank(errorLogs)) {
            return "No error logs provided for explanation.";
        }

        String prompt = buildPrompt(errorLogs);
        String requestBody = buildRequestBody(prompt);
        
        try {
            URI apiUri = URI.create(config.getApiUrl());
            
            // Use Jenkins' ProxyConfiguration.newHttpRequestBuilder() to get a properly 
            // configured HttpRequest that respects Jenkins proxy settings
            HttpRequest.Builder requestBuilder = ProxyConfiguration.newHttpRequestBuilder(apiUri);
            
            // Build the HTTP request with proper proxy configuration
            HttpRequest request = requestBuilder
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            // Create HttpClient with timeout configuration
            // The proxy configuration is handled by the HttpRequest builder above
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

            // Execute the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            LOGGER.fine("Response body length: " + responseBody.length());
            LOGGER.fine("Response body preview: " + responseBody.substring(0, Math.min(500, responseBody.length())));

            if (response.statusCode() != 200) {
                LOGGER.severe("AI API request failed with status " + response.statusCode() + ": " + responseBody);
                return "Failed to get explanation from AI service. Status: " + response.statusCode() 
                    + ". Please check your API configuration and key.";
            }

            return parseResponse(responseBody);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("AI API request was interrupted: " + e.getMessage());
            return "Request was interrupted: " + e.getMessage();
        } catch (Exception e) {
            LOGGER.severe("AI API request failed: " + e.getMessage());
            return "Failed to communicate with AI service: " + e.getMessage();
        }
    }

    private String buildPrompt(String errorLogs) {
        return "You are an expert Jenkins administrator and software engineer. "
                + "Please analyze the following Jenkins build error logs and provide a clear, "
                + "actionable explanation of what went wrong and how to fix it:\n\n"
                + "ERROR LOGS:\n"
                + errorLogs + "\n\n" + "Please provide:\n"
                + "1. A summary of what caused the error\n"
                + "2. Specific steps to resolve the issue\n"
                + "3. Any relevant best practices to prevent similar issues\n\n"
                + "Keep your response concise and focused on actionable solutions.";
    }

    private String buildRequestBody(String prompt) throws IOException {
        ObjectNode requestJson = MAPPER.createObjectNode();
        requestJson.put("model", config.getModel());
        requestJson.put("max_tokens", 1000);
        requestJson.put("temperature", 0.3);

        ArrayNode messages = MAPPER.createArrayNode();
        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestJson.set("messages", messages);

        return MAPPER.writeValueAsString(requestJson);
    }

    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonNode jsonNode = MAPPER.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");

            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null && !content.isNull()) {
                        return content.asText().trim();
                    }
                }
            }

            // Check for error in response
            JsonNode error = jsonNode.get("error");
            if (error != null) {
                JsonNode errorMessage = error.get("message");
                if (errorMessage != null) {
                    return "AI API Error: " + errorMessage.asText();
                }
            }

            return "Unable to parse AI response. Response: " + responseBody;

        } catch (Exception e) {
            LOGGER.severe("Failed to parse AI response: " + e.getMessage());
            return "Failed to parse AI response: " + e.getMessage();
        }
    }
}
