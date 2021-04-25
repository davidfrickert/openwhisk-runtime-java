package org.apache.openwhisk.runtime.java.action.metrics;

public class MemoryHelper {
	public static Long currentMemoryUsage(Runtime runtime) {
		return runtime.totalMemory() - runtime.freeMemory();
	}
}
