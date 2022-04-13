package de.proficom.currantrunner.testng;

import java.util.ArrayList;
import java.util.List;

import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestNGMethod;

public class TestSuiteListener implements ISuiteListener {

	/**
	 * Is invoked once all the suites is about to start.
	 * 
	 * This will initialize the CurrantRunner interface and will inform
	 * CurrantRunner which tests are about to be executed.
	 */
	public void onStart(ISuite suite) {
		// Initialize CurrantRunner
		CurrantRunnerTestNG.getCurrantRunner().init();

		// Collect a list of all test cases of TestNG Test Suite
		// NOTE: When DataProviders are get only ONE testcase entry in DB. It will be
		// used n-times (where n is the number of TestNG DataProvider executions).
		List<String> allTestsInSuite = new ArrayList<String>();
		for (ITestNGMethod test : suite.getAllMethods()) {
			if (test.isTest()) {
				allTestsInSuite.add(test.getQualifiedName());
			}
		}

		// Update names in DB of CurrantRunner
		CurrantRunnerTestNG.getCurrantRunner().onTestsetStarted(allTestsInSuite);
	}

	/**
	 * Is invoked once all the suites is finished.
	 * 
	 * Re-Train the model in CurrantRunner after every suite Finally we dump the DB
	 * and close CurrantRunner
	 */
	public void onFinish(ISuite suite) {
		// Get the list of all tests in test suite
		List<String> allTestsInSuite = new ArrayList<>();
		for (ITestNGMethod test : suite.getAllMethods()) {
			if (test.isTest()) {
				allTestsInSuite.add(test.getQualifiedName());
			}
		}

		// Get the list of executed test cases
		List<String> allExecutedTests = new ArrayList<>();
		for (IInvokedMethod test : suite.getAllInvokedMethods()) {
			if (test.isTestMethod()) {
				allExecutedTests.add(test.getTestMethod().getQualifiedName());
			}
		}

		// Forward the information to CurrantRunner
		CurrantRunnerTestNG.getCurrantRunner().onTestsetFinished(allTestsInSuite, allExecutedTests);

		// Now we can train the model and dump DB to CLI
		CurrantRunnerTestNG.getCurrantRunner().trainModel();
		CurrantRunnerTestNG.getCurrantRunner().printCurrentMetrics();
		
		// Clean up CurrantRunner
		CurrantRunnerTestNG.getCurrantRunner().deinit();
	}

}
