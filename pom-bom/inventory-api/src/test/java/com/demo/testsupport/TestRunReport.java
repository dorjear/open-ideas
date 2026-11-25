package com.demo.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** An offline report updated as each test starts/finishes, including failed runs. */
final class TestRunReport {

    private static final Path ROOT = Path.of(System.getProperty("test.reports.directory", "target/test-reports"))
            .toAbsolutePath().normalize();
    private static final Instant STARTED = Instant.now();
    private static final String RUN_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS'Z'")
            .withZone(ZoneOffset.UTC).format(STARTED) + "-" + ProcessHandle.current().pid();
    private static final Path RUN_DIRECTORY = ROOT.resolve("runs").resolve(RUN_ID);
    private static final Map<String, Case> CASES = new LinkedHashMap<>();

    private TestRunReport() {
    }

    static synchronized Case start(String name) {
        String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
        safeName = safeName.substring(0, Math.min(safeName.length(), 120));
        String id = safeName + "-" + UUID.randomUUID().toString().substring(0, 8);
        Case test = new Case(id, name, RUN_DIRECTORY.resolve("logs").resolve(id + ".log"));
        try {
            Files.createDirectories(test.logFile.getParent());
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot create test log directory", failure);
        }
        CASES.put(id, test);
        render();
        return test;
    }

    static synchronized void finish(Case test, String status, Long elapsedMs, Throwable failure) {
        test.status = status;
        test.elapsedMs = elapsedMs;
        test.detail = failure == null ? "" : failure.toString();
        render();
    }

    private static void render() {
        write(ROOT.resolve("current-run.txt"), RUN_ID);
        write(ROOT.resolve("index.html"), html("runs/" + RUN_ID + "/logs/"));
        write(RUN_DIRECTORY.resolve("index.html"), html("logs/"));
    }

    private static String html(String logPrefix) {
        StringBuilder rows = new StringBuilder();
        StringBuilder counts = new StringBuilder();
        for (String status : new String[]{"PASSED", "FAILED", "ABORTED", "SKIPPED", "RUNNING"}) {
            long count = CASES.values().stream().filter(test -> test.status.equals(status)).count();
            counts.append("<span class=\"badge ").append(status).append("\">")
                    .append(status).append(": ").append(count).append("</span> ");
        }
        for (Case test : CASES.values()) {
            rows.append("<tr><td><span class=\"badge ").append(test.status).append("\">")
                    .append(test.status).append("</span></td><td><strong>").append(escape(test.name))
                    .append("</strong>");
            if (!test.detail.isEmpty()) {
                rows.append("<div class=\"detail\">").append(escape(test.detail)).append("</div>");
            }
            rows.append("</td><td>").append(test.elapsedMs == null ? "—" : test.elapsedMs + " ms")
                    .append("</td><td><a href=\"").append(logPrefix).append(test.id)
                    .append(".log\">Open log</a></td></tr>");
        }
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Test run report</title>
                <style>
                body{font:15px/1.5 system-ui,sans-serif;margin:32px;color:#172b4d;background:#f5f7fa}
                main{max-width:1280px;margin:auto}h1{margin-bottom:4px}.meta,.detail{color:#536174}
                .badge{display:inline-block;padding:4px 9px;border-radius:5px;background:#e4e8ed;font-size:12px;font-weight:650}
                .PASSED{background:#d9f3e3;color:#17613a}.FAILED{background:#ffe1e1;color:#9f2020}
                .ABORTED{background:#fff0cb;color:#805300}.RUNNING{background:#dcecff;color:#174e91}
                table{width:100%%;border-collapse:collapse;background:white;margin-top:24px}
                th,td{text-align:left;padding:12px;border-bottom:1px solid #e4e8ed;vertical-align:top}
                th{background:#eaf0f6}td:nth-child(2){overflow-wrap:anywhere}td:nth-child(3){white-space:nowrap}
                a{color:#1556a4}.detail{font-family:monospace;margin-top:6px;white-space:pre-wrap}
                </style></head><body><main>
                <h1>Test run report</h1><p class="meta">Started (UTC): %s<br>Run: %s</p>
                <p>%s</p><p class="meta">Refresh this page during a run to see updates. Each log includes step,
                request, response, and final-result messages emitted during that test, including cleanup.</p>
                <!-- COVERAGE:START -->
                <section><h2>Code coverage</h2><p class="meta">Line, method, and branch coverage will appear
                after Maven completes the JaCoCo report. Coverage is not available for this run yet.</p></section>
                <!-- COVERAGE:END -->
                <table><thead><tr><th>Result</th><th>Test case</th><th>Duration</th><th>Log</th></tr></thead>
                <tbody>%s</tbody></table></main></body></html>
                """.formatted(STARTED, RUN_ID, counts, rows);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void write(Path destination, String content) {
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "report-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot write test report " + destination, failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Do not hide the original write failure.
                }
            }
        }
    }

    static final class Case {
        final String id;
        final String name;
        final Path logFile;
        String status = "RUNNING";
        Long elapsedMs;
        String detail = "";

        Case(String id, String name, Path logFile) {
            this.id = id;
            this.name = name;
            this.logFile = logFile;
        }
    }
}
