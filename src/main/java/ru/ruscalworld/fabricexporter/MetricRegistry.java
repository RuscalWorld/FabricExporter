package ru.ruscalworld.fabricexporter;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleCollector;
import ru.ruscalworld.fabricexporter.config.MainConfig;
import ru.ruscalworld.fabricexporter.metrics.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

public class MetricRegistry {
    private final FabricExporter exporter;
    private final List<Metric> metrics = new ArrayList<>();
    private final HashMap<String, Collector> customMetrics = new HashMap<>();
    private final Timer timer;

    public MetricRegistry(FabricExporter exporter) {
        this.exporter = exporter;
        timer = new Timer();
    }

    public void runUpdater() {
        MainConfig config = this.getExporter().getConfig();
        MetricUpdater metricUpdater = new MetricUpdater(this.getExporter());
        this.getMetrics().forEach(metricUpdater::registerMetric);
        this.getTimer().schedule(metricUpdater, 1000, config.getUpdateInterval());
    }

    public void registerDefault() {
        this.registerMetric(new OnlinePlayers());
        this.registerMetric(new Entities());
        this.registerMetric(new LoadedChunks());
        this.registerMetric(new TotalLoadedChunks());

        if (this.getExporter().getConfig().shouldUseSpark()) {
            this.registerMetric(new TicksPerSecond());
            this.registerMetric(new MillisPerTick());
        }

        this.registerCustomMetric("handshakes", new Counter.Builder()
                .name(getMetricName("handshakes"))
                .help("Amount of handshake requests")
                .labelNames("type")
        );
    }

    public void registerMetric(Metric metric) {
        MainConfig config = this.getExporter().getConfig();
        if (metric instanceof SparkMetric && !config.shouldUseSpark()) return;
        if (this.isDisabled(metric.getName())) return;

        metric.getGauge().register();
        this.metrics.add(metric);
    }

    public void registerCustomMetric(String name, SimpleCollector.Builder<?, ?> collector) {
        if (this.isDisabled(name)) return;
        this.getCustomMetrics().put(name, collector.register());
    }

    public boolean isDisabled(String name) {
        MainConfig config = this.getExporter().getConfig();
        String property = "enable-" + name.replace("_", "-");
        return !config.getProperties().getProperty(property, "true").equals("true");
    }

    public static String getMetricName(String name) {
        return "minecraft_" + name;
    }

    public static String[] getGlobalLabelNames() {
        return new String[] { "instance" };
    }

    public String[] getGlobalLabelValues() {
        return new String[] {
                this.getExporter().getConfig().getInstanceName(), // server
        };
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public FabricExporter getExporter() {
        return exporter;
    }

    public Timer getTimer() {
        return timer;
    }

    public HashMap<String, Collector> getCustomMetrics() {
        return customMetrics;
    }
}
