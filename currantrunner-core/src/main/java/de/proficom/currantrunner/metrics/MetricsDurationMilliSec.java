package de.proficom.currantrunner.metrics;

import java.time.Duration;

/**
 * Metric to store and retrieve duration of test execution in milliseconds
 */
public class MetricsDurationMilliSec extends MetricsBaseInteger {
	@Override
	public String getCliName() {
		return "Duration(ms)";
	}

	@Override
	public int getCliMinLength() {
		return 10;
	}

	@Override
	public String getDBColumnName() {
		return "duration";
	}

	// Store duration value in milliseconds
	@Override
	public void updateMetricByDuration(Duration duration) {
		this.setIntegerValue((int) duration.toMillis());
	}

}
