package de.proficom.currantrunner.metrics;

/**
 * Base class for metrics based on data type INTEGER
 */
public abstract class MetricsBaseInteger extends MetricsBase<Integer> {

	// Read / write of value
	
	public int getIntegerValue() {
		return _value;
	};
	
	public void setIntegerValue(Integer value) {
		_value = value;
	}

	private int _value = 0;

	// CLI
	@Override
	public String formatCliCurrentValue(Integer value) {
		return Integer.toString(value);
	}
	
	// DB access
	
	@Override
	public String getDBColumnType() {
		return "INTEGER";
	}
	
	@Override
	public String getDBDefaultValue() {
		return "NULL";
	}

	@Override
	public void setMetricValue(Integer value) {
		setIntegerValue(value);
	}

	@Override
	public Integer getMetricValue() {
		return getIntegerValue();
	}

	// Machine Learning

	@Override
	public double getMLValue() {
		// For ML algorithm we usually can use the integer value directly
		return this.getIntegerValue();
	}
	
}
