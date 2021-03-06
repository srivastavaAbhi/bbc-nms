package org.motechproject.nms.masterdata.it;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.*;
import org.motechproject.nms.masterdata.event.handler.HealthFacilityCsvUploadHandler;
import org.motechproject.nms.masterdata.service.*;
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
 * This class is used to test(IT) the operations of HealthFacility Csv
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)

public class HealthFacilityCsvHandlerIT extends BasePaxIT {

    private HealthFacilityCsvUploadHandler healthFacilityCsvHandler;

    List<Long> createdIds = new ArrayList<Long>();

    @Inject
    private StateService stateService;

    @Inject
    private DistrictService districtService;

    @Inject
    private TalukaService talukaService;

    @Inject
    private HealthFacilityCsvService healthFacilityCsvService;

    @Inject
    private HealthFacilityService healthFacilityService;

    @Inject
    private HealthBlockService healthBlockService;

    @Inject
    private BulkUploadErrLogService bulkUploadErrLogService;

    @Before
    public void setUp() {
        healthFacilityCsvHandler = new HealthFacilityCsvUploadHandler(stateService,
                districtService, talukaService, healthFacilityCsvService,
                healthFacilityService, healthBlockService, bulkUploadErrLogService);
    }

    @Test
    public void testDataServiceInstance() throws Exception {
        assertNotNull(healthFacilityCsvService);
        assertNotNull(healthFacilityService);
        assertNotNull(talukaService);
        assertNotNull(districtService);
        assertNotNull(stateService);
        assertNotNull(bulkUploadErrLogService);
        assertNotNull(healthFacilityCsvHandler);
    }

    @Test
    public void testHealthFacilityCsvHandler() {

        State stateData = TestHelper.getStateData();
        District districtData = TestHelper.getDistrictData();
        Taluka talukaData = TestHelper.getTalukaData();
        HealthBlock healthBlockData = TestHelper.getHealthBlockData();

        stateData.getDistrict().add(districtData);
        districtData.getTaluka().add(talukaData);
        talukaData.getHealthBlock().add(healthBlockData);

        stateService.create(stateData);

        HealthFacilityCsv csvData = TestHelper.getHealthFacilityCsvData();
        HealthFacilityCsv invalidCsvData = TestHelper.getInvalidHealthFacilityCsvData();
        createHealthFacilityCsvData(csvData);
        createHealthFacilityCsvData(invalidCsvData);

        createdIds.add(csvData.getId());
        createdIds.add(invalidCsvData.getId());
        createdIds.add(csvData.getId() + 1);

        healthFacilityCsvHandler.healthFacilityCsvSuccess(TestHelper.createMotechEvent(createdIds, LocationConstants.HEALTH_FACILITY_CSV_SUCCESS));
        HealthFacility healthFacilityData = healthFacilityService.findHealthFacilityByParentCode(123L, 456L, 8L, 1002L, 1111L);

        assertNotNull(healthFacilityData);
        assertTrue(123L == healthFacilityData.getStateCode());
        assertTrue(456L == healthFacilityData.getDistrictCode());
        assertTrue(8L == healthFacilityData.getTalukaCode());
        assertTrue(1002L == healthFacilityData.getHealthBlockCode());
        assertTrue(1111L == healthFacilityData.getHealthFacilityCode());
        assertTrue(9999L == healthFacilityData.getHealthFacilityType());
        assertTrue("HF1".equals(healthFacilityData.getName()));

        csvData = TestHelper.getUpdateHealthFacilityCsvData();
        createHealthFacilityCsvData(csvData);

        clearId();
        createdIds.add(csvData.getId());

        healthFacilityCsvHandler.healthFacilityCsvSuccess(TestHelper.createMotechEvent(createdIds, LocationConstants.HEALTH_FACILITY_CSV_SUCCESS));
        HealthFacility healthFacilityUpdateData = healthFacilityService.findHealthFacilityByParentCode(123L, 456L, 8L, 1002L, 1111L);

        assertNotNull(healthFacilityUpdateData);
        assertTrue(123L == healthFacilityUpdateData.getStateCode());
        assertTrue(456L == healthFacilityUpdateData.getDistrictCode());
        assertTrue(8L == healthFacilityUpdateData.getTalukaCode());
        assertTrue(1002L == healthFacilityUpdateData.getHealthBlockCode());
        assertTrue(1111L == healthFacilityUpdateData.getHealthFacilityCode());
        assertTrue(9999L == healthFacilityUpdateData.getHealthFacilityType());
        assertTrue("HF2".equals(healthFacilityUpdateData.getName()));
    }

    private void clearId() {
        createdIds.clear();
    }

    private void createHealthFacilityCsvData(HealthFacilityCsv csvData) {
        healthFacilityCsvService.create(csvData);
    }
}
