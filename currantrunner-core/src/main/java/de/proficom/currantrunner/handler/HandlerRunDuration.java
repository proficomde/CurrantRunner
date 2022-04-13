package de.proficom.currantrunner.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricsBase;
import de.proficom.currantrunner.metrics.MetricsDurationMilliSec;

/**
 * Handler to store the duration of test execution
 * It's either given by test framework (e.g. TestNG) or measured internally.
 */
public class HandlerRunDuration implements ITestCaseHandler {
	Instant tmTestStarted = Instant.now();

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();
		_metrics.add(new MetricsDurationMilliSec());
		return _metrics;
	}

	@Override
	public void onTestStarted(TestCase tc) {
		// Remember the timestamp when test has started
		this.tmTestStarted = Instant.now();
	}

	@Override
	public void onTestFinished(TestCase tc, TestCase.Results result, Duration tmExecution) {
		Duration durationOfTestcase;

		// Get the duration of the test case run
		// Use either the parameter from handler or (if it's not set) use the own
		// calculation.
		if (tmExecution.isZero() || tmExecution.isNegative()) {
			Instant tmTestFinished = Instant.now();
			durationOfTestcase = Duration.between(this.tmTestStarted, tmTestFinished);
		} else {
			durationOfTestcase = tmExecution;
		}

		// Forward these informations to test case metrics
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if (curMetric instanceof MetricsDurationMilliSec) {
				curMetric.updateMetricByDuration(durationOfTestcase);
			}
		}
	}

}
