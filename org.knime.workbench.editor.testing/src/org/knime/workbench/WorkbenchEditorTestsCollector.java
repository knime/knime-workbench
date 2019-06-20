package org.knime.workbench;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.knime.testing.core.AbstractTestcaseCollector;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

/**
 * Testcase collector for this plug-in.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@RunWith(AllTests.class)
public class WorkbenchEditorTestsCollector extends AbstractTestcaseCollector {

	/**
	 * This is called via the JUnit framework in order to collect all testcases.
	 *
	 * @return a test suite with all testcases
	 *
	 * @throws Exception
	 *             if something goes wrong
	 */
	public static TestSuite suite() throws Exception {
		TestSuite suite = new TestSuite();

		for (Class<?> testClass : new WorkbenchEditorTestsCollector().getUnittestsClasses()) {
			suite.addTest(new JUnit4TestAdapter(testClass));
		}

		return suite;
	}

}