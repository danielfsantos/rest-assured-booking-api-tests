package watcher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lê test-history.jsonl (gerado pelo TestResultWatcher), agrupa os eventos
 * por teste e calcula um score de flakiness para cada um.
 *
 * Definição de flakiness usada aqui: número de ALTERNÂNCIAS entre PASSED e
 * FAILED nas últimas N execuções, dividido pelo número de transições
 * possíveis (N-1). Um teste que falha sempre (regressão real) tem score 0,
 * assim como um teste que sempre passa. Um teste que alterna a cada execução
 * tem score 1.0 (o pior caso).
 *
 * Uso: java -cp target/classes:target/test-classes com.assureapi.watcher.FlakinessAnalyzer
 */
public class FlakinessAnalyzer {

    private static final String HISTORY_FILE = "test-history.jsonl";
    private static final String REPORT_FILE = "flakiness-report.json";
    private static final int WINDOW_SIZE = 20; // últimas N execuções consideradas por teste
    private static final double FLAKY_THRESHOLD = 0.15; // acima disso, marcamos como flaky

    public static void main(String[] args) throws IOException {
        List<JsonObject> events = readHistory();
        Map<String, List<JsonObject>> byTest = groupByTest(events);

        List<TestFlakinessResult> results = byTest.entrySet().stream()
                .map(entry -> calculateFlakiness(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(TestFlakinessResult::score).reversed())
                .collect(Collectors.toList());

        printReport(results);
        writeJsonReport(results);
    }

    private static List<JsonObject> readHistory() throws IOException {
        if (!Files.exists(Paths.get(HISTORY_FILE))) {
            System.out.println("[FlakinessAnalyzer] Nenhum histórico encontrado ainda (" + HISTORY_FILE + ").");
            return Collections.emptyList();
        }
        Gson gson = new Gson();
        List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
        List<JsonObject> events = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            events.add(gson.fromJson(line, JsonObject.class));
        }
        return events;
    }

    private static Map<String, List<JsonObject>> groupByTest(List<JsonObject> events) {
        Map<String, List<JsonObject>> grouped = new LinkedHashMap<>();
        for (JsonObject event : events) {
            String key = event.get("testClass").getAsString() + "#" + event.get("testMethod").getAsString();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        }
        // Ordena cada lista por timestamp (mais antigo primeiro) e mantém só a janela mais recente
        for (List<JsonObject> list : grouped.values()) {
            list.sort(Comparator.comparing(o -> o.get("timestamp").getAsString()));
        }
        return grouped;
    }

    private static TestFlakinessResult calculateFlakiness(String testName, List<JsonObject> allEvents) {
        List<JsonObject> window = allEvents.size() > WINDOW_SIZE
                ? allEvents.subList(allEvents.size() - WINDOW_SIZE, allEvents.size())
                : allEvents;

        int transitions = 0;
        int passCount = 0;
        int failCount = 0;
        String previousStatus = null;

        for (JsonObject event : window) {
            String status = event.get("status").getAsString();
            if ("PASSED".equals(status)) passCount++;
            if ("FAILED".equals(status)) failCount++;

            if (previousStatus != null && !previousStatus.equals(status)
                    && isRelevant(previousStatus) && isRelevant(status)) {
                transitions++;
            }
            previousStatus = status;
        }

        int possibleTransitions = Math.max(window.size() - 1, 1);
        double score = (double) transitions / possibleTransitions;

        return new TestFlakinessResult(testName, window.size(), passCount, failCount, transitions, score);
    }

    private static boolean isRelevant(String status) {
        // Ignora ABORTED/DISABLED no cálculo de alternância — só PASSED/FAILED importam
        return "PASSED".equals(status) || "FAILED".equals(status);
    }

    private static void printReport(List<TestFlakinessResult> results) {
        System.out.println("\n=== Relatório de Flakiness ===");
        if (results.isEmpty()) {
            System.out.println("Sem dados suficientes ainda.");
            return;
        }
        for (TestFlakinessResult r : results) {
            String flag = r.score() >= FLAKY_THRESHOLD ? "⚠️  FLAKY" : "estável";
            System.out.printf(
                    "%-50s | execuções: %-3d | passes: %-3d | falhas: %-3d | transições: %-3d | score: %.2f | %s%n",
                    r.testName(), r.executions(), r.passCount(), r.failCount(), r.transitions(), r.score(), flag
            );
        }
    }

    private static void writeJsonReport(List<TestFlakinessResult> results) throws IOException {
        Gson gson = new Gson();
        Files.writeString(Paths.get(REPORT_FILE), gson.toJson(results));
        System.out.println("\nRelatório salvo em: " + REPORT_FILE);
    }

    /**
     * Resultado do cálculo de flakiness para um teste específico.
     */
    public record TestFlakinessResult(
            String testName,
            int executions,
            int passCount,
            int failCount,
            int transitions,
            double score
    ) {}
}
