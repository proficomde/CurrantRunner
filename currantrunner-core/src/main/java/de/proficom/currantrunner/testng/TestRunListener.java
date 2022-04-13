package de.proficom.currantrunner.testng;

import java.time.Duration;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import de.proficom.currantrunner.core.TestCase.Results;

/**
 * A listener that gets invoked before and after a method is invoked by TestNG.
 * This is called either for a @Test function or Before/After functions.
 */
public class TestRunListener implements IInvokedMethodListener {

	/**
	 * Invoked, before a test case runs.
	 */
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		// Inform CurrantRunner that a new test is about to start
		if (method.isTestMethod()) {
			CurrantRunnerTestNG.getCurrantRunner().onTestStarted(method.getTestMethod().getQualifiedName());
		}
	}

	/**
	 * Invoked, after a test case run. We analyze the RuntimeData and collect
	 * coverage data, the result of the test case and how long the test case took is
	 * stored in the database.
	 */
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		if (method.isTestMethod()) {
			// Convert TestNG result to CurrantRunner result
			Results currantRunnerResult = Results.FAILED;
			switch (testResult.getStatus()) {
			case ITestResult.SUCCESS:
				currantRunnerResult = Results.PASSED;
				break;
			case ITestResult.FAILURE:
				currantRunnerResult = Results.FAILED;
				break;
			case ITestResult.SKIP:
				currantRunnerResult = Results.SKIPPED;
				break;
			default:
				currantRunnerResult = Results.FAILED;
			}
			
			// Get test duration
			long durationMS = testResult.getEndMillis() - testResult.getStartMillis();

			// Inform CurrantRunner that a new test is finished
			CurrantRunnerTestNG.getCurrantRunner().onTestFinished(method.getTestMethod().getQualifiedName(),
					currantRunnerResult, Duration.ofMillis(durationMS));
		}
	}

}
