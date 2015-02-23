package org.motechproject.nms.masterdata.event.handler;

import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.domain.Taluka;
import org.motechproject.nms.masterdata.domain.TalukaCsv;
import org.motechproject.nms.masterdata.repository.DistrictRecordsDataService;
import org.motechproject.nms.masterdata.repository.StateRecordsDataService;
import org.motechproject.nms.masterdata.repository.TalukaCsvRecordsDataService;
import org.motechproject.nms.masterdata.repository.TalukaRecordsDataService;
import org.motechproject.nms.util.BulkUploadError;
import org.motechproject.nms.util.CsvProcessingSummary;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by abhishek on 29/1/15.
 */
@Component
public class TalukaCsvUploadHandler {

    @Autowired
    private StateRecordsDataService stateRecordsDataService;


    @Autowired
    private DistrictRecordsDataService districtRecordsDataService;

    @Autowired
    private TalukaCsvRecordsDataService talukaCsvRecordsDataService;

    @Autowired
    private TalukaRecordsDataService talukaRecordsDataService;

    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(TalukaCsvUploadHandler.class);

    @MotechListener(subjects = {"mds.crud.masterdata.TalukaCsv.csv-import.success"})
    public void talukaCsvSuccess(MotechEvent motechEvent) {

            int failedRecordCount = 0;
            int successRecordCount = 0;

            Map<String, Object> params = motechEvent.getParameters();

            String csvFileName = (String)params.get("csv-import.filename");
            String logFileName = BulkUploadError.createBulkUploadErrLogFileName(csvFileName);
            CsvProcessingSummary result = new CsvProcessingSummary(successRecordCount, failedRecordCount);
            BulkUploadError errorDetails = new BulkUploadError();

            List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");
            TalukaCsv talukaCsvRecord = null;

            for (Long id : createdIds) {
                try {
                    talukaCsvRecord = talukaCsvRecordsDataService.findById(id);

                    if (talukaCsvRecord != null) {

                        Taluka newRecord = mapTalukaCsv(talukaCsvRecord);

                        State stateRecord = stateRecordsDataService.findRecordByStateCode(newRecord.getStateCode());
                        District districtRecord = districtRecordsDataService.findDistrictByParentCode(newRecord.getDistrictCode(), newRecord.getStateCode());

                        if(stateRecord != null && districtRecord !=null) {
                            insertTalukaData(newRecord);
                            result.incrementSuccessCount();
                            talukaCsvRecordsDataService.delete(talukaCsvRecord);
                        } else {
                            result.incrementFailureCount();
                            errorDetails.setRecordDetails(id.toString());
                            errorDetails.setErrorCategory("Record_Not_Found");
                            errorDetails.setErrorDescription("Record not in database");
                            bulkUploadErrLogService.writeBulkUploadErrLog(logFileName, errorDetails);
                        }
                    } else {
                        result.incrementFailureCount();
                        errorDetails.setRecordDetails(id.toString());
                        errorDetails.setErrorCategory("Record_Not_Found");
                        errorDetails.setErrorDescription("Record not in database");
                        bulkUploadErrLogService.writeBulkUploadErrLog(logFileName, errorDetails);
                    }
                } catch (DataValidationException dataValidationException) {
                    errorDetails.setRecordDetails(talukaCsvRecord.toString());
                    errorDetails.setErrorCategory(dataValidationException.getErrorCode());
                    errorDetails.setErrorDescription(dataValidationException.getErroneousField());
                    bulkUploadErrLogService.writeBulkUploadErrLog(logFileName, errorDetails);
                    talukaCsvRecordsDataService.delete(talukaCsvRecord);
                } catch (Exception e) {
                    failedRecordCount++;
                }
            }
            bulkUploadErrLogService.writeBulkUploadProcessingSummary("userName", csvFileName, logFileName, result);
    }

    @MotechListener(subjects = {"mds.crud.masterdata.TalukaCsv.csv-import.failed"})
    public void talukaCsvFailed(MotechEvent event){

        talukaCsvRecordsDataService.deleteAll();

        logger.info("Taluka successfully deleted from temporary tables");
    }

    private Taluka mapTalukaCsv(TalukaCsv record)  throws DataValidationException {
        Taluka newRecord = new Taluka();

        String talukaName = ParseDataHelper.parseString("TalukaName", record.getName(), true);
        Long stateCode = ParseDataHelper.parseLong("StateCode", record.getStateCode(),true);
        Long districtCode = ParseDataHelper.parseLong("DistrictCode", record.getDistrictCode(),true);
        String talukaCode = ParseDataHelper.parseString("TalukaCode", record.getTalukaCode(), true);

        newRecord.setName(talukaName);
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setTalukaCode(talukaCode);

        return newRecord;
    }

    private void insertTalukaData(Taluka talukaData) {

        Taluka existTalukaData = talukaRecordsDataService.findTalukaByParentCode(
                talukaData.getStateCode(),
                talukaData.getDistrictCode(),
                talukaData.getTalukaCode());

        if (existTalukaData != null) {

            talukaRecordsDataService.update(existTalukaData);
            logger.info("Taluka permanent data is successfully updated.");
        } else {
            talukaRecordsDataService.create(talukaData);
            logger.info("Taluka permanent data is successfully updated.");
        }
    }

}