package fitnesse.reporting.history;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;

import fitnesse.FitNesseVersion;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.DateTimeUtil;
import util.TimeMeasurement;
import fitnesse.FitNesseContext;
import fitnesse.testsystems.TestSummary;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.mem.InMemoryPage;
import fitnesse.wiki.WikiPage;

public class ExecutionReportTest {
  private FitNesseContext context;

  @Before
  public void setup() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    context = FitNesseUtil.makeTestContext(root);
  }

  @Test
  public void canReadTestExecutionReport() throws Exception {
    TestExecutionReport original = new TestExecutionReport(new FitNesseVersion("version"), "rootPath");
    original.setTotalRunTimeInMillis(totalTimeMeasurementWithElapsedMillis(42));

    StringWriter writer = new StringWriter();
    original.toXml(writer, context.pageFactory.getVelocityEngine());
    ExecutionReport report = ExecutionReport.makeReport(writer.toString());
    assertTrue(report instanceof TestExecutionReport);
    assertEquals(original, report);
    assertEquals(42, report.getTotalRunTimeInMillis());
  }

  private TimeMeasurement totalTimeMeasurementWithElapsedMillis(final long millis) {
    return new TimeMeasurement() {
      @Override
      public long elapsed() {
        return millis;
      }
    };
  }

  @Test
  public void canMakeSuiteExecutionReport() throws Exception {
    SuiteExecutionReport original = new SuiteExecutionReport(new FitNesseVersion("version"), "rootPath");
    original.date = DateTimeUtil.getDateFromString("12/31/1969 18:00:00");
    original.getFinalCounts().add(new TestSummary(1, 2, 3, 4));
    original.setTotalRunTimeInMillis(totalTimeMeasurementWithElapsedMillis(41));
    long time = DateTimeUtil.getTimeFromString("12/31/1969 18:00:00");
    SuiteExecutionReport.PageHistoryReference reference = new SuiteExecutionReport.PageHistoryReference("dah", time, 3L);
    reference.setTestSummary(new TestSummary(0, 99, 0, 0));
    original.addPageHistoryReference(reference);
    StringWriter writer = new StringWriter();
    original.toXml(writer, context.pageFactory.getVelocityEngine());
    ExecutionReport report = ExecutionReport.makeReport(writer.toString());
    assertTrue(report instanceof SuiteExecutionReport);
    assertEquals(original, report);
    assertEquals(41, report.getTotalRunTimeInMillis());
  }
  
  @Test
  public void shouldHandleMissingRunTimesGraceFully() throws Exception {
    TestExecutionReport report = new TestExecutionReport(new FitNesseVersion("version"), "rootPath");
    Element element = mock(Element.class);
    NodeList emptyNodeList = mock(NodeList.class);
    when(element.getElementsByTagName("totalRunTimeInMillis")).thenReturn(emptyNodeList);
    when(emptyNodeList.getLength()).thenReturn(0);
    assertThat(report.getTotalRunTimeInMillisOrZeroIfNotPresent(element), is(0L));
    
    element = mock(Element.class);
    NodeList matchingNodeList = mock(NodeList.class);
    Node elementWithText = mock(Element.class);
    when(element.getElementsByTagName("totalRunTimeInMillis")).thenReturn(matchingNodeList);
    when(matchingNodeList.getLength()).thenReturn(1);
    when(matchingNodeList.item(0)).thenReturn(elementWithText);
    when(elementWithText.getTextContent()).thenReturn("255");
    assertThat(report.getTotalRunTimeInMillisOrZeroIfNotPresent(element), is(255L));
  }

  @Test
  public void hasRunTimesShouldBeVersionAware() throws Exception {
    assertFalse(executionReportWithVersion("v20100303").hasRunTimes());
    assertTrue(executionReportWithVersion("v20100607").hasRunTimes());
    assertTrue(executionReportWithVersion("v20100608").hasRunTimes());
  }
  
  private ExecutionReport executionReportWithVersion(final String theVersion) {
    return new ExecutionReport() {
      @Override
      protected void unpackResults(Element testResults) {
      }
      @Override
      public String getVersion() {
        return theVersion;
      }
    };
  }
}
