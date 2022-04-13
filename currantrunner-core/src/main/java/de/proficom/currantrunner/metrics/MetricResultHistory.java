package de.proficom.currantrunner.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.core.TestCase.Results;

/**
 * The "HISTORY" value is based on the number of PASSED/FAILED results in the past.
 * Value is stored in DB as string, separated by comma.
 */
public class MetricResultHistory extends MetricsBaseString {
	private int HISTORY_DEPTH = 5;
	
	/**
	 * Constructor
	 * 
	 * @param depth		number of tests taken from the past
	 */
	public MetricResultHistory(int depth) {
		HISTORY_DEPTH = depth;
	}

	@Override
	public String getCliName() {
		return "Last " + Integer.toString(HISTORY_DEPTH) + " results";
	}
	
	@Override
	public int getCliMinLength() {
		// Every item is rendered with '0'/'1', separated by comma
		// which will result in two character per item (except last item)
		return 2 * HISTORY_DEPTH - 1;
	}
	
	@Override
	public String getDBColumnName() {
		return "history_" + Integer.toString(HISTORY_DEPTH);
	}

	@Override
	public void updateMetricByResult(TestCase.Results result) {
		// Append last result to list of results
		String strValue = this.getStringValue();
		if (strValue == null || strValue.length() == 0) {
			strValue = "";
		}
	
		// Split current values
		String[] historyArray = strValue.split(",");
		List<String> historyList = new ArrayList<String>(Arrays.asList(historyArray));
		
		// Remove empty entries
		Predicate<String> filterEmpty = str -> (str.length() <= 0);
		historyList.removeIf(filterEmpty);
		
		// Append the last result
		if (result == Results.PASSED) {
			historyList.add("0");
		} else if (result == Results.FAILED) {
			historyList.add("1");
		}
		
		// If needed: Keep only last items
		while (historyList.size() > HISTORY_DEPTH) {
			historyList.remove(0);
		}
		
		// Set the new value
		this.setStringValue(String.join(",", historyList));
	}
	
	/**
	 * Transforms a list of failure history ([0, 0, 1, 1]) into a value between 0
	 * and 1 to train the model with that failure history. The more recently
	 * failures happened, the higher the value gets.
	 * 
	 * It holds: historyValue( [0,1,1,0] ) < historyValue( [0,0,1,1] )
	 * 
	 * @return a number describing how often this test case failed in the past
	 *         between 0 and 1
	 */
	@Override
	public double getMLValue() {
		/*
		 * If it a new test case, the failure history is empty, therefore the test never
		 * failed and 0 is returned. Nevertheless new test cases are executed as first.
		 * Normally new test cases weren't prioritized neither.
		 */
		String failureString = this.getStringValue();
		if (failureString == null || failureString.length() == 0) {
			return 0.0;
		}

		// Calculate the value using all results in the past
		String[] failureHistory = failureString.split(",");		
		if (failureHistory == null || failureHistory.length == 0) {
			return 0.0;
		}
		double result = 0;
		double leaningRate = 0.7;
		for (String failure : failureHistory) {
			int f = Integer.parseInt(failure);
			result += leaningRate * f - (1 - leaningRate) * result;
		}
		return result;
	}

}
