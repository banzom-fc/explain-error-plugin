package io.jenkins.plugins.explain_error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * Service class for communicating with AI APIs.
 */
public class AIService {

    private static final Logger LOGGER = Logger.getLogger(AIService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final int SOCKET_TIMEOUT = 60000; // 60 seconds

    private final ExplainErrorPlugin.GlobalConfigurationImpl config;

    public AIService(ExplainErrorPlugin.GlobalConfigurationImpl config) {
        this.config = config;
    }

    public String explainError(String errorLogs) throws IOException {
        
        if (StringUtils.isBlank(errorLogs)) {
            return "No error logs provided for explanation.";
        }

        String prompt = buildPrompt(errorLogs);
        
        String requestBody = buildRequestBody(prompt);
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpPost post = new HttpPost(config.getApiUrl());

        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + config.getApiKey());
        post.setEntity(new StringEntity(requestBody, "UTF-8"));

        HttpResponse response = client.execute(post);
        String responseBody = EntityUtils.toString(response.getEntity());

        LOGGER.fine("Response body length: " + responseBody.length());
        LOGGER.fine("Response body preview: " + responseBody.substring(0, Math.min(500, responseBody.length())));

        if (response.getStatusLine().getStatusCode() != 200) {
            LOGGER.severe("AI API request failed with status "
                    + response.getStatusLine().getStatusCode() + ": " + responseBody);
            return "Failed to get explanation from AI service. Status: "
                    + response.getStatusLine().getStatusCode() + ". Please check your API configuration and key.";
        }

        String explanation = parseResponse(responseBody);
        
        return explanation;
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
