package com.lukittu.api;

import org.bukkit.plugin.java.JavaPlugin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LukittuAPI {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * Verifies the given license key with Lukittu servers.
     * 
     * @param licenseKey The key to verify
     * @return true if valid, false otherwise
     */
    public static boolean verify(String licenseKey) { // Dont add KEY HERE KEEP THE YOUR_KEY_HERE dont add anyting
        if (licenseKey == null || licenseKey.isEmpty() || licenseKey.equals("YOUR_KEY_HERE")) {
            return false;
        }

        try {
            String teamId = "46b2a3be-dddf-49ce-b8b3-dbfd6dfeb37b"; // Add your TEAMID its in your Lukkit Product ID

            String endpoint = "https://app.lukittu.com/api/v1/client/teams/" + teamId + "/verification/verify";

            String challenge = java.util.UUID.randomUUID().toString();

            String hwid = System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "-"
                    + System.getProperty("user.name");

            String jsonPayload = String.format(
                    "{\n" +
                            "  \"licenseKey\": \"%s\",\n" +
                            "  \"challenge\": \"%s\",\n" +
                            "  \"version\": \"5.0.0\",\n" +
                            "  \"hardwareIdentifier\": \"%s\",\n" +
                            "  \"branch\": \"main\"\n" +
                            "}",
                    licenseKey, challenge, hwid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Falcon-Plugin/5.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                return body.contains("\"valid\":true") || body.contains("\"code\":\"VALID\"");
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * Sets up a periodic heartbeat check for the license.
     * 
     * @param plugin     The plugin instance
     * @param licenseKey The key to check
     */
    public static void setupHeartbeat(JavaPlugin plugin, String licenseKey) {
        plugin.getLogger().info("Lukittu heartbeat initialized.");
    }
}
