package com.eprocure.notification.infrastructure.email;

import com.eprocure.notification.application.port.out.EmailDispatchException;
import com.eprocure.notification.application.port.out.EmailSendCommand;
import com.eprocure.notification.application.port.out.EmailSendResult;
import com.eprocure.notification.application.port.out.EmailSenderPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.notification.email.provider", havingValue = "brevo")
public class BrevoEmailSenderAdapter implements EmailSenderPort {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiUrl;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSenderAdapter(
            ObjectMapper objectMapper,
            @Value("${eprocure.notification.email.brevo.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
            @Value("${eprocure.notification.email.brevo.api-key:}") String apiKey,
            @Value("${eprocure.notification.email.from-address:}") String fromEmail,
            @Value("${eprocure.notification.email.from-name:eProcure}") String fromName) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.fromName = fromName == null || fromName.isBlank() ? "eProcure" : fromName.trim();
    }

    @Override
    public EmailSendResult send(EmailSendCommand command) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            throw new EmailDispatchException("Brevo email provider is not configured");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload(command)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmailDispatchException("Brevo email provider returned HTTP " + response.statusCode());
            }
            return new EmailSendResult(providerMessageId(response.body()));
        } catch (IOException exception) {
            throw new EmailDispatchException("Brevo email provider I/O failure", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmailDispatchException("Brevo email provider interrupted", exception);
        }
    }

    private String payload(EmailSendCommand command) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode sender = root.putObject("sender");
        sender.put("name", fromName);
        sender.put("email", fromEmail);
        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("email", command.to());
        root.putArray("to").add(recipient);
        root.put("subject", command.subject());
        root.put("textContent", command.body());
        return objectMapper.writeValueAsString(root);
    }

    private String providerMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messageId = root.get("messageId");
            return messageId == null || messageId.isNull() ? null : messageId.asText();
        } catch (IOException exception) {
            return null;
        }
    }
}
