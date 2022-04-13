package de.proficom.currantrunner.metrics;

public class MetricUniqueness extends MetricsBaseDouble {

	@Override
	public String getCliName() {
		return "Uniqueness";
	}
	
	@Override
	public int getCliMinLength() {
		return 10;
	}
	
	@Override
	public String getDBColumnName() {
		return "uniqueness";
	}

	// The following functions may be overwritten to update metrics based on uniqueness of it's test name
	@Override
	public void updateMetricByUniqueness(double uniqueness) {
		this.setDoubleValue(uniqueness);
	}	

}
