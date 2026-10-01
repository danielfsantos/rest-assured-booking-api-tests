package watcher;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Lê flakiness-report.json (gerado pelo FlakinessAnalyzer) e gera um
 * dashboard HTML autocontido, para ser publicado como artefato do CI.
 *
 * Uso: java -cp target/classes:target/test-classes com.assureapi.watcher.FlakinessDashboardGenerator
 */
public class FlakinessDashboardGenerator {

    private static final String REPORT_JSON = "flakiness-report.json";
    private static final String DASHBOARD_HTML = "flakiness-dashboard.html";
    private static final double FLAKY_THRESHOLD = 0.15;

    public static void main(String[] args) throws IOException {
        if (!Files.exists(Paths.get(REPORT_JSON))) {
            System.out.println("[FlakinessDashboard] " + REPORT_JSON + " não encontrado. Rode o FlakinessAnalyzer antes.");
            return;
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<FlakinessAnalyzer.TestFlakinessResult>>() {}.getType();
        List<FlakinessAnalyzer.TestFlakinessResult> results = gson.fromJson(
                Files.readString(Paths.get(REPORT_JSON)), listType
        );

        String html = buildHtml(results);
        Files.writeString(Paths.get(DASHBOARD_HTML), html);
        System.out.println("[FlakinessDashboard] Dashboard gerado em: " + DASHBOARD_HTML);
    }

    private static String buildHtml(List<FlakinessAnalyzer.TestFlakinessResult> results) {
        StringBuilder rows = new StringBuilder();
        for (FlakinessAnalyzer.TestFlakinessResult r : results) {
            boolean isFlaky = r.score() >= FLAKY_THRESHOLD;
            String rowClass = isFlaky ? "flaky" : "stable";
            String badge = isFlaky ? "⚠️ FLAKY" : "✅ estável";
            rows.append("<tr class=\"").append(rowClass).append("\">")
                    .append("<td>").append(escape(r.testName())).append("</td>")
                    .append("<td>").append(r.executions()).append("</td>")
                    .append("<td>").append(r.passCount()).append("</td>")
                    .append("<td>").append(r.failCount()).append("</td>")
                    .append("<td>").append(r.transitions()).append("</td>")
                    .append("<td>").append(String.format("%.2f", r.score())).append("</td>")
                    .append("<td>").append(badge).append("</td>")
                    .append("</tr>\n");
        }

        return "<!DOCTYPE html>\n"
                + "<html lang=\"pt-br\"><head><meta charset=\"UTF-8\">"
                + "<title>Flakiness Dashboard</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;margin:2rem;background:#f7f8fa;color:#222}"
                + "h1{color:#1f4e79}"
                + "table{border-collapse:collapse;width:100%;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,.1)}"
                + "th,td{padding:.6rem .8rem;text-align:left;border-bottom:1px solid #e0e0e0}"
                + "th{background:#1f4e79;color:#fff}"
                + "tr.flaky{background:#fdecea}"
                + "tr.stable{background:#fff}"
                + "</style></head><body>"
                + "<h1>Relatório de Flakiness</h1>"
                + "<p>Gerado a partir das últimas execuções registradas em test-history.jsonl</p>"
                + "<table><thead><tr>"
                + "<th>Teste</th><th>Execuções</th><th>Passes</th><th>Falhas</th>"
                + "<th>Transições</th><th>Score</th><th>Status</th>"
                + "</tr></thead><tbody>\n"
                + rows
                + "</tbody></table></body></html>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}