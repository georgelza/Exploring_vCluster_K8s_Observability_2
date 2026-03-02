/*
 * #####################################################################################################################
 *
 *   Project         :   Demo Application showing Prometheus metrics
 *
 *   File            :   App.java
 *
 *   Description     :
 *
 *   By              :   George Leonard
 *   Email           :   georgelza@gmail.com
 *
 *   Created         :   Feb 2026
 *
 * #####################################################################################################################
 */

package com.observation.prometheusdemo;

import io.prometheus.client.*;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class App {

    private static final String APP_VERSION = "1.0.0";
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    // ── Config from environment (populated via ConfigMap) ─────────────────────
    private static final String  APP_NAME     = getEnv("APP_NAME",    "java-prometheus-demo");
    private static final double  SLEEP_MIN    = getEnvDouble("SLEEP_MIN", 1.0);
    private static final double  SLEEP_MAX    = getEnvDouble("SLEEP_MAX", 5.0);
    private static final int     METRICS_PORT = getEnvInt("METRICS_PORT", 8000);
    private static final String  MAX_RUN_RAW  = getEnv("MAX_RUN", "");
    private static final int     MAX_RUN;
    private static final boolean UNLIMITED;

    static {
        int  mr  = 0;
        boolean ul = true;
        if (!MAX_RUN_RAW.isBlank()) {
            try {
                mr = Integer.parseInt(MAX_RUN_RAW.trim());
                ul = (mr <= 0);
            } catch (NumberFormatException ignored) {}
        }
        MAX_RUN   = mr;
        UNLIMITED = ul;
    }

    // ── Structured logger ─────────────────────────────────────────────────────
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Emit a single JSON log line.
     * Column order: app | module | level | ts | event | remaining fields
     *
     * @param modName   think of module as class/method/function name
     * @param level     standard logging level: INFO, WARN, ERROR etc.
     * @param event     short event label
     * @param fields    additional key-value pairs appended after event
     */
    private static void logEvent(String modName, String level, String event, Map<String, Object> fields) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("app",    APP_NAME);
        record.put("module", modName);
        record.put("level",  level);
        record.put("ts",     ISO_FMT.format(Instant.now()));
        record.put("event",  event);
        record.putAll(fields);
        try {
            System.out.println(JSON.writeValueAsString(record));
            System.out.flush();
        } catch (Exception e) {
            System.err.println("log error: " + e.getMessage());
        }
    }

    // ── Prometheus metrics ────────────────────────────────────────────────────

    // Info gauge — labels carry config values
    private static final Gauge APP_INFO = Gauge.build()
            .name("app_info")
            .help("Application information")
            .labelNames("app_name", "start_time", "version", "sleep_min", "sleep_max", "max_run")
            .register();

    // Gauge: random number
    private static final Gauge RANDOM_NUMBER = Gauge.build()
            .name("random_number")
            .help("Random number generated each loop")
            .register();

    // Gauge: percentage complete
    private static final Gauge PCT_COMPLETE = Gauge.build()
            .name("loop_pct_complete")
            .help("Current loop count as % of MaxRun (0 when unlimited)")
            .register();

    // Summary: loop elapsed time
    private static final Summary LOOP_SUMMARY = Summary.build()
            .name("loop_duration_seconds")
            .help("Summary of loop execution times")
            .register();

    // Counter: total iterations
    private static final Counter LOOP_COUNTER = Counter.build()
            .name("loop_total")
            .help("Total number of while loop iterations executed")
            .register();

    // Histogram: per-loop elapsed time, 5 explicit buckets
    private static final Histogram LOOP_HISTOGRAM = Histogram.build()
            .name("loop_execution_seconds")
            .help("Histogram of loop execution times")
            .buckets(1.0, 2.0, 3.0, 4.0, 5.0)
            .register();

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException, InterruptedException {

        Instant startInstant  = Instant.now();
        String  startTimeStr  = ISO_FMT.format(startInstant);
        String  maxRunStr     = UNLIMITED ? "unlimited" : String.valueOf(MAX_RUN);

        // Populate app_info metric
        APP_INFO.labels(APP_NAME, startTimeStr, APP_VERSION,
                String.format("%.1f", SLEEP_MIN),
                String.format("%.1f", SLEEP_MAX),
                maxRunStr).set(1);

        // Start Prometheus HTTP server
        HTTPServer server = new HTTPServer.Builder()
                .withPort(METRICS_PORT)
                .build();

        // Log startup
        Map<String, Object> startFields = new LinkedHashMap<>();
        startFields.put("app_name",     APP_NAME);
        startFields.put("start_time",   startTimeStr);
        startFields.put("version",      APP_VERSION);
        startFields.put("sleep_min",    SLEEP_MIN);
        startFields.put("sleep_max",    SLEEP_MAX);
        startFields.put("max_run",      maxRunStr);
        startFields.put("metrics_port", METRICS_PORT);
        logEvent("main", "INFO", "startup", startFields);

        // ── Main loop ─────────────────────────────────────────────────────────
        Random rng       = new Random();
        int    loopCount = 0;

        while (true) {
            // Check cap
            if (!UNLIMITED && loopCount >= MAX_RUN) {
                double totalRuntime = (Instant.now().toEpochMilli() - startInstant.toEpochMilli()) / 1000.0;
                Map<String, Object> doneFields = new LinkedHashMap<>();
                doneFields.put("max_run",                MAX_RUN);
                doneFields.put("current_run",            loopCount);
                doneFields.put("total_execution_time_s", String.format("%.2f", totalRuntime));
                logEvent("main", "INFO", "max_run_reached", doneFields);
                server.close();
                System.exit(0);
            }

            long loopStartMs = System.currentTimeMillis();

            // Random sleep within bounds
            double sleepSec = SLEEP_MIN + rnd(rng) * (SLEEP_MAX - SLEEP_MIN);
            TimeUnit.MILLISECONDS.sleep((long)(sleepSec * 1000));

            double elapsed      = (System.currentTimeMillis() - loopStartMs) / 1000.0;
            double totalRuntime = (System.currentTimeMillis() - startInstant.toEpochMilli()) / 1000.0;

            // Random number 1-10
            int randNum = rng.nextInt(10) + 1;

            // Update metrics
            RANDOM_NUMBER.set(randNum);
            LOOP_COUNTER.inc();
            LOOP_SUMMARY.observe(elapsed);
            LOOP_HISTOGRAM.observe(elapsed);

            loopCount++;

            double pct    = 0.0;
            String pctStr = "n/a";
            if (!UNLIMITED) {
                pct    = (double) loopCount / MAX_RUN * 100.0;
                pctStr = String.format("%.2f", pct);
            }
            PCT_COMPLETE.set(pct);

            Map<String, Object> tickFields = new LinkedHashMap<>();
            tickFields.put("max_run",                maxRunStr);
            tickFields.put("current_run",            loopCount);
            tickFields.put("pct_complete",           pctStr);
            tickFields.put("total_execution_time_s", String.format("%.2f", totalRuntime));
            tickFields.put("loop_execution_time_s",  String.format("%.2f", elapsed));
            tickFields.put("sleep_s",                String.format("%.2f", sleepSec));
            tickFields.put("random_number",          randNum);
            logEvent("main", "INFO", "loop_tick", tickFields);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String getEnv(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    private static double getEnvDouble(String key, double fallback) {
        try { return Double.parseDouble(getEnv(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int getEnvInt(String key, int fallback) {
        try { return Integer.parseInt(getEnv(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static double rnd(Random r) { return r.nextDouble(); }
}
