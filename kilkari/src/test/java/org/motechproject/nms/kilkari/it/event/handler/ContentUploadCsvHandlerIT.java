package org.motechproject.nms.kilkari.it.event.handler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.nms.kilkari.domain.ContentType;
import org.motechproject.nms.kilkari.domain.ContentUpload;
import org.motechproject.nms.kilkari.domain.CsvContentUpload;
import org.motechproject.nms.kilkari.event.handler.CsvContentUploadHandler;
import org.motechproject.nms.kilkari.repository.ContentUploadDataService;
import org.motechproject.nms.kilkari.repository.CsvContentUploadDataService;
import org.motechproject.nms.kilkari.service.ContentUploadService;
import org.motechproject.nms.kilkari.service.CsvContentUploadService;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.Circle;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.LanguageLocationCode;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.repository.CircleDataService;
import org.motechproject.nms.masterdata.repository.DistrictRecordsDataService;
import org.motechproject.nms.masterdata.repository.LanguageLocationCodeDataService;
import org.motechproject.nms.masterdata.repository.StateRecordsDataService;
import org.motechproject.nms.masterdata.service.CircleService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class ContentUploadCsvHandlerIT extends BasePaxIT {

    @Inject
    private CircleService circleService;

    @Inject
    private LanguageLocationCodeService languageLocationCodeService;

    @Inject
    private BulkUploadErrLogService bulkUploadErrLogService;

    @Inject
    private ContentUploadService contentUploadService;

    @Inject
    private CsvContentUploadService contentUploadCsvService;

    @Inject
    private CsvContentUploadDataService csvContentUploadDataService;

    @Inject
    private ContentUploadDataService contentUploadDataService;

    @Inject
    private CircleDataService circleDataService;

    @Inject
    private StateRecordsDataService stateService;

    @Inject
    private DistrictRecordsDataService districtService;

    @Inject
    private LanguageLocationCodeDataService llcDataService;



    @Before
    public void setUp() {
        tearDown();
    }

    @Test
    public void shouldCreateContentUploadRecordsAfterCsvUpload() throws Exception {
        CsvContentUploadHandler csvHandler = new CsvContentUploadHandler(bulkUploadErrLogService, contentUploadService,
                contentUploadCsvService, circleService, languageLocationCodeService);

        List<Long> createdIds = new ArrayList<Long>();
        Long id = preSetup();


        createdIds.add(id);
        csvHandler.contentUploadCsvSuccess(createMotechEvent(createdIds));

        Assert.assertNull(contentUploadCsvService.getRecord(createdIds.get(0)));

        ContentUpload record = contentUploadService.getRecordByContentId(1L);
        Assert.assertTrue(record.getContentDuration() == 10);
        Assert.assertEquals(record.getContentType(), ContentType.PROMPT);
        Assert.assertEquals(record.getContentName(), "contentName");
        Assert.assertTrue(record.getLanguageLocationCode().toString().equals("123"));
        Assert.assertEquals(record.getCircleCode(), "circleCode");
        Assert.assertEquals(record.getContentFile(), "contentFile");
    }

    @Test
    public void shouldUpdateContentUploadRecordsAfterCsvUpload() throws Exception {
        CsvContentUploadHandler csvHandler = new CsvContentUploadHandler(bulkUploadErrLogService, contentUploadService,
                contentUploadCsvService, circleService, languageLocationCodeService);

        List<Long> createdIds = new ArrayList<Long>();
        Long id = preSetup();
        createdIds.add(id);
        csvHandler.contentUploadCsvSuccess(createMotechEvent(createdIds));
        Assert.assertNull(contentUploadCsvService.getRecord(createdIds.get(0)));
        createdIds.remove(0);


        CsvContentUpload csv = new CsvContentUpload();
        csv.setLanguageLocationCode("123");
        csv.setContentType("CONTENT");
        csv.setContentFile("contentFileChanged");
        csv.setCircleCode("circleCode");
        csv.setContentName("contentNameChanged");
        csv.setContentDuration("100");
        csv.setContentId("1");
        CsvContentUpload dbCsv = csvContentUploadDataService.create(csv);
        createdIds.add(dbCsv.getId());

        csvHandler.contentUploadCsvSuccess(createMotechEvent(createdIds));
        Assert.assertNull(contentUploadCsvService.getRecord(createdIds.get(0)));

        ContentUpload record = contentUploadService.getRecordByContentId(1L);
        Assert.assertTrue(record.getContentDuration() == 100);
        Assert.assertEquals(record.getContentType(), ContentType.CONTENT);
        Assert.assertEquals(record.getContentName(), "contentNameChanged");
        Assert.assertTrue(record.getLanguageLocationCode().toString().equals("123"));
        Assert.assertEquals(record.getCircleCode(), "circleCode");
        Assert.assertEquals(record.getContentFile(), "contentFileChanged");
    }

    @Test
    public void shouldWriteErrorLogIfCsvRecordIsNotFound() throws Exception {
        CsvContentUploadHandler csvHandler = new CsvContentUploadHandler(bulkUploadErrLogService, contentUploadService,
                contentUploadCsvService, circleService, languageLocationCodeService);

        List<Long> createdIds = new ArrayList<Long>();
        createdIds.add(1L);
        csvHandler.contentUploadCsvSuccess(createMotechEvent(createdIds));
    }

    @Test
    public void shouldRaiseDataValidationException() throws Exception {
        CsvContentUploadHandler csvHandler = new CsvContentUploadHandler(bulkUploadErrLogService, contentUploadService,
                contentUploadCsvService, circleService, languageLocationCodeService);
        Circle circle = new Circle();
        circle.setName("MotechEventCreateTest");
        circle.setCode("circleCode");
        Circle dbCircle = circleDataService.create(circle);
        //create State with statecode "1"
        State state = new State();
        state.setName("testState");
        state.setStateCode(1L);
        State dbState = stateService.create(state);
        //create district with districtCode "1" and stateCode "1"
        District district = new District();
        district.setStateCode(1L);
        district.setName("testDistrict");
        district.setDistrictCode(1L);
        district.setStateCode(1L);
        dbState = stateService.findRecordByStateCode(district.getStateCode());
        dbState.getDistrict().add(district);
        stateService.update(dbState);

        District dbDistrict = districtService.findDistrictByParentCode(1L, 1L);

        LanguageLocationCode llc = new LanguageLocationCode();
        llc.setCircleCode("circleCode");
        llc.setDistrictCode(1L);
        llc.setStateCode(1L);
        llc.setLanguageLocationCode(123);
        llc.setLanguageKK("LanguageKK");
        llc.setLanguageMA("LanguageMA");
        llc.setLanguageMK("LanguageMK");
        llc.setCircle(dbCircle);
        llc.setDistrict(dbDistrict);
        llc.setState(dbState);
        languageLocationCodeService.create(llc);

        CsvContentUpload contentCsv = new CsvContentUpload();
        contentCsv.setLanguageLocationCode("123@@@@");
        contentCsv.setContentType("Prompt");
        contentCsv.setContentFile("contentFile");
        contentCsv.setCircleCode("circleCode");
        contentCsv.setContentName("contentName");
        contentCsv.setContentDuration("10");
        contentCsv.setContentId("1");
        CsvContentUpload dbCsv = csvContentUploadDataService.create(contentCsv);

        List<Long> createdIds = new ArrayList<Long>();
        createdIds.add(dbCsv.getId());


        csvHandler.contentUploadCsvSuccess(createMotechEvent(createdIds));
    }

    public MotechEvent createMotechEvent(List<Long> ids) {
        Map<String, Object> params = new HashMap<>();
        params.put("csv-import.created_ids", ids);
        params.put("csv-import.filename", "contentUpload");
        return new MotechEvent(LocationConstants.CIRCLE_CSV_SUCCESS, params);
    }

    public Long preSetup() {
        Circle circle = new Circle();
        circle.setName("MotechEventCreateTest");
        circle.setCode("circleCode");
        Circle dbCircle = circleDataService.create(circle);

        //create State with statecode "1"
        State state = new State();
        state.setName("testState");
        state.setStateCode(1L);
        State dbState = stateService.create(state);

        //create district with districtCode "1" and stateCode "1"
        District district = new District();
        district.setStateCode(1L);
        district.setName("testDistrict");
        district.setDistrictCode(1L);
        district.setStateCode(1L);
        dbState = stateService.findRecordByStateCode(district.getStateCode());
        dbState.getDistrict().add(district);
        stateService.update(dbState);

        District dbDistrict = districtService.findDistrictByParentCode(1L, 1L);


        LanguageLocationCode llc = new LanguageLocationCode();
        llc.setCircleCode("circleCode");
        llc.setDistrictCode(1L);
        llc.setStateCode(1L);
        llc.setLanguageLocationCode(123);
        llc.setLanguageKK("LanguageKK");
        llc.setLanguageMA("LanguageMA");
        llc.setLanguageMK("LanguageMK");
        llc.setCircle(dbCircle);
        llc.setDistrict(dbDistrict);
        llc.setState(dbState);
        languageLocationCodeService.create(llc);

        CsvContentUpload contentCsv = new CsvContentUpload();
        contentCsv.setLanguageLocationCode("123");
        contentCsv.setContentType("Prompt");
        contentCsv.setContentFile("contentFile");
        contentCsv.setCircleCode("circleCode");
        contentCsv.setContentName("contentName");
        contentCsv.setContentDuration("10");
        contentCsv.setContentId("1");
        CsvContentUpload dbCsv = csvContentUploadDataService.create(contentCsv);
        return dbCsv.getId();
    }

    @After
    public void tearDown() {
        contentUploadDataService.deleteAll();
        llcDataService.deleteAll();
        stateService.deleteAll();
        circleDataService.deleteAll();
    }
}