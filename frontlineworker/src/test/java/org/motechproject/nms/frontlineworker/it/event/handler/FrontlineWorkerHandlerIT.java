package org.motechproject.nms.frontlineworker.it.event.handler;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.nms.frontlineworker.Designation;
import org.motechproject.nms.frontlineworker.Status;
import org.motechproject.nms.frontlineworker.domain.FrontLineWorker;
import org.motechproject.nms.frontlineworker.domain.FrontLineWorkerCsv;
import org.motechproject.nms.frontlineworker.event.handler.FrontLineWorkerUploadHandler;
import org.motechproject.nms.frontlineworker.service.FrontLineWorkerCsvService;
import org.motechproject.nms.frontlineworker.service.FrontLineWorkerService;
import org.motechproject.nms.masterdata.domain.Circle;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.HealthBlock;
import org.motechproject.nms.masterdata.domain.HealthFacility;
import org.motechproject.nms.masterdata.domain.HealthSubFacility;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.domain.Taluka;
import org.motechproject.nms.masterdata.domain.Village;
import org.motechproject.nms.masterdata.repository.DistrictRecordsDataService;
import org.motechproject.nms.masterdata.repository.HealthBlockRecordsDataService;
import org.motechproject.nms.masterdata.repository.HealthFacilityRecordsDataService;
import org.motechproject.nms.masterdata.repository.HealthSubFacilityRecordsDataService;
import org.motechproject.nms.masterdata.repository.StateRecordsDataService;
import org.motechproject.nms.masterdata.repository.TalukaRecordsDataService;
import org.motechproject.nms.masterdata.repository.VillageRecordsDataService;
import org.motechproject.nms.masterdata.service.LocationService;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * This Class models the integration testing of FrontLineWorkerUploadHandler.
 */

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class FrontlineWorkerHandlerIT extends BasePaxIT {

    @Inject
    private BulkUploadErrLogService bulkUploadErrLogService;

    @Inject
    private LocationService locationService;

    @Inject
    private StateRecordsDataService stateRecordsDataService;

    @Inject
    private DistrictRecordsDataService districtRecordsDataService;

    @Inject
    private VillageRecordsDataService villageRecordsDataService;

    @Inject
    private TalukaRecordsDataService talukaRecordsDataService;

    @Inject
    private HealthBlockRecordsDataService healthBlockRecordsDataService;

    @Inject
    private HealthFacilityRecordsDataService healthFacilityRecordsDataService;

    @Inject
    private HealthSubFacilityRecordsDataService healthSubFacilityRecordsDataService;

    @Inject
    private FrontLineWorkerService frontLineWorkerService;

    @Inject
    private FrontLineWorkerCsvService frontLineWorkerCsvService;

    private FrontLineWorkerUploadHandler frontLineWorkerUploadHandler;

    private State stateData;

    private District districtData;

    private Taluka talukaData;

    private Village villageData;

    private HealthBlock healthBlockData;

    private HealthFacility healthFacilityData;

    private HealthSubFacility healthSubFacilityData;

    private static boolean setUpIsDone = false;

    private State state;

    private District district;

    private Circle circle;

    @Before
    public void setUp() {


        frontLineWorkerUploadHandler = new FrontLineWorkerUploadHandler(bulkUploadErrLogService,
                locationService,
                frontLineWorkerService, frontLineWorkerCsvService
        );

        assertNotNull(bulkUploadErrLogService);
        assertNotNull(locationService);
        assertNotNull(frontLineWorkerService);
        assertNotNull(frontLineWorkerCsvService);

        if (!setUpIsDone) {
            System.out.println("");
            state = new State();
            district = new District();
            circle = new Circle();
            state = createState();
            district = createDistrict();
            createTaluka();
            createHealthBlock();
            createHealthFacility();
            createHealthSubFacility();
            createVillage();
        }
        // do the setup
        setUpIsDone = true;

    }

    private State createState() {
        State state = new State();
        state.setName("Delhi");
        state.setStateCode(12L);
        state.setCreator("Etasha");
        state.setOwner("Etasha");
        state.setModifiedBy("Etasha");
        stateRecordsDataService.create(state);
        assertNotNull(state);
        return state;
    }

    private District createDistrict() {
        District district = new District();
        district.setName("East Delhi");
        district.setStateCode(12L);
        district.setDistrictCode(123L);
        district.setCreator("Etasha");
        district.setOwner("Etasha");
        district.setModifiedBy("Etasha");
        State stateData = stateRecordsDataService.findRecordByStateCode(district.getStateCode());
        stateData.getDistrict().add(district);
        stateRecordsDataService.update(stateData);
        assertNotNull(district);
        return district;
    }

    private void createTaluka() {
        Taluka taluka = new Taluka();
        taluka.setName("taluka");
        taluka.setStateCode(12L);
        taluka.setDistrictCode(123L);
        taluka.setTalukaCode(1L);
        taluka.setCreator("Etasha");
        taluka.setOwner("Etasha");
        taluka.setModifiedBy("Etasha");
        District districtData = districtRecordsDataService.findDistrictByParentCode(taluka.getDistrictCode(), taluka.getStateCode());
        districtData.getTaluka().add(taluka);
        districtRecordsDataService.update(districtData);
        assertNotNull(taluka);
    }

    private void createVillage() {
        Village village = new Village();
        village.setName("villageName");
        village.setStateCode(12L);
        village.setDistrictCode(123L);
        village.setTalukaCode(1L);
        village.setVillageCode(1234L);
        village.setCreator("Etasha");
        village.setOwner("Etasha");
        village.setModifiedBy("Etasha");

        Taluka talukaRecord = talukaRecordsDataService.findTalukaByParentCode(village.getStateCode(),
                village.getDistrictCode(), village.getTalukaCode());
        talukaRecord.getVillage().add(village);
        talukaRecordsDataService.update(talukaRecord);
        assertNotNull(village);
    }

    private void createHealthBlock() {
        HealthBlock healthBlock = new HealthBlock();
        healthBlock.setName("healthBlockName");
        healthBlock.setStateCode(12L);
        healthBlock.setDistrictCode(123L);
        healthBlock.setTalukaCode(1L);
        healthBlock.setHealthBlockCode(1234L);
        healthBlock.setCreator("Etasha");
        healthBlock.setOwner("Etasha");
        healthBlock.setModifiedBy("Etasha");
        Taluka talukaRecord = talukaRecordsDataService.findTalukaByParentCode(healthBlock.getStateCode(),
                healthBlock.getDistrictCode(), healthBlock.getTalukaCode());
        talukaRecord.getHealthBlock().add(healthBlock);
        talukaRecordsDataService.update(talukaRecord);

        assertNotNull(healthBlock);
    }


    private void createHealthFacility() {
        HealthFacility healthFacility = new HealthFacility();
        healthFacility.setName("healthFacilityName");
        healthFacility.setStateCode(12L);
        healthFacility.setDistrictCode(123L);
        healthFacility.setTalukaCode(1L);
        healthFacility.setHealthBlockCode(1234L);
        healthFacility.setHealthFacilityCode(12345L);
        healthFacility.setCreator("Etasha");
        healthFacility.setOwner("Etasha");
        healthFacility.setModifiedBy("Etasha");

        HealthBlock healthBlockData = healthBlockRecordsDataService.findHealthBlockByParentCode(
                healthFacility.getStateCode(), healthFacility.getDistrictCode(), healthFacility.getTalukaCode(),
                healthFacility.getHealthBlockCode());
        healthBlockData.getHealthFacility().add(healthFacility);
        healthBlockRecordsDataService.update(healthBlockData);
        assertNotNull(healthFacility);
    }


    private void createHealthSubFacility() {
        HealthSubFacility healthSubFacility = new HealthSubFacility();
        healthSubFacility.setName("healthSubFacilityName");
        healthSubFacility.setStateCode(12L);
        healthSubFacility.setDistrictCode(123L);
        healthSubFacility.setTalukaCode(1L);
        healthSubFacility.setHealthBlockCode(1234L);
        healthSubFacility.setHealthFacilityCode(12345L);
        healthSubFacility.setHealthSubFacilityCode(123456L);
        healthSubFacility.setCreator("Etasha");
        healthSubFacility.setOwner("Etasha");
        healthSubFacility.setModifiedBy("Etasha");

        HealthFacility healthFacilityData = healthFacilityRecordsDataService.findHealthFacilityByParentCode(
                healthSubFacility.getStateCode(), healthSubFacility.getDistrictCode(),
                healthSubFacility.getTalukaCode(), healthSubFacility.getHealthBlockCode(),
                healthSubFacility.getHealthFacilityCode());

        healthFacilityData.getHealthSubFacility().add(healthSubFacility);
        healthFacilityRecordsDataService.update(healthFacilityData);
        assertNotNull(healthSubFacility);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testFrontLineWorkerValidDataGetByPhnNo() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setContactNo("9990545494");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");


        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);

        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("9990545494");

        assertNotNull(flw);
        assertTrue(1L == flw.getFlwId());
        assertEquals("9990545494", flw.getContactNo());
        assertEquals(Designation.USHA, flw.getDesignation());
        assertEquals("Etasha", flw.getName());

        assertTrue(12L == flw.getStateCode());
        assertTrue(123L == flw.getDistrictId().getDistrictCode());
        assertTrue(1L == flw.getTalukaId().getTalukaCode());
        assertTrue(1234L == flw.getVillageId().getVillageCode());
        assertTrue(1234L == flw.getHealthBlockId().getHealthBlockCode());
        assertTrue(12345L == flw.getHealthFacilityId().getHealthFacilityCode());
        assertTrue(123456L == flw.getHealthSubFacilityId().getHealthSubFacilityCode());

        assertEquals("1234", flw.getAdhaarNumber());
        assertEquals("9876", flw.getAshaNumber());
        assertEquals("Etasha", flw.getCreator());
        assertEquals("Etasha", flw.getModifiedBy());
        assertEquals("Etasha", flw.getOwner());
        assertEquals(Status.INACTIVE, flw.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }


    @Test
    public void testFrontLineWorkerValidDataGetById() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("2");
        frontLineWorkerCsv.setContactNo("9990545495");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwByFlwIdAndStateId(2L, 12L);

        assertNotNull(flw);

        assertTrue(2L == flw.getFlwId());
        assertEquals("9990545495", flw.getContactNo());
        assertEquals(Designation.USHA, flw.getDesignation());
        assertEquals("Etasha", flw.getName());

        assertTrue(12L == flw.getStateCode());
        assertTrue(123L == flw.getDistrictId().getDistrictCode());
        assertTrue(1L == flw.getTalukaId().getTalukaCode());
        assertTrue(1234L == flw.getVillageId().getVillageCode());
        assertTrue(1234L == flw.getHealthBlockId().getHealthBlockCode());
        assertTrue(12345L == flw.getHealthFacilityId().getHealthFacilityCode());
        assertTrue(123456L == flw.getHealthSubFacilityId().getHealthSubFacilityCode());

        assertEquals("1234", flw.getAdhaarNumber());
        assertEquals("9876", flw.getAshaNumber());
        assertEquals("Etasha", flw.getCreator());
        assertEquals("Etasha", flw.getModifiedBy());
        assertEquals("Etasha", flw.getOwner());
        assertEquals(Status.INACTIVE, flw.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }

    @Test
    public void testFrontLineWorkerValidDataLargerphnNo() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("3");
        frontLineWorkerCsv.setContactNo("99905454950");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwByFlwIdAndStateId(3L, 12L);

        assertNotNull(flw);

        assertTrue(3L == flw.getFlwId());
        assertEquals("9905454950", flw.getContactNo());
        assertEquals(Designation.USHA, flw.getDesignation());
        assertEquals("Etasha", flw.getName());

        assertTrue(12L == flw.getStateCode());
        assertTrue(123L == flw.getDistrictId().getDistrictCode());
        assertTrue(1L == flw.getTalukaId().getTalukaCode());
        assertTrue(1234L == flw.getVillageId().getVillageCode());
        assertTrue(1234L == flw.getHealthBlockId().getHealthBlockCode());
        assertTrue(12345L == flw.getHealthFacilityId().getHealthFacilityCode());
        assertTrue(123456L == flw.getHealthSubFacilityId().getHealthSubFacilityCode());

        assertEquals("1234", flw.getAdhaarNumber());
        assertEquals("9876", flw.getAshaNumber());
        assertEquals("Etasha", flw.getCreator());
        assertEquals("Etasha", flw.getModifiedBy());
        assertEquals("Etasha", flw.getOwner());
        assertEquals(Status.INACTIVE, flw.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }

    @Test
    public void testFrontLineWorkerValidDatasmallPhnNo() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("4");
        frontLineWorkerCsv.setContactNo("99905");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwByFlwIdAndStateId(4L, 12L);

        assertNotNull(flw);

        assertTrue(4L == flw.getFlwId());
        assertEquals("99905", flw.getContactNo());
        assertEquals(Designation.USHA, flw.getDesignation());
        assertEquals("Etasha", flw.getName());

        assertTrue(12L == flw.getStateCode());
        assertTrue(123L == flw.getDistrictId().getDistrictCode());
        assertTrue(1L == flw.getTalukaId().getTalukaCode());
        assertTrue(1234L == flw.getVillageId().getVillageCode());
        assertTrue(1234L == flw.getHealthBlockId().getHealthBlockCode());
        assertTrue(12345L == flw.getHealthFacilityId().getHealthFacilityCode());
        assertTrue(123456L == flw.getHealthSubFacilityId().getHealthSubFacilityCode());

        assertEquals("1234", flw.getAdhaarNumber());
        assertEquals("9876", flw.getAshaNumber());
        assertEquals("Etasha", flw.getCreator());
        assertEquals("Etasha", flw.getModifiedBy());
        assertEquals("Etasha", flw.getOwner());
        assertEquals(Status.INACTIVE, flw.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

    }

    @Test
    public void testFrontLineWorkerNoState() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();


        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("11");//Invalid
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();

    }


    @Test
    public void testFrontLineWorkerNoDistrict() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("122");//Invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }


    @Test
    public void testFrontLineWorkerInvalidTaluka() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1233");//Invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerInvalidVillage() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1233");//invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerInvalidHealthBlock() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1233");//invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerInvalidHealthFacility() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12344");//Invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);
        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerInvalidHealthSubFacility() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123455");//Invalid
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerInvalidDesignation() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ABC");//Invalid
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerContactNoAbsent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerStateCodeAbsent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerDistrictCodeAbsent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerDesignationAbsent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }


    @Test
    public void testFrontLineWorkerTalukaAbsentVillagePresent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerTalukaAbsentHealthBlockPresent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerHBAbsentPHCPresent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerPHCAbsentSSCPresent() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("1");
        frontLineWorkerCsv.setName("etasha");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setContactNo("9990545496");
        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setSubCentreCode("123456");
        frontLineWorkerCsv.setIsValid("true");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("9990545496");
        assertNull(frontLineWorker);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);

        throw new DataValidationException();

    }


    @Test
    public void testFrontLineWorkerUpdationNoFlwId() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("10");
        frontLineWorkerCsv.setContactNo("1234567890");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Jyoti");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");

        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("1234567890");

        assertNotNull(flw);

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();
        frontLineWorkerCsvNew.setFlwId("");
        frontLineWorkerCsvNew.setContactNo("1234567890");
        frontLineWorkerCsvNew.setType("USHA");
        frontLineWorkerCsvNew.setName("Jyoti");

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("9876");
        frontLineWorkerCsvNew.setIsValid("True");
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");
        assertNotNull(frontLineWorkerCsvService);

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("1234567890");
        assertNotNull(frontLineWorker);
        assertTrue(10L == frontLineWorker.getFlwId());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();

    }

    @Test
    public void testFrontLineWorkerUpdation() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("10");
        frontLineWorkerCsv.setContactNo("1234567890");
        frontLineWorkerCsv.setType("ANM");
        frontLineWorkerCsv.setName("Jyoti");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);
        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("1234567890");

        assertNotNull(flw);

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();
        frontLineWorkerCsvNew.setFlwId("10");
        frontLineWorkerCsvNew.setContactNo("1234567890");
        frontLineWorkerCsvNew.setType("ANM");
        frontLineWorkerCsvNew.setName("Jyoti2");//changed from Jyoti to Jyoti2

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("1234");//Changed from 9876 to 1234
        frontLineWorkerCsvNew.setIsValid("True");
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");

        assertNotNull(frontLineWorkerCsvService);

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("1234567890");
        assertNotNull(frontLineWorker);
        assertEquals("1234", frontLineWorker.getAshaNumber());
        assertEquals("Jyoti2", frontLineWorker.getName());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }

    @Test
    public void testFrontLineWorkerStatusInvalidToValid() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("11");
        frontLineWorkerCsv.setContactNo("22222");
        frontLineWorkerCsv.setType("AWW");
        frontLineWorkerCsv.setName("Jaya");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("False");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("22222");

        assertNotNull(flw);
        assertEquals(Status.INVALID, flw.getStatus());

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();
        frontLineWorkerCsvNew.setFlwId("11");
        frontLineWorkerCsvNew.setContactNo("22222");
        frontLineWorkerCsvNew.setType("AWW");
        frontLineWorkerCsvNew.setName("Jaya2");//changed from Jaya to Jaya2

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("1234");//Changed from 9876 to 1234
        frontLineWorkerCsvNew.setIsValid("True");//Changed from false to true
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("22222");
        assertNotNull(frontLineWorker);
        assertEquals("9876", frontLineWorker.getAshaNumber());
        assertEquals("Jaya", frontLineWorker.getName());
        assertEquals(Status.INVALID, flw.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();

    }


    @Test
    public void testFrontLineWorkerStatusValidToInvalid() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("12");
        frontLineWorkerCsv.setContactNo("33333");
        frontLineWorkerCsv.setType("AWW");
        frontLineWorkerCsv.setName("Sushma");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("33333");

        assertNotNull(flw);
        assertEquals(Status.INACTIVE, flw.getStatus());

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();
        frontLineWorkerCsvNew.setFlwId("12");
        frontLineWorkerCsvNew.setContactNo("33333");
        frontLineWorkerCsvNew.setType("AWW");
        frontLineWorkerCsvNew.setName("Sushma");

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("1234");//Changed from 9876 to 1234
        frontLineWorkerCsvNew.setIsValid("False");//Changed from true to false
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("33333");
        assertNotNull(frontLineWorker);
        assertEquals("1234", frontLineWorker.getAshaNumber());
        assertEquals(Status.INVALID, frontLineWorker.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }

    @Test
    public void testFrontLineWorkerUpdationWithIsValidNull() {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("13");
        frontLineWorkerCsv.setContactNo("44444");
        frontLineWorkerCsv.setType("ASHA");
        frontLineWorkerCsv.setName("Rekha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");


        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("44444");

        assertNotNull(flw);
        assertEquals(Status.INACTIVE, flw.getStatus());
        assertEquals("9876", flw.getAshaNumber());

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();

        frontLineWorkerCsvNew.setFlwId("13");
        frontLineWorkerCsvNew.setContactNo("44444");
        frontLineWorkerCsvNew.setType("ASHA");
        frontLineWorkerCsvNew.setName("Rekha");

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("1234");
        frontLineWorkerCsvNew.setIsValid("True");
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("44444");
        assertNotNull(frontLineWorker);
        assertEquals("1234", frontLineWorker.getAshaNumber());
        assertEquals(Status.INACTIVE, frontLineWorker.getStatus());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
    }

    @Test
    public void testFrontLineWorkerUpdationWithNoFlwId() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("14");
        frontLineWorkerCsv.setContactNo("12345");
        frontLineWorkerCsv.setType("ANM");
        frontLineWorkerCsv.setName("Jyoti");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("12345");

        assertNotNull(flw);
        assertTrue(14L == flw.getFlwId());

        //Updation
        FrontLineWorkerCsv frontLineWorkerCsvNew = new FrontLineWorkerCsv();
        frontLineWorkerCsvNew.setContactNo("12345");
        frontLineWorkerCsvNew.setType("ANM");
        frontLineWorkerCsvNew.setName("Jyoti2");//changed from Jyoti to Jyoti2

        frontLineWorkerCsvNew.setStateCode("12");
        frontLineWorkerCsvNew.setDistrictCode("123");
        frontLineWorkerCsvNew.setTalukaCode("1");
        frontLineWorkerCsvNew.setVillageCode("1234");
        frontLineWorkerCsvNew.setHealthBlockCode("1234");
        frontLineWorkerCsvNew.setPhcCode("12345");
        frontLineWorkerCsvNew.setSubCentreCode("123456");

        frontLineWorkerCsvNew.setAdhaarNo("1234");
        frontLineWorkerCsvNew.setAshaNumber("1234");//Changed from 9876 to 1234
        frontLineWorkerCsvNew.setIsValid("True");
        frontLineWorkerCsvNew.setOwner("Etasha");
        frontLineWorkerCsvNew.setCreator("Etasha");
        frontLineWorkerCsvNew.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb2 = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsvNew);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters_new = new HashMap<>();
        List<Long> uploadedIds_new = new ArrayList<Long>();

        uploadedIds_new.add(frontLineWorkerCsvdb2.getId());
        parameters_new.put("csv-import.created_ids", uploadedIds_new);
        parameters_new.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEventNew = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters_new);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEventNew);
        thrown.expect(DataValidationException.class);

        FrontLineWorker frontLineWorker = frontLineWorkerService.getFlwBycontactNo("12345");
        assertNotNull(frontLineWorker);
        assertEquals("9876", frontLineWorker.getAshaNumber());
        assertEquals("Jyoti", frontLineWorker.getName());
        assertTrue(14L == frontLineWorker.getFlwId());

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();
    }

    @Test
    public void testFrontLineWorkerVillageWithoutTaluka() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("20");
        frontLineWorkerCsv.setContactNo("9990");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Anjali");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);


        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("9990");

        assertNull(flw);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();
    }


    @Test
    public void testFrontLineWorkerHealthBlockWithoutTaluka() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("20");
        frontLineWorkerCsv.setContactNo("9990");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("9990");

        assertNull(flw);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();
    }

    @Test
    public void testFrontLineWorkerPhcWithoutHealthBlock() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("20");
        frontLineWorkerCsv.setContactNo("9990");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setPhcCode("12345");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);

        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("9990");
        assertNull(flw);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();
    }

    @Test
    public void testFrontLineWorkerSubCentreWithoutPhc() throws DataValidationException {

        FrontLineWorkerCsv frontLineWorkerCsv = new FrontLineWorkerCsv();

        frontLineWorkerCsv.setFlwId("20");
        frontLineWorkerCsv.setContactNo("9990");
        frontLineWorkerCsv.setType("USHA");
        frontLineWorkerCsv.setName("Etasha");

        frontLineWorkerCsv.setStateCode("12");
        frontLineWorkerCsv.setDistrictCode("123");
        frontLineWorkerCsv.setTalukaCode("1");
        frontLineWorkerCsv.setVillageCode("1234");
        frontLineWorkerCsv.setHealthBlockCode("1234");
        frontLineWorkerCsv.setSubCentreCode("123456");

        frontLineWorkerCsv.setAdhaarNo("1234");
        frontLineWorkerCsv.setAshaNumber("9876");
        frontLineWorkerCsv.setIsValid("True");
        frontLineWorkerCsv.setOwner("Etasha");
        frontLineWorkerCsv.setCreator("Etasha");
        frontLineWorkerCsv.setModifiedBy("Etasha");

        FrontLineWorkerCsv frontLineWorkerCsvdb = frontLineWorkerCsvService.createFrontLineWorkerCsv(frontLineWorkerCsv);

        assertNotNull(frontLineWorkerCsvService);

        Map<String, Object> parameters = new HashMap<>();
        List<Long> uploadedIds = new ArrayList<Long>();

        uploadedIds.add(frontLineWorkerCsvdb.getId());
        parameters.put("csv-import.created_ids", uploadedIds);
        parameters.put("csv-import.filename", "FrontLineWorker.csv");

        MotechEvent motechEvent = new MotechEvent("FrontLineWorkerCsv.csv_success", parameters);
        frontLineWorkerUploadHandler.flwDataHandlerSuccess(motechEvent);
        thrown.expect(DataValidationException.class);
        FrontLineWorker flw = frontLineWorkerService.getFlwBycontactNo("9990");

        assertNull(flw);

        List<FrontLineWorkerCsv> listFlwCsv = frontLineWorkerCsvService.retrieveAllFromCsv();
        assertTrue(listFlwCsv.size() == 0);
        throw new DataValidationException();
    }
}

