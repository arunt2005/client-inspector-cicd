package com.apps.controller;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    // Initialize the UA Analyzer once (heavy object)
    private final UserAgentAnalyzer uaAnalyzer = UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(1000)
            .build();

    @GetMapping("/inspect")
    public String getClientInfo(HttpServletRequest request, Model model) {
        Map<String, String> data = new LinkedHashMap<>();
        String ip = request.getRemoteAddr();

        // Handle localhost for testing
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
            ip = "8.8.8.8"; // Default to Google IP for GeoIP testing
        }

        try {
            // 1. Geolocation Logic
            DatabaseReader reader = new DatabaseReader.Builder(
                    new ClassPathResource("GeoLite2-City.mmdb").getInputStream()).build();
            CityResponse geoResponse = reader.city(InetAddress.getByName(ip));

            data.put("IP Address", ip);
            data.put("City", geoResponse.getCity().getName());
            data.put("Country", geoResponse.getCountry().getName());
            data.put("Latitude", geoResponse.getLocation().getLatitude().toString());
            data.put("Longitude", geoResponse.getLocation().getLongitude().toString());

            // 2. User-Agent Parsing
            UserAgent agent = uaAnalyzer.parse(request.getHeader("User-Agent"));
            data.put("Browser", agent.getValue("AgentName"));
            data.put("OS", agent.getValue("OperatingSystemName"));
            data.put("Device", agent.getValue("DeviceClass"));

            // 3. Audit Logging
            logger.info("Access logged: IP={} | OS={} | Browser={}", ip, data.get("OS"), data.get("Browser"));

        } catch (Exception e) {
            logger.error("Error retrieving client info: {}", e.getMessage());
            data.put("Error", "Could not retrieve full details");
        }

        model.addAttribute("clientData", data);
        return "client-info";
    }

}