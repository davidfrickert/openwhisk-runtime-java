package org.apache.openwhisk.runtime.java.action.metrics;

public interface MetricsConfig {
    default String getPushHost() {
        return "192.168.1.100";
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
