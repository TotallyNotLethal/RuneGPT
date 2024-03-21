import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordUtils {

    public static void sendDiscordWebhookMessage(String webhookUrl, String message) {
        HttpClient client = HttpClient.newHttpClient();

        // Create a JSON object and add the message content
        JSONObject json = new JSONObject();
        json.put("content", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendDiscordWebhookFile(String webhookUrl, String fileContent) throws IOException, InterruptedException {
        String boundary = UUID.randomUUID().toString();
        String bodyData = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"files[0]\"; filename=" + extractScriptName(fileContent) + ".txt\r\n" +
                "Content-Type: text/plain\r\n\r\n" +
                fileContent + "\r\n" +
                "--" + boundary + "--";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(bodyData))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String extractScriptName(String scriptContent) {
        // Pattern to match the @ScriptManifest annotation and extract the name attribute
        Pattern pattern = Pattern.compile("@ScriptManifest\\([^)]*name\\s*=\\s*\"([^\"]*)\"[^)]*\\)");
        Matcher matcher = pattern.matcher(scriptContent);

        if (matcher.find()) {
            // Return the value of the name attribute
            return matcher.group(1);
        }
        return "Name not found";
    }
}
