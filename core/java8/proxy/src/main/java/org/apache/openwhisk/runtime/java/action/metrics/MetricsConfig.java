package org.apache.openwhisk.runtime.java.action.metrics;

public interface MetricsConfig {
    default String getPushHost() {
        return "10.147.18.110";
    }
    default int getPushPort() {
        return 9092;
    }
    default String getPushAddress() {
        return getPushHost() + ":" + getPushPort();
    }

    static MetricsConfig defaultConfigs() {
        return new MetricsConfig() {

        };
    }
}
