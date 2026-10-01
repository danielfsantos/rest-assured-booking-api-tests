package watcher;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;



public class TestResultWatcher  implements TestWatcher {

    private static final String HISTORY_FILE = "test-history.jsonl";

    @Override
    public void testSuccessful(ExtensionContext context) {
        recordResult(context, "PASSED", null);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        recordResult(context, "ABORTED", cause);
    }

    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        recordResult(context, "DISABLED", null);
    }

    public void recordResult(ExtensionContext context, String status, Throwable cause){
        String testClass = context.getRequiredTestClass().getSimpleName();
        String testMethod = context.getRequiredTestMethod().getName();
        String timeStamp = Instant.now().toString();
        String commitSha = System.getenv().getOrDefault("GITHUB_SHA", "local");
        String errorType = (cause != null) ? cause.getClass().getSimpleName() : "";
        String errorMessage = (cause != null && cause.getMessage() != null)
                ? sanitize(cause.getMessage())
                : "";

        String jsonLine = String.format(
                "{\"testClass\":\"%s\",\"testMethod\":\"%s\",\"status\":\"%s\"," +
                        "\"errorType\":\"%s\",\"errorMessage\":\"%s\",\"commit\":\"%s\",\"timestamp\":\"%s\"}",
                testClass, testMethod, status, errorType, errorMessage, commitSha, timeStamp
        );

        appendToFile(jsonLine);

    }


    private String sanitize(String message){
        return message.replace("\"", "'").replace("\n", " ").replace("\r", "");
    }


    private synchronized void appendToFile(String jsonLine) {
        try {
            if (!Files.exists(Paths.get(HISTORY_FILE))) {
                Files.createFile(Paths.get(HISTORY_FILE));
            }
            try (FileWriter writer = new FileWriter(HISTORY_FILE, true)) {
                writer.write(jsonLine + System.lineSeparator());
            }
        } catch (IOException e) {
            // Não deixamos a falha de escrita do histórico quebrar a suíte de testes
            System.err.println("[TestResultWatcher] Falha ao gravar histórico: " + e.getMessage());
        }
    }





    }

