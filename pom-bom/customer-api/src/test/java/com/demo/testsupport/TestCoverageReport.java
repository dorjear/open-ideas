package com.demo.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Publishes JaCoCo totals after the test JVM exits and its execution data is written. */
public final class TestCoverageReport {

    private TestCoverageReport() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path coverage = Path.of(args[1]).toAbsolutePath().normalize();
        Path currentRun = root.resolve("current-run.txt");
        Path xml = coverage.resolve("jacoco.xml");
        // A skipped run or disabled agent must never publish an earlier run's totals.
        if (!Files.exists(currentRun) || !Files.exists(xml)
                || Files.getLastModifiedTime(xml).compareTo(Files.getLastModifiedTime(currentRun)) < 0) {
            return;
        }
        String runId = Files.readString(currentRun).strip();
        if (!runId.matches("[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException("Invalid test report run ID");
        }
        Path run = root.resolve("runs").resolve(runId);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element report = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
        StringBuilder totals = new StringBuilder();
        for (String type : new String[]{"LINE", "METHOD", "BRANCH"}) {
            long covered = 0;
            long missed = 0;
            var children = report.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element counter
                        && counter.getTagName().equals("counter") && counter.getAttribute("type").equals(type)) {
                    covered = Long.parseLong(counter.getAttribute("covered"));
                    missed = Long.parseLong(counter.getAttribute("missed"));
                }
            }
            long total = covered + missed;
            String percent = total == 0 ? "N/A" : String.format(Locale.ROOT, "%.1f%%", 100.0 * covered / total);
            totals.append("<tr><th>").append(type.charAt(0)).append(type.substring(1).toLowerCase(Locale.ROOT))
                    .append("</th><td>").append(percent).append("</td><td>")
                    .append(covered).append(" / ").append(total).append("</td></tr>");
        }
        try (var paths = Files.walk(coverage)) {
            for (Path source : paths.toList()) {
                Path destination = run.resolve("coverage").resolve(coverage.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        publish(root.resolve("index.html"), "runs/" + runId + "/coverage/", totals.toString());
        publish(run.resolve("index.html"), "coverage/", totals.toString());
        System.out.println("JaCoCo line, method, and branch coverage added to " + root.resolve("index.html"));
    }

    private static void publish(Path html, String prefix, String totals) throws IOException {
        String section = """
                <!-- COVERAGE:START -->
                <section><h2>Code coverage (JaCoCo)</h2>
                <p class="meta">Application code exercised by this run's unit, slice, repository, HTTP-client,
                and component tests. Test helpers and dependencies are excluded.</p>
                <table aria-label="Code coverage"><thead><tr><th>Metric</th><th>Coverage</th><th>Covered / total</th></tr></thead>
                <tbody>%s</tbody></table>
                <p><a href="%sindex.html">Open detailed JaCoCo report</a> ·
                <a href="%sjacoco.xml">XML</a> · <a href="%sjacoco.csv">CSV</a></p></section>
                <!-- COVERAGE:END -->
                """.formatted(totals, prefix, prefix, prefix);
        Files.writeString(html, Files.readString(html).replaceFirst(
                "(?s)<!-- COVERAGE:START -->.*?<!-- COVERAGE:END -->",
                java.util.regex.Matcher.quoteReplacement(section.stripTrailing())));
    }
}
