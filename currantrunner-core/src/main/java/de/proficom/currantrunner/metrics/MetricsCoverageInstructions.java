package de.proficom.currantrunner.metrics;

import de.proficom.currantrunner.handler.CoverageCounters;

/**
 * Metric to store and retrieve INSTRUCTION COVERAGE values
 */
public class MetricsCoverageInstructions extends MetricsBaseInteger {

	@Override
	public String getCliName() {
		return "covInstr.";
	}
	
	@Override
	public int getCliMinLength() {
		return 10;
	}
	
	@Override
	public String getDBColumnName() {
		return "instructioncoverage";
	}

	// For this metric we store instruction coverage
	@Override
	public void updateMetricByCoverage(CoverageCounters coverage) {
		this.setIntegerValue(coverage.getCoveredInstructions());
	}
}
