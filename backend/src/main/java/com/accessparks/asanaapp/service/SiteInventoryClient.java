package com.accessparks.asanaapp.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SiteInventoryClient {

    // e.g. "http://192.168.102.32/views" - internal-network only, no auth.
    @Value("${sitelist.base-url}")
    private String baseUrl;

    private static final int TIMEOUT_MS = 30_000;

    public Document fetchSiteList() {
        return get(baseUrl + "/sitelist.php", null, null);
    }

    public Document fetchSiteInfo(Long subvenueId, String subvenueName) {
        return get(baseUrl + "/siteinfo.php", subvenueId, subvenueName);
    }

    public Document fetchSubvenueDetail(Long subvenueId, String subvenueName) {
        return get(baseUrl + "/subvenue.php", subvenueId, subvenueName);
    }

    private Document get(String url, Long subvenueId, String subvenueName) {
        try {
            var conn = Jsoup.connect(url).timeout(TIMEOUT_MS).ignoreContentType(false);
            if (subvenueId != null) {
                conn.data("subvenue_id", String.valueOf(subvenueId))
                    .data("subvenue", subvenueName == null ? "" : subvenueName)
                    .data("venue", "")
                    .data("venue_id", "");
            }
            return conn.get();
        } catch (Exception e) {
            throw new RuntimeException("Site inventory request failed: " + url + " - " + e.getMessage(), e);
        }
    }
}
