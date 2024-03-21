import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

@ScriptManifest(category = Category.MISC, name = "REPL", author = "LethalLuck", version = 1.1)
public class Main extends AbstractScript {
    private REPLGUI gui;
    boolean updateAvailable = false;

    @Override
    public void onStart() {
        SwingUtilities.invokeLater(() -> {
            gui = new REPLGUI();
            gui.setVisible(true);
        });
        Sleep.sleep(500);

        updateAvailable = gui.CheckVersion();

        if(updateAvailable)
            BeginUpdate();

        gui.codeInputArea.revalidate();
        gui.codeInputArea.validate();
        gui.codeInputArea.setEditable(true);
    }

    @Override
    public int onLoop() {
        if (gui != null && gui.scriptRunning && ScriptManager.compiledScriptInstance != null) {
            return ScriptManager.compiledScriptInstance.onLoop();
        } else {
            // Your original onLoop logic
            return Calculations.random(200, 600);
        }
    }

    @Override
    public void onPaint(Graphics graphics) {
        if (gui != null && gui.scriptRunning && ScriptManager.compiledScriptInstance != null) {
            ScriptManager.compiledScriptInstance.onPaint(graphics);
        } else {
            // Your original onPaint logic
        }
    }

    @Override
    public void onExit() {
        if (gui != null && gui.scriptRunning && ScriptManager.compiledScriptInstance != null) {
            ScriptManager.compiledScriptInstance.onExit();
        } else {
            // Your original onExit logic
        }
        if (gui != null){
            gui.codeInputArea = null;
            gui.removeAll();
            gui.dispose();
        }
    }

    public void BeginUpdate() {
        String fileURL = "https://github.com/TotallyNotLethal/RuneGPT/releases/download/Main/DreamREPL.jar";
        String userName = System.getProperty("user.name");
        String saveDir = "C:\\Users\\" + userName + "\\DreamBot\\Scripts\\DreamREPL.jar";

        try (BufferedInputStream in = new BufferedInputStream(new URL(fileURL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(saveDir)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            System.out.println("Update completed successfully. File downloaded to: " + saveDir);
        } catch (IOException e) {
            // Handle IO exceptions (such as file not found or access denied)
            e.printStackTrace();
            System.out.println("Failed to download the update.");
        }

        stop();
    }
}