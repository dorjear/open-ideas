package com.demo.testsupport;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Logs each invocation to its own file and reports its outcome after cleanup. */
public class TestResultLogger implements BeforeEachCallback, TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TestResultLogger.class);
    private static final String CASE_KEY = "testCaseId";
    private static final ThreadLocal<String> CURRENT_TEST = new ThreadLocal<>();
    private final Map<String, ActiveTest> activeTests = new ConcurrentHashMap<>();

    public static String currentTest() {
        return Optional.ofNullable(CURRENT_TEST.get()).orElse("test setup");
    }

    public static void step(String name) {
        LOG.info("====={}=====", name);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        ActiveTest test = open(context);
        activeTests.put(context.getUniqueId(), test);
        LOG.info("TEST START [{}]", test.result.name);
        step(context.getRequiredTestMethod().getName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        finish(context, "PASSED", null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        finish(context, "FAILED", cause);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        finish(context, "ABORTED", cause);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        finish(context, "SKIPPED", null, reason.orElse("disabled"));
    }

    private static String testName(ExtensionContext context) {
        String method = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("test");
        String display = context.getDisplayName();
        return context.getRequiredTestClass().getSimpleName() + "."
                + (display.equals(method + "()") ? display : method + " " + display);
    }

    private static ActiveTest open(ExtensionContext context) {
        TestRunReport.Case result = TestRunReport.start(testName(context));
        LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
        var root = logging.getLogger(Logger.ROOT_LOGGER_NAME);
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(logging);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n%ex");
        encoder.start();
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(logging);
        appender.setName("test-case-" + result.id);
        appender.setFile(result.logFile.toString());
        appender.setAppend(false);
        appender.setEncoder(encoder);
        appender.addFilter(new Filter<>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return result.id.equals(event.getMDCPropertyMap().get(CASE_KEY))
                        ? FilterReply.ACCEPT : FilterReply.DENY;
            }
        });
        appender.start();
        if (!appender.isStarted()) {
            encoder.stop();
            throw new IllegalStateException("Cannot open test log " + result.logFile);
        }
        root.addAppender(appender);
        String previousCaseId = MDC.get(CASE_KEY);
        MDC.put(CASE_KEY, result.id);
        CURRENT_TEST.set(result.name);
        return new ActiveTest(result, root, appender, encoder, previousCaseId, System.nanoTime());
    }

    private void finish(ExtensionContext context, String outcome, Throwable cause) {
        finish(context, outcome, cause, "");
    }

    private void finish(ExtensionContext context, String outcome, Throwable cause, String reason) {
        ActiveTest test = activeTests.remove(context.getUniqueId());
        boolean started = test != null;
        if (test == null) {
            // Disabled tests or setup failures can finish without a beforeEach callback.
            test = open(context);
        }
        Long elapsedMs = started ? (System.nanoTime() - test.startedNanos) / 1_000_000 : null;
        try {
            MDC.put(CASE_KEY, test.result.id);
            if (cause == null) {
                LOG.info("TEST RESULT [{}] {} durationMs={} {}", test.result.name, outcome,
                        elapsedMs == null ? "N/A" : elapsedMs, reason);
            } else {
                LOG.error("TEST RESULT [{}] {} durationMs={}", test.result.name, outcome,
                        elapsedMs == null ? "N/A" : elapsedMs, cause);
            }
            TestRunReport.finish(test.result, outcome, elapsedMs, cause);
        } finally {
            test.root.detachAppender(test.appender);
            test.appender.stop();
            test.encoder.stop();
            if (test.previousCaseId == null) {
                MDC.remove(CASE_KEY);
            } else {
                MDC.put(CASE_KEY, test.previousCaseId);
            }
            CURRENT_TEST.remove();
        }
    }

    private record ActiveTest(TestRunReport.Case result, ch.qos.logback.classic.Logger root,
                              FileAppender<ILoggingEvent> appender, PatternLayoutEncoder encoder,
                              String previousCaseId, long startedNanos) {
    }
}
