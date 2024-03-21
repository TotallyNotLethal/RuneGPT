import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

import org.dreambot.api.utilities.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class OpenAI {

    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String api) {
        apiKey = api;
    }

    private String apiKey;
    public String bodyGPT = "gpt-3.5-turbo-0125";
    private final List<String> messageHistory = new ArrayList<>();

    public OpenAI(String apiKey) {
        this.apiKey = apiKey;
        addMessage("system", "You are a DreamBot programming expert, you will program anything the user desires using the DreamBot API, referencing class calls DIRECTLY (!!!Example: getLocalPlayer -> Players.getLocal(), getInventory -> Inventory, sleep/sleepUntil -> Sleep.sleep/Sleep.sleepUntil, getWalking -> Walking, getGameObjects -> GameObjects) <- ENSURE ALL CLASSES ARE DIRECTLY CALLED. The Script MUST HAVE ALL IMPORTS AS THERE IS NO EXTENDED SCRIPTS OR CLASSES [IMPORTANT] ALL CLASSES NEED DIRECT CALLS AND IMPORTS NO EXCEPTIONS [/IMPORTANT]!!!!! import org.dreambot.api.script.*; MUST BE IMPORTED FOR THE MAIN CLASS NO EXCEPTIONS!! You will reply with NOTHING other than the code that is requested NOTHING ELSE EVER NO MATTER WHAT. The code is ALWAYS in JAVA, and has proper tab spacing, new lines etc. The MAIN Class is ALWAYS named: REPLClass [NO EXCEPTIONS!!!] Do NOT use deprecated API's.");
    }


    public String sendMessage(String message, String gptSet) throws IOException, InterruptedException {
        // URL for the OpenAI API
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        addMessage("user", message);
        bodyGPT = gptSet;
        Logger.log("Using GPT version: " + bodyGPT);

        String jsonRequestBody = constructRequestBody();

        // Prepare the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        // Send the request
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        // Return the response body
        return response.body();
    }

    public void addMessage(String role, String content) {
        // Create a JSON object for the message and add it to the history in a structured way
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content.replace("\"", "\\\"")); // Basic manual escaping, better handled by JSON library
        // Convert the message object to a string and add to history
        messageHistory.add(message.toString());
    }

    // Construct JSON request body with history
    private String constructRequestBody() {
        JSONArray messages = new JSONArray();
        for (String historyItem : messageHistory) {
            // Assuming historyItem is a JSON string; otherwise, create a JSONObject and add it
            messages.put(new JSONObject(historyItem));
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", bodyGPT);
        requestBody.put("max_tokens", 4096);
        requestBody.put("temperature", 0.7);
        requestBody.put("messages", messages);

        return requestBody.toString(); // This will be a correctly formatted and escaped JSON string
    }
}