package org.motechproject.nms.masterdata.it;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.domain.StateCsv;
import org.motechproject.nms.masterdata.event.handler.StateCsvUploadHandler;
import org.motechproject.nms.masterdata.service.StateCsvService;
import org.motechproject.nms.masterdata.service.StateService;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class is used to test(IT) the operations of State Csv
 */

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class StateCsvHandlerIT extends BasePaxIT {

    @Inject
    private BulkUploadErrLogService bulkUploadErrLogService;

    @Inject
    private StateCsvService stateCsvService;

    @Inject
    private StateService stateService;

    private StateCsvUploadHandler stateCsvUploadHandler;

    List<Long> createdIds = new ArrayList<Long>();

    @Before
    public void setUp() {
        stateCsvUploadHandler = new StateCsvUploadHandler(stateService,
                stateCsvService, bulkUploadErrLogService);
    }

    @Test
    public void testDataServiceInstance() throws Exception {
        assertNotNull(stateCsvService);
        assertNotNull(stateService);
        assertNotNull(bulkUploadErrLogService);
    }

    @Test
    public void testStateCsvSuccessAndFailure() {

        StateCsv csvData = TestHelper.getStateCsvData();
        stateCsvService.create(csvData);

        StateCsv invalidCsvData = TestHelper.getInvalidStateCsvData();
        stateCsvService.create(invalidCsvData);


        createdIds.add(csvData.getId());
        createdIds.add(csvData.getId() + 1);
        createdIds.add(invalidCsvData.getId());

        stateCsvUploadHandler.stateCsvSuccess(TestHelper.createMotechEvent(createdIds, LocationConstants.STATE_CSV_SUCCESS));

        State stateData = stateService.findRecordByStateCode(123L);

        assertNotNull(stateData);
        assertTrue(123L == stateData.getStateCode());
        assertTrue("UP".equals(stateData.getName()));

        //Updated Name in State
        csvData = TestHelper.getUpdatedStateCsvData();
        stateCsvService.create(csvData);

        clearId();
        createdIds.add(csvData.getId());

        stateCsvUploadHandler.stateCsvSuccess(TestHelper.createMotechEvent(createdIds, LocationConstants.STATE_CSV_SUCCESS));
        State updatedStateData = stateService.findRecordByStateCode(123L);

        assertNotNull(updatedStateData);
        assertTrue(123L == updatedStateData.getStateCode());
        assertTrue("UK".equals(updatedStateData.getName()));
    }

    private void clearId() {

        createdIds.clear();
    }

}
