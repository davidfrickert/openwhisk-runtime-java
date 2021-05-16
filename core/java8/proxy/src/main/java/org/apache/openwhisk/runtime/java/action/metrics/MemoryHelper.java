package org.apache.openwhisk.runtime.java.action.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MemoryHelper {

	private static long pid;

	static {
		try {
			pid = Long.parseLong(new File("/proc/self").getCanonicalFile().getName());
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	private static final String getRssOfPid = "ps -q " + pid + " -o rss=";

	public static Long heapMemory(Runtime runtime) {
		return runtime.totalMemory() - runtime.freeMemory();
	}

	public static Long rssMemory(Runtime runtime) {

		StringBuilder output = new StringBuilder();

		try {
			final Process proc = runtime.exec(getRssOfPid);
			proc.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return Long.parseLong(output.toString().trim()) * 1000; // ps returns value in kb
	}
}
