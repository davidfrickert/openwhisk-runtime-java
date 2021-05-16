package org.apache.openwhisk.runtime.java.action.metrics;

public class MetricsPusher extends Thread {

	private final MetricsSupport metrics;

	public MetricsPusher(MetricsSupport metrics) {
		this.metrics = metrics;
	}

	@Override
	public void run() {
		try {
			while (true) {
				sleep(50);
				metrics.push();
			}
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}
}
