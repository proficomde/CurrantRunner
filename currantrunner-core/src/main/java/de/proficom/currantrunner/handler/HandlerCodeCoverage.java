package de.proficom.currantrunner.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricsBase;
import de.proficom.currantrunner.metrics.MetricsCoverageBranch;
import de.proficom.currantrunner.metrics.MetricsCoverageComplexity;
import de.proficom.currantrunner.metrics.MetricsCoverageInstructions;

/**
 * During unit tests the testcase data will be based additionally on coverage
 * information and duration of test execution
 */
public class HandlerCodeCoverage implements ITestCaseHandler {

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();

		// Add test coverage metrics
		_metrics.add(new MetricsCoverageInstructions());
		_metrics.add(new MetricsCoverageBranch());
		_metrics.add(new MetricsCoverageComplexity());

		return _metrics;
	}

	@Override
	public void onTestStarted(TestCase tc) {
		// Reset the coverage information in JaCoCo
		try {
			IAgent jacocoAgent = org.jacoco.agent.rt.RT.getAgent();
			jacocoAgent.reset();
		} catch (Exception e) {
			System.out.println("[CurrantRunner][JaCoCo] Error: " + e.getMessage());
		}
	}

	@Override
	public void onTestFinished(TestCase tc, TestCase.Results result, Duration tmExecution) {
		// Analyze coverage information with JaCoCo
		CoverageCounters jacocoCoverage = new CoverageCounters();
		try {
			IAgent jacocoAgent = org.jacoco.agent.rt.RT.getAgent();

			// Get execution data from agent
			// Thereby the Coverage Information is reset
			ByteArrayInputStream jacocoExecDataStream = new ByteArrayInputStream(jacocoAgent.getExecutionData(true));
			ExecutionDataReader jacocoExecDataReader = new ExecutionDataReader(jacocoExecDataStream);
			ExecutionDataStore jacocoDataStore = new ExecutionDataStore();

			// Read all execution data
			// Thereby fill coverage's datastore and a list of all used class names
			Set<String> allAvailableClasses = new HashSet<>();
			jacocoExecDataReader.setSessionInfoVisitor(new ISessionInfoVisitor() {
				public void visitSessionInfo(final SessionInfo info) {
					// Nothing to do
				}
			});
			jacocoExecDataReader.setExecutionDataVisitor(new IExecutionDataVisitor() {
				public void visitClassExecution(final ExecutionData data) {
					jacocoDataStore.put(data);
					allAvailableClasses.add(data.getName());
				}
			});
			jacocoExecDataReader.read();
			jacocoExecDataStream.close();

			// Now iterate all classes in JVM and calculate coverage information
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(jacocoDataStore, coverageBuilder);
			Iterator<String> itClasses = allAvailableClasses.iterator();
			while (itClasses.hasNext()) {
				String nameOfClass = itClasses.next();
				String nameOfClassResource = '/' + nameOfClass.replace('.', '/') + ".class";
				InputStream classResource = getClass().getResourceAsStream(nameOfClassResource);
				analyzer.analyzeClass(classResource, nameOfClass);
			}
			
			// Now we have a coverage of all executed classes
			// This is a base for all our internal counter
			jacocoCoverage.updateCounters(coverageBuilder);
		} catch (Exception e) {
			System.err.println("[CurrantRunner][JaCoCo] Error: " + e.getMessage());
		}

		// Forward these informations to test case metrics
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if ((curMetric instanceof MetricsCoverageInstructions)
				|| (curMetric instanceof MetricsCoverageBranch)
				|| (curMetric instanceof MetricsCoverageComplexity)) {
				curMetric.updateMetricByCoverage(jacocoCoverage);
			}
		}
	}

}
