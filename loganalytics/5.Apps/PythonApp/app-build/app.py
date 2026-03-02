#######################################################################################################################
#
#
#   Project         :   Python based Demo Application showing Prometheus metrics
#
#   File            :   app.py
#
#   Description     :   
#
#   By              :   George Leonard
#   Email           :   georgelza@gmail.com
#
#   Created         :   Feb 2026
#
#########################################################################################################################
__author__      = "George Leonard"
__email__       = "georgelza@gmail.com"
__version__     = "1.0"
__copyright__   = "Copyright 2026"

import json
import os
import random
import sys
import time

from prometheus_client import Gauge, Histogram, Info, Counter, Summary, start_http_server

# ---------------------------------------------------------------------------
# Config from environment (populated via ConfigMap)
# ---------------------------------------------------------------------------
APP_NAME      = os.environ.get("APP_NAME", "python-prometheus-demo")
SLEEP_MIN     = float(os.environ.get("SLEEP_MIN", "1"))
SLEEP_MAX     = float(os.environ.get("SLEEP_MAX", "5"))
MAX_RUN_RAW   = os.environ.get("MAX_RUN", "")          # empty string → run forever
MAX_RUN       = int(MAX_RUN_RAW) if MAX_RUN_RAW.strip() else None
METRICS_PORT  = int(os.environ.get("METRICS_PORT", "8000"))

# ---------------------------------------------------------------------------
# Structured logger  – every line is a JSON object, AppName is always first
# ---------------------------------------------------------------------------
def log(mod_name: str, level: str, event: str, **fields):
    
    """Emit a single JSON log line.
    Column order: app | module | level | ts | event | <remaining fields>
    app:        Can be a service/system name,
    module:     Think of module, as class/function/method name/description
    level:      is standard logging levels, info, warning, error, critical, etc
    ts:         time stamp
    event:      
    ...
    """
    
    record = {
        "app":      APP_NAME,
        "module":   mod_name,
        "level":    level,
        "ts":       time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "event":    event,
    }
    record.update(fields)
    print(json.dumps(record), flush=True)
# end log

# ---------------------------------------------------------------------------
# Prometheus metrics
# ---------------------------------------------------------------------------
# Info: app identity & config (sourced from ConfigMap)
app_info = Info("app", "Application information")

# Gauge: random number
random_number_metric = Gauge("random_number", "Random number generated each loop")

# Gauge: percentage complete (only meaningful when MAX_RUN is set)
pct_complete_metric = Gauge("loop_pct_complete",
                             "Current loop count as % of MaxRun (0 when unlimited)")

# Summary: loop elapsed time (_count + _sum → avg derivable in Grafana)
loop_summary = Summary("loop_duration_seconds", "Summary of loop execution times")

# Counter: total iterations executed
loop_counter = Counter("loop_total", "Total number of while loop iterations executed")

# Histogram: per-loop elapsed time, 5 explicit buckets
loop_histogram = Histogram(
    "loop_execution_seconds",
    "Histogram of loop execution times",
    buckets=[1.0, 2.0, 3.0, 4.0, 5.0],
)

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    
    start_time     = time.time()
    start_time_str = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(start_time))

    # Populate Info metric – includes all ConfigMap-sourced values
    info_labels = {
        "app_name":   APP_NAME,
        "start_time": start_time_str,
        "version":    "1.0.0",
        "sleep_min":  str(SLEEP_MIN),
        "sleep_max":  str(SLEEP_MAX),
        "max_run":    str(MAX_RUN) if MAX_RUN is not None else "unlimited",
    }
    app_info.info(info_labels)

    # Start Prometheus HTTP server
    start_http_server(METRICS_PORT)

    # Log all startup info
    log("main", "INFO", "startup",
        metrics_port=METRICS_PORT,
        **{k: v for k, v in info_labels.items()})

    # ---------------------------------------------------------------------------
    # Main loop
    # ---------------------------------------------------------------------------
    loop_count = 0

    while True:
        # Check if we've hit the cap
        if MAX_RUN is not None and loop_count >= MAX_RUN:
            total_runtime = time.time() - start_time
            log("main", "INFO", "max_run_reached",
                max_run=MAX_RUN,
                current_run=loop_count,
                total_execution_time_s=round(total_runtime, 2))
            sys.exit(0)
        #end if
        
        loop_start = time.time()

        # Random sleep within ConfigMap-defined bounds
        sleep_time = random.uniform(SLEEP_MIN, SLEEP_MAX)
        time.sleep(sleep_time)

        # Measure actual elapsed wall-clock time for this iteration
        elapsed       = time.time() - loop_start
        total_runtime = time.time() - start_time

        # Update metrics
        random_number_metric.set(random.randint(1, 10))
        loop_counter.inc()
        loop_summary.observe(elapsed)
        loop_histogram.observe(elapsed)

        loop_count += 1

        # Percentage complete (0.0 when running continuously)
        pct = round((loop_count / MAX_RUN) * 100, 2) if MAX_RUN else 0.0
        pct_complete_metric.set(pct)

        # Structured log line per iteration
        log("main", "INFO", "loop_tick",
            max_run=MAX_RUN if MAX_RUN is not None else "unlimited",
            current_run=loop_count,
            pct_complete=pct if MAX_RUN else "n/a",
            total_execution_time_s=round(total_runtime, 2),
            loop_execution_time_s=round(elapsed, 2),
            sleep_s=round(sleep_time, 2),
            random_number=int(random_number_metric._value.get()))
    
    # end while
# end main