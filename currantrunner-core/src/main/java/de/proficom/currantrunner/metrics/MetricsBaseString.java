package de.proficom.currantrunner.metrics;

/**
 * Base class for metrics based on data type STRING
 */
public abstract class MetricsBaseString extends MetricsBase<String> {

	// Read / write of value

	public String getStringValue() {
		return _value;
	};

	public void setStringValue(String value) {
		_value = value;
	}

	private String _value = "";

	// CLI
	@Override
	public String formatCliCurrentValue(String value) {
		return value;
	}

	// DB access

	@Override
	public String getDBColumnType() {
		return "VARCHAR(255)";
	}

	@Override
	public String getDBDefaultValue() {
		return "NULL";
	}

	@Override
	public void setMetricValue(String value) {
		setStringValue(value);
	}

	@Override
	public String getMetricValue() {
		return getStringValue();
	}

}
