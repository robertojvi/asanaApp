package com.accessparks.asanaapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class JiraClient {

    // e.g. "yourcompany.atlassian.net" - no scheme, no trailing slash.
    @Value("${jira.site-url}")
    private String siteUrl;

    @Value("${jira.email}")
    private String email;

    @Value("${jira.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() {
        return "https://" + siteUrl + "/rest/api/3";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = email + ":" + apiToken;
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Uses UriComponentsBuilder (unlike AsanaClient's manual concatenation) because
    // Jira query params - JQL in particular - contain spaces and reserved characters
    // that must be percent-encoded.
    public JsonNode get(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl() + path);
        if (params != null) params.forEach(builder::queryParam);
        URI uri = builder.build().encode().toUri();
        return exchangeWithRetry(uri);
    }

    // Retries once on 429, honoring Retry-After, same pattern as AsanaClient.
    private JsonNode exchangeWithRetry(URI uri) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            return mapper.readTree(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            String retryAfter = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : "5";
            try {
                Thread.sleep(Long.parseLong(retryAfter == null ? "5" : retryAfter) * 1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return exchangeWithRetry(uri);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Jira API call failed: " + uri + " - " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Jira API call failed: " + uri, e);
        }
    }
}
