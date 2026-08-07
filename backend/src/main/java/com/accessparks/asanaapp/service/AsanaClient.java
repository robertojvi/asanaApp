package com.accessparks.asanaapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class AsanaClient {

    private static final String BASE_URL = "https://app.asana.com/api/1.0";

    @Value("${asana.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public JsonNode get(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(BASE_URL + path);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((k, v) -> url.append(k).append("=").append(v).append("&"));
        }
        return exchangeWithRetry(url.toString(), HttpMethod.GET, null);
    }

    public JsonNode put(String path, Map<String, Object> data) {
        Map<String, Object> body = Map.of("data", data);
        return exchangeWithRetry(BASE_URL + path, HttpMethod.PUT, body);
    }

    public JsonNode post(String path, Map<String, Object> data) {
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        return exchangeWithRetry(BASE_URL + path, HttpMethod.POST, body);
    }

    // Retries once on 429, honoring Retry-After, same pattern as the Node client.
    private JsonNode exchangeWithRetry(String url, HttpMethod method, Object body) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            return mapper.readTree(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            String retryAfter = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : "5";
            try {
                Thread.sleep(Long.parseLong(retryAfter == null ? "5" : retryAfter) * 1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return exchangeWithRetry(url, method, body);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Asana API call failed: " + url + " - " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Asana API call failed: " + url, e);
        }
    }

    /** Duplicates a project (e.g. a template) into a new one. Returns the job node. */
    public JsonNode duplicateProject(String templateProjectGid, String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("include", new String[]{
            "forms", "members", "task_notes", "task_assignee",
            "task_subtasks", "task_attachments", "task_dates", "task_dependencies",
            "task_followers", "task_tags", "task_projects"
        });
        return post("/projects/" + templateProjectGid + "/duplicate", data).get("data");
    }

    /** Polls an Asana job until it succeeds, returning the new project's gid. */
    public String pollJobForNewProjectGid(String jobGid, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            JsonNode job = get("/jobs/" + jobGid, Map.of("opt_fields", "status,new_project.gid")).get("data");
            String status = job.get("status").asText();
            if ("succeeded".equals(status)) {
                return job.get("new_project").get("gid").asText();
            }
            if ("failed".equals(status)) {
                throw new RuntimeException("Asana duplication job failed");
            }
            Thread.sleep(2000);
        }
        throw new RuntimeException("Timed out waiting for Asana duplication job");
    }
}
