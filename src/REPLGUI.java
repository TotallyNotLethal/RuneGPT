import org.dreambot.api.Client;
import org.dreambot.api.utilities.Logger;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.dreambot.api.script.AbstractScript;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class REPLGUI extends JFrame {
    String pluginVersion = "1.1";
    DefaultListModel<String> scriptListModel;
    public RSyntaxTextArea codeInputArea;
    private JTextField clientJarPathField;
    private JTextField userInputField;
    private JTextField apiKeyTextField;
    private JCheckBox uploadTrainCheckbox;
    private JCheckBox chatGPT35Checkbox;
    OpenAI chatbot;
    static Runnable scriptRunner;
    public volatile boolean scriptRunning = false;
    private Thread scriptThread;
    private JTabbedPane tabbedPane;
    private DefaultListModel<String> chatLogModel;
    private JList<String> chatLogList;
    private JLabel statusLabel;
    public boolean uploadChecked = true;

    String[][] importChecks = {
            {"import org.dreambot.api.utilities.Sleep;", "sleepUntil", "Sleep.sleepUntil"},
            {"import org.dreambot.api.methods.interactive.Players;", "getLocalPlayer", "Players.getLocal"},
            {"import org.dreambot.api.methods.container.impl.Inventory;", "getInventory()", "Inventory"},
            {"import org.dreambot.api.methods.walking.impl.Walking;", "getWalking()", "Walking"},
            {"import org.dreambot.api.methods.interactive.GameObjects;", "getGameObjects()", "GameObjects"},
            //{"import org.dreambot.api.methods.map.Tile;", "getTile()", "Tile."},
            {"import org.dreambot.api.methods.container.impl.bank.Bank;", "getBank()", "Bank"},
            {"import org.dreambot.api.methods.container.impl.equipment.Equipment;", "getEquipment()", "Equipment"},
            {"import org.dreambot.api.methods.map.Area;", "getArea()", "Area"},
            {"import org.dreambot.api.methods.interactive.NPCs;", "getNPCS()", "NPCS"},
            {"import org.dreambot.api.methods.item.GroundItems;", "getGroundItems()", "GroundItems"}
    };


    public REPLGUI() {
        initializeUI();
        apiKeyTextField.setText(loadApiKeyFromFile());
        chatbot = new OpenAI(apiKeyTextField.getText());
    }

    public boolean CheckVersion() {
        String readmeUrl = "https://raw.githubusercontent.com/TotallyNotLethal/RuneGPT/main/Version";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readmeUrl))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::parseVersion)
                .join();
        return this.getTitle().contains("UPDATE");
    }

    private void parseVersion(String readmeContent) {
        Scanner scanner = new Scanner(readmeContent);

        if(!scanner.nextLine().equals(pluginVersion)) {
            JOptionPane.showMessageDialog(tabbedPane, "Plugin needs updated!", "Update Needed", JOptionPane.WARNING_MESSAGE);
            setTitle("REPL GUI | UPDATE AVAILABLE (v" + readmeContent + ")");
        }
    }

    private void initializeUI() {
        setTitle("REPL GUI v" + pluginVersion);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize the tabbed pane
        tabbedPane = new JTabbedPane();

        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Code compilation tab
        JPanel codeCompilationPanel = new JPanel(new BorderLayout());
        initializeCodeCompilationUI(codeCompilationPanel);
        tabbedPane.addTab("Code Compilation", codeCompilationPanel);

        // Chat log tab
        chatLogModel = new DefaultListModel<>();
        chatLogList = new JList<>(chatLogModel);
        JScrollPane chatLogScrollPane = new JScrollPane(chatLogList);
        chatLogList.setLayoutOrientation(JList.VERTICAL);
        tabbedPane.addTab("Chat Log", chatLogScrollPane);

        chatLogList.setCellRenderer(new ExpandableListCellRenderer());
        chatLogList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = chatLogList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    ExpandableListCellRenderer renderer = (ExpandableListCellRenderer) chatLogList.getCellRenderer();
                    renderer.toggleExpandedState(index);
                    chatLogList.revalidate();
                    chatLogList.repaint();
                }
            }
        });
        chatLogList.setVisibleRowCount(5); // Adjust as needed for initial view size
        chatLogList.setFixedCellHeight(50); // Set a fixed height for collapsed state, adjust as needed

        JPanel optionsPanel = new JPanel();
        initializeOptionsUI(optionsPanel);
        tabbedPane.addTab("Options", optionsPanel);

        // Public Scripts tab
        JPanel publicScriptsPanel = new JPanel(new BorderLayout());
        initializePublicScriptsUI(publicScriptsPanel);
        tabbedPane.addTab("Public Scripts", publicScriptsPanel);

        // Add the tabbed pane to the frame
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializePublicScriptsUI(JPanel panel) {
        scriptListModel = new DefaultListModel<>();
        JList<String> scriptList = new JList<>(scriptListModel);
        JScrollPane scriptScrollPane = new JScrollPane(scriptList);

        fetchAndDisplayScripts();

        // Button to load the selected script
        JButton loadScriptButton = new JButton("Load Script");
        loadScriptButton.addActionListener(e -> {
            String selectedScript = scriptList.getSelectedValue();
            if (selectedScript != null) {
                // Load the selected script into the code editor
                // This is a placeholder - replace with actual loading logic
                loadScriptContent(selectedScript);
            } else {
                JOptionPane.showMessageDialog(panel, "Please select a script to load.", "No Script Selected", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Layout components
        panel.add(scriptScrollPane, BorderLayout.CENTER);
        panel.add(loadScriptButton, BorderLayout.SOUTH);
    }

    private void fetchAndDisplayScripts() {
        String readmeUrl = "https://raw.githubusercontent.com/TotallyNotLethal/RuneGPT/main/README.md";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readmeUrl))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::parseAndDisplayScriptNames)
                .join();
    }

    private void parseAndDisplayScriptNames(String readmeContent) {
        Scanner scanner = new Scanner(readmeContent);
        Logger.log(readmeContent);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // Add line to script list model here
            scriptListModel.addElement(line); // Assuming scriptListModel is accessible
        }
    }

    private void loadScriptContent(String scriptName) {
        codeInputArea.setText("");
        String baseUrl = "https://raw.githubusercontent.com/TotallyNotLethal/RuneGPT/main/";
        String formattedName = URLEncoder.encode(scriptName, StandardCharsets.UTF_8).replace("+", "%20");
        String scriptUrl = baseUrl + "RuneGPT%20" + formattedName + ".txt";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scriptUrl))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(scriptContent -> {
                    // Assuming codeInputArea is accessible
                    SwingUtilities.invokeLater(() -> codeInputArea.setText(scriptContent));
                })
                .join(); // This is blocking, consider managing threading appropriately
    }

    private void initializeOptionsUI(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Font Size Option
        JPanel fontSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel fontSizeLabel = new JLabel("Font Size:");
        JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 24, 1));
        fontSizeSpinner.setValue(codeInputArea.getFont().getSize()); // Initialize with current font size
        fontSizeSpinner.addChangeListener(e -> {
            int newSize = (Integer) fontSizeSpinner.getValue();
            Font currentFont = codeInputArea.getFont();
            Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), newSize);
            codeInputArea.setFont(newFont);
        });
        fontSizePanel.add(fontSizeLabel);
        fontSizePanel.add(fontSizeSpinner);
        panel.add(fontSizePanel);

        // OpenAI API-Key Option
        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel apiKeyLabel = new JLabel("OpenAI API-Key:");
        apiKeyTextField = new JTextField(20);
        apiKeyTextField.setToolTipText("Enter your OpenAI API Key here");

        JButton getApiKeyButton = new JButton("Get API-Key");
        getApiKeyButton.addActionListener(e -> {
            try {
                // Open the URL in the user's default web browser
                Desktop.getDesktop().browse(new URI("https://platform.openai.com/api-keys"));
            } catch (Exception ex) {
                // Handle any exceptions (e.g., no desktop supported, URI syntax error)
                ex.printStackTrace();
                JOptionPane.showMessageDialog(apiKeyPanel, "Failed to open the URL in the browser.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        apiKeyPanel.add(apiKeyLabel);
        apiKeyPanel.add(apiKeyTextField);
        apiKeyPanel.add(getApiKeyButton);
        panel.add(apiKeyPanel);

        // Upload to Train Option
        JPanel uploadTrainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel uploadTrainLabel = new JLabel("Upload to train?");
        uploadTrainCheckbox = new JCheckBox();
        uploadTrainCheckbox.setToolTipText("Check to enable uploading data for training");
        uploadTrainCheckbox.setSelected(true);
        JLabel chatGPT35 = new JLabel("Use ChatGPT 3.5");
        chatGPT35Checkbox = new JCheckBox();
        //uploadTrainCheckbox.setToolTipText("Currently disabled...");
        //chatGPT35Checkbox.setEnabled(false);
        uploadTrainPanel.add(uploadTrainLabel);
        uploadTrainPanel.add(uploadTrainCheckbox);
        uploadTrainPanel.add(chatGPT35);
        uploadTrainPanel.add(chatGPT35Checkbox);
        panel.add(uploadTrainPanel);

        // You can add more options here similar to the above configurations...
    }

    private void saveApiKeyToFile(String apiKey) {
        // Define the file path; saves in the user's home directory
        String userName = System.getProperty("user.name");
        String directoryPath = "C:\\Users\\" + userName + "\\DreamBot\\Scripts\\RuneGPT";
        String filePath = directoryPath + File.separator + "apiKey.txt";

        // Check if the directory exists, if not, create it
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean wasSuccessful = directory.mkdirs();
            if (!wasSuccessful) {
                // If the directory was not successfully created, log an error and return
                Logger.log("Failed to create the directory for saving the API Key");
                return;
            }
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write the API key to the file
            writer.write(apiKey);
            writer.flush();

            // Optionally, show a confirmation dialog
            Logger.log("API Key saved successfully");
        } catch (IOException e) {
            // Handle possible I/O errors
            e.printStackTrace();
            Logger.log("Failed to save the API Key");

        }
    }

    private String loadApiKeyFromFile() {
        // Define the file path; the file is located in the user's home directory
        String userName = System.getProperty("user.name");
        String filePath = "C:\\Users\\" + userName +  "\\DreamBot\\Scripts\\RuneGPT" + File.separator + "apiKey.txt";
        File file = new File(filePath);

        // Check if the file exists
        if (!file.exists()) {
            // Handle the case where the file does not exist
            // For example, return null or an empty string, or display an error message
            return null;
        }

        // Use try-with-resources to ensure the reader is closed after use
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Read the API key from the file (assuming the API key is on the first line of the file)
            String apiKey = reader.readLine();

            // Return the read API key
            return apiKey;
        } catch (IOException e) {
            // Handle possible I/O errors
            e.printStackTrace();
            // Optionally, show an error dialog or log the error
            // Return null or an empty string based on how you want to handle the error
            return null;
        }
    }


    private void initializeCodeCompilationUI(JPanel panel) {
        // Create a text field for the client JAR path
        String userName = System.getProperty("user.name");
        String filePath = "C:\\Users\\" + userName;
                clientJarPathField = new JTextField(filePath + "\\DreamBot\\BotData\\client.jar");
        clientJarPathField.setToolTipText("If custom installation enter the path to the client JAR");

        // Initialize user input field
        userInputField = new JTextField();
        userInputField.setToolTipText("Type your command here and press Enter");

        // Add ActionListener to the user input field
        userInputField.addActionListener(e -> {
            String userInput = userInputField.getText();
            handleUserInput(userInput); // Call a method to handle the user input
            userInputField.setText(""); // Clear the input field after handling the input
        });

        // Create a panel to hold the input field and possibly other controls
        JPanel userInputPanel = new JPanel(new BorderLayout());

        // Add the text field to the frame
        JPanel topPanel = new JPanel(new BorderLayout());
        //topPanel.add(new JLabel("Client JAR Path:"), BorderLayout.WEST);
        //topPanel.add(clientJarPathField, BorderLayout.CENTER);
        topPanel.add(new JLabel("Command:"), BorderLayout.WEST);
        topPanel.add(userInputField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);



        // Initialize RSyntaxTextArea for syntax highlighting
        codeInputArea = new RSyntaxTextArea();
        codeInputArea.setEditable(true);
        LanguageSupportFactory.get().register(codeInputArea);
        codeInputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeInputArea.setCodeFoldingEnabled(true);
        codeInputArea.setTabSize(4);
        codeInputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        codeInputArea.setMargin(new Insets(5, 5, 5, 5));
        codeInputArea.setWrapStyleWord(true);
        codeInputArea.setLineWrap(true);
        codeInputArea.setEnabled(true);

        String dreamBotUser = Client.getForumUser().getUsername();

        String initialCode = """
                import org.dreambot.api.script.*;
                import org.dreambot.api.methods.Calculations;
                import org.dreambot.api.utilities.Logger;
                import org.dreambot.api.utilities.Sleep;
                import org.dreambot.api.script.Category;
                import java.awt.*;
                
                @ScriptManifest(name = "Script name here", description = "Script description here", author = """ + "\"" + dreamBotUser + "\"" +

                """
                , version = 1.0, category = Category.MISC, image = "")
                public class REPLClass extends AbstractScript {
                \t
                \t@Override
                \tpublic void onStart() {
                \t\tLogger.log("Script started!");
                \t}
                \t
                \t@Override
                \tpublic int onLoop() {
                \t\t
                \t\treturn Calculations.random(200, 600);
                \t}
                \t
                \t@Override
                \tpublic void onPaint(Graphics graphics) {
                \t\t
                \t}
                \t
                \t@Override
                \tpublic void onExit() {
                \t}
                }""";

        codeInputArea.setText(initialCode);

        codeInputArea.requestFocusInWindow();

        // Wrap the RSyntaxTextArea in an RTextScrollPane for scroll functionality
        RTextScrollPane sp = new RTextScrollPane(codeInputArea);
        add(sp, BorderLayout.CENTER);

        JButton executeButton = new JButton("Execute");
        executeButton.addActionListener(e -> onExecutePressed());

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> stopScript());

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(executeButton);
        buttonPanel.add(stopButton);

        // Add the button panel to the bottom of the frame
        add(buttonPanel, BorderLayout.SOUTH);

        // Document filter for auto-indentation
        ((AbstractDocument) codeInputArea.getDocument()).setDocumentFilter(new AutoIndentDocumentFilter());
        java.util.logging.Logger.getLogger("org.fife.rsta.ac.LanguageSupportFactory").setLevel(Level.OFF);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void appendToChatLog(String message) {
        SwingUtilities.invokeLater(() -> {
            chatLogModel.addElement(message); // Add the message as a new element
            chatLogList.ensureIndexIsVisible(chatLogModel.getSize() - 1); // Auto-scroll to the new element
        });
    }

    private void onExecutePressed() {
        ScriptManager sc = new ScriptManager();
        if (scriptThread == null || !scriptThread.isAlive()) {
            scriptRunning = true; // Set the flag to true when starting the script
            scriptThread = new Thread(() -> {
                String clientJarUrl = clientJarPathField.getText();
                String code = codeInputArea.getText();
                boolean isCompiled = sc.compileAndLoadJavaCode(code, clientJarUrl);
                if (isCompiled) {
                    Logger.log("Class compiled and ran successfully.");
                    try {
                        if(uploadTrainCheckbox.isSelected())
                            DiscordUtils.sendDiscordWebhookFile("https://discord.com/api/webhooks/1205710476520980542/_pr5vkTDZJ0qZUULcBP_uKdfyeXWq5YHaWi5VjZyqypE8MDQLqgIVr99qe43R5ENkrhU", code);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    scriptThread = new Thread(scriptRunner); // Use the outer scriptThread variable
                    scriptThread.start();
                } else {
                    Logger.log("Compilation Error.");
                }
            });
            scriptThread.start();
        } else {
            Logger.log("Script is already running.");
        }
    }

    private void stopScript() {
        if (scriptRunning){// && scriptThread != null && scriptThread.isAlive()) {
            scriptRunning = false; // Set the flag to false to stop the script
            scriptThread.interrupt(); // Interrupt the thread if you are using blocking operations in onLoop
            Logger.log("Script stopped by user.");
            try {
                scriptThread.join(); // Optional: wait for the thread to finish execution
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Set the interrupted status again
            }
        }
    }

    private void handleUserInput(String input) {
        if(apiKeyTextField.getText().trim().isEmpty()) {
            // Show a popup dialog with a warning message
            JOptionPane.showMessageDialog(this, "The OpenAI API-Key cannot be empty unless GPT 3.5 is checked!", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        chatbot.setApiKey(apiKeyTextField.getText().trim());
        saveApiKeyToFile(apiKeyTextField.getText().trim());
        new Thread(() -> {
            Logger.log("Waiting for ChatGPT response...");
            try {
                String response = chatbot.sendMessage(String.format("%s \n\n Current code: %s", input, codeInputArea.getText() ), chatGPT35Checkbox.isSelected() ? "gpt-3.5-turbo-0125" : "gpt-4-turbo-preview");

                SwingUtilities.invokeLater(() -> {
                    // For debugging, clear the text area first (not normally required).
                    codeInputArea.setText(""); // Clear existing content explicitly.
                });

                Logger.log("ChatGPT responded!");
                Logger.log("Fixing imports...");
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("choices")) {
                    JSONArray choicesArray = jsonResponse.getJSONArray("choices");
                    if (!choicesArray.isEmpty()) {
                        JSONObject firstChoice = choicesArray.getJSONObject(0);
                        if (firstChoice.has("message")) {
                            String content = firstChoice.getJSONObject("message").getString("content");
                            try {
                                String finalContent = CheckContentErrors(content, importChecks);

                                SwingUtilities.invokeLater(() -> {
                                    // For debugging, clear the text area first (not normally required).
                                    codeInputArea.setText(""); // Clear existing content explicitly.
                                    codeInputArea.setText(finalContent); // Set new content.
                                });
                            } catch(Exception e) { Logger.log(e.toString()); }
                        }
                    }
                } else {
                    Logger.log("No valid response found.");
                        // Show a popup dialog with a warning message
                    JOptionPane.showMessageDialog(this, "The OpenAI API-Key supplied is invalid!", "Input Error", JOptionPane.WARNING_MESSAGE);
                    String dreamBotUser = Client.getForumUser().getUsername();

                    String initialCode = """
                import org.dreambot.api.script.*;
                import org.dreambot.api.methods.Calculations;
                import org.dreambot.api.utilities.Logger;
                import org.dreambot.api.utilities.Sleep;
                import org.dreambot.api.script.Category;
                import java.awt.*;
                
                @ScriptManifest(name = "Script name here", description = "Script description here", author = """ + "\"" + dreamBotUser + "\"" +

                            """
                            , version = 1.0, category = Category.MISC, image = "")
                            public class REPLClass extends AbstractScript {
                            \t
                            \t@Override
                            \tpublic void onStart() {
                            \t\tLogger.log("Script started!");
                            \t}
                            \t
                            \t@Override
                            \tpublic int onLoop() {
                            \t\t
                            \t\treturn Calculations.random(200, 600);
                            \t}
                            \t
                            \t@Override
                            \tpublic void onPaint(Graphics graphics) {
                            \t\t
                            \t}
                            \t
                            \t@Override
                            \tpublic void onExit() {
                            \t}
                            }""";

                    codeInputArea.setText(initialCode);

                }
            } catch (Exception e) {
                Logger.log("Error processing input: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public String CheckContentErrors(String content, String[][] importChecks) {
        try {
            Set<String> importsToAdd = new LinkedHashSet<>(); // Preserve insertion order
            boolean contentChanged = false;

            for (String[] check : importChecks) {
                String importLine = check[0];
                String method = check[1];
                String replacement = check[2];

                // Pattern to detect method usage that might require the import
                Pattern pattern = Pattern.compile(Pattern.quote(method));
                Matcher matcher = pattern.matcher(content);
                StringBuilder buffer = new StringBuilder();
                boolean foundMethod = false;

                while (matcher.find() && !content.contains(importLine)) {
                    foundMethod = true;
                    Logger.log(String.format("Found %s, import not found, need to %s", method, importLine));
                    // Replace the method call with the specified replacement
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(buffer);

                // If the method requiring import was found and the import is not in the content
                if (foundMethod && !content.contains(importLine)) {
                    importsToAdd.add(importLine);
                }

                // Update content if any replacements were made
                if (buffer.length() > 0) {
                    content = buffer.toString();
                }
            }

            // Build the final content with needed imports at the top
            StringBuilder finalContent = new StringBuilder();
            importsToAdd.forEach(importLine -> finalContent.append(importLine).append("\n"));
            finalContent.append(content);
            String sendMe = finalContent.toString().replaceFirst("java", "").replaceAll("```", "")
                    .replaceAll("Sleep.Sleep.", "Sleep.").replaceAll(".useItemOn", ".combine")
                    .replaceAll("Bank.openClosest","Bank.open").replaceAll("Dialogues.chooseOption","Dialogues.typeOption")
                    .replaceAll("impl.inventory.Inventory", "impl.Inventory").trim();
            Logger.log("Imports finished!");

            appendToChatLog(sendMe);
            return sendMe;
        }   catch(Exception e) { return e.toString(); }
    }


}

