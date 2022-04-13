package de.proficom.currantrunner.metrics;

import de.proficom.currantrunner.handler.CoverageCounters;

/**
 * Metric to store and retrieve COMPLEXITY of functions
 */
public class MetricsCoverageComplexity extends MetricsBaseInteger {

	@Override
	public String getCliName() {
		return "covCmplx.";
	}
	
	@Override
	public int getCliMinLength() {
		return 10;
	}
	
	@Override
	public String getDBColumnName() {
		return "complexitycoverage";
	}

	// For this metric we store complexity coverage
	@Override
	public void updateMetricByCoverage(CoverageCounters coverage) {
		this.setIntegerValue(coverage.getCoveredComplexity());
	}
}
