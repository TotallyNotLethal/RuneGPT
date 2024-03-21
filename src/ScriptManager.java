import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.utilities.Logger;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptManager {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private static Class<?> cls;
    public static AbstractScript compiledScriptInstance;

    public boolean compileAndLoadJavaCode(String code, String clientJarUrl) {
        // Check if the input code already contains a class definition
        boolean containsClassDefinition = code.contains("class " + "REPLClass");
        StringBuilder classCode = new StringBuilder();

        // If the input code does not contain a class definition, wrap the code
        if (!containsClassDefinition) {
            StringBuilder imports = new StringBuilder();
            StringBuilder classBody = new StringBuilder();

            // Split the input code into lines
            String[] lines = code.split("\\n");
            for (String line : lines) {
                if (line.startsWith("import")) {
                    // Add the import statement to imports
                    imports.append(line).append("\n");
                } else {
                    // Add the code line to class body
                    classBody.append(line).append("\n");
                }
            }
            // Wrap the input code in a class definition
            classCode.append(imports)
                    .append("public class ").append("REPLClass").append(" {\n")
                    .append(classBody)
                    .append("\n}");
        } else {
            // Use the input code as is
            classCode.append(code);
        }

        JavaFileObject file = new JavaSourceFromString("REPLClass", classCode.toString());
        //Logger.log(fullClassCode);

        List<String> optionList = new ArrayList<>();
// Get the current System classpath
        String classpath = System.getProperty("java.class.path");
        Logger.log(classpath);

        String userName = System.getProperty("user.name");
        String clientPath = "C:\\Users\\" + userName + "\\DreamBot\\BotData\\client.jar";

// Path to the DreamBot client.jar
        optionList.add("-classpath");
        optionList.add(classpath);


                String userHome = System.getProperty("user.home"); // This resolves to the user's home directory, equivalent to %USERPROFILE% on Windows
        String outputDir = userHome + "/DreamBot/Scripts/RuneGPT"; // Adjust the path as necessary
        Logger.log(outputDir);
        optionList.add("-d");
        optionList.add(outputDir);

        new File(outputDir).mkdirs();

        // Compilation
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, Collections.singletonList(file));
        boolean success = task.call();

        // Load and instantiate compiled class
        if (success) {
            REPLGUI.scriptRunner = () -> {
                try {
                    // Adjust the path to the directory where the compiled classes are saved
                    String compiledClassesDir = outputDir; // The directory where compiled classes are

                    // Convert the string path to a URL
                    URL compiledClassesDirURL = new File(compiledClassesDir).toURI().toURL();

                    // Include the client JAR URL
                    URL clientJarURL = new File(clientJarUrl).toURI().toURL();

                    // Create a new URLClassLoader with both the compiled classes directory and the client JAR
                    URLClassLoader classLoader = URLClassLoader.newInstance(
                            new URL[]{compiledClassesDirURL, clientJarURL},
                            this.getClass().getClassLoader() // Use the current class loader as the parent
                    );

                    // Attempt to load the class using the new class loader
                    cls = Class.forName("REPLClass", true, classLoader);
                    AbstractScript instance = (AbstractScript) cls.getDeclaredConstructor().newInstance();
                    setCompiledScriptInstance(instance);


                    Method onStartMethod = cls.getDeclaredMethod("onStart");
                    onStartMethod.invoke(instance);
                } catch (Exception e) {
                    Logger.log(e);
                }
            };
        } else {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                Logger.log(String.format("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource().toUri()));
            }

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                Logger.log(diagnostic.getCode());
                Logger.log(diagnostic.getKind());
                Logger.log(diagnostic.getPosition());
                Logger.log(diagnostic.getStartPosition());
                Logger.log(diagnostic.getEndPosition());
                Logger.log(diagnostic.getSource());
                Logger.log(diagnostic.getMessage(null));
            }

            return false;
        }
        return true;
    }

    public void setCompiledScriptInstance(AbstractScript instance) {
        compiledScriptInstance = instance;
    }
}
