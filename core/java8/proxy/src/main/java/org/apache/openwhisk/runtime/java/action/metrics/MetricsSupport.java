package org.apache.openwhisk.runtime.java.action.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.PushGateway;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class MetricsSupport {
    private final MeterRegistry registry;
    private final PushGateway pushGateway;

    private static MetricsSupport instance;

    private static final UUID runtimeIdentifier = UUID.randomUUID();

    private MetricsSupport(final MetricsConfig metricsConfig) {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.pushGateway = new PushGateway(metricsConfig.getPushAddress());
    }

    public static MetricsSupport get() {
        if (instance == null) {
            instance = new MetricsSupport(MetricsConfig.defaultConfigs());
            instance.getMeterRegistry().config()
                    .commonTags("framework", "photons");
        }
        return instance;
    }

    public static Optional<MetricsSupport> getIfAvailable() {
        return Optional.ofNullable(instance);
    }

    public MeterRegistry getMeterRegistry() {
        return registry;
    }

    public PrometheusMeterRegistry getPromMeterRegistry() {
        return (PrometheusMeterRegistry) getMeterRegistry();
    }

    public void push() {
        try {
            pushGateway.pushAdd(getPromMeterRegistry().getPrometheusRegistry(), runtimeIdentifier.toString());
        } catch (IOException e) {
            System.err.println("Couldn't push metrics to external system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void delete() {
        try {
            pushGateway.delete(runtimeIdentifier.toString());
        } catch (IOException e) {
            System.err.println("Couldn't delete metrics from external system:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setFunctionName(String functionName) {
        registry.config()
                .commonTags("function.name", functionName);
    }

}
