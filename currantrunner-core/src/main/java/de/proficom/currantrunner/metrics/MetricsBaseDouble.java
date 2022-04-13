package de.proficom.currantrunner.metrics;

import java.util.Locale;

/**
 * Base class for metrics based on data type DOUBLE
 */
public abstract class MetricsBaseDouble extends MetricsBase<Double> {

	// Read / write of value
	
	public double getDoubleValue() {
		return _value;
	};
	
	public void setDoubleValue(double value) {
		_value = value;
	}

	private double _value = 0.0;

	// CLI
	@Override
	public String formatCliCurrentValue(Double value) {
		// Trim double values to three digits
		return String.format(Locale.US, "%.3f", value);
	}
	
	// DB access
	
	@Override
	public String getDBColumnType() {
		return "DOUBLE";
	}
	
	@Override
	public String getDBDefaultValue() {
		return "NULL";
	}

	@Override
	public void setMetricValue(Double value) {
		setDoubleValue(value);
	}

	@Override
	public Double getMetricValue() {
		return getDoubleValue();
	}

	// Machine Learning

	@Override
	public double getMLValue() {
		// For ML algorithm we usually can use the value directly
		return this.getDoubleValue();
	}
	
}
