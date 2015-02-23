package org.motechproject.nms.masterdata.event.handler;

/**
 * Created by abhishek on 31/1/15.
 */

import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.domain.*;
import org.motechproject.nms.masterdata.repository.*;
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

@Component
public class HealthSubFacilityCsvUploadHandler {

    @Autowired
    private StateRecordsDataService stateRecordsDataService;

    @Autowired
    private DistrictRecordsDataService districtRecordsDataService;

    @Autowired
    private TalukaRecordsDataService talukaRecordsDataService;

    @Autowired
    private HealthFacilityCsvRecordsDataService healthFacilityCsvRecordsDataService;

    @Autowired
    private HealthFacilityRecordsDataService healthFacilityRecordsDataService;

    @Autowired
    private HealthSubFacilityCsvRecordsDataService healthSubFacilityCsvRecordsDataService;

    @Autowired
    private HealthSubFacilityRecordsDataService healthSubFacilityRecordsDataService;


    @Autowired
    private HealthBlockCsvRecordsDataService healthBlockCsvRecordsDataService;

    @Autowired
    private HealthBlockRecordsDataService healthBlockRecordsDataService;


    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(HealthSubFacilityCsvUploadHandler.class);


    @MotechListener(subjects = {"mds.crud.masterdata.HealthSubFacilityCsv.csv-import.success"})
    public void healthSubFacilityCsvSuccess(MotechEvent motechEvent) {

        int failedRecordCount = 0;
        int successRecordCount = 0;

        Map<String, Object> params = motechEvent.getParameters();

        String csvFileName = (String) params.get("csv-import.filename");
        String logFileName = BulkUploadError.createBulkUploadErrLogFileName(csvFileName);
        CsvProcessingSummary result = new CsvProcessingSummary(successRecordCount, failedRecordCount);
        BulkUploadError errorDetails = new BulkUploadError();

        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");
        HealthSubFacilityCsv healthSubFacilityCsvRecord = null;

        for (Long id : createdIds) {
            try {
                healthSubFacilityCsvRecord = healthSubFacilityCsvRecordsDataService.findById(id);

                if (healthSubFacilityCsvRecord != null) {

                    HealthSubFacility newRecord = mapHealthSubFacilityCsv(healthSubFacilityCsvRecord);

                    State stateRecord = stateRecordsDataService.findRecordByStateCode(newRecord.getStateCode());
                    District districtRecord = districtRecordsDataService.findDistrictByParentCode(newRecord.getDistrictCode(), newRecord.getStateCode());
                    Taluka talukaRecord = talukaRecordsDataService.findTalukaByParentCode(newRecord.getStateCode(), newRecord.getDistrictCode(), newRecord.getTalukaCode());

                    HealthBlock healthBlockRecord = healthBlockRecordsDataService.findHealthBlockByParentCode(
                            newRecord.getStateCode(), newRecord.getDistrictCode(), newRecord.getTalukaCode(), newRecord.getHealthBlockCode());

                    HealthFacility healthFacilityRecord = healthFacilityRecordsDataService.findHealthFacilityByParentCode(
                            newRecord.getStateCode(), newRecord.getDistrictCode(),
                            newRecord.getTalukaCode(), newRecord.getHealthBlockCode(),
                            newRecord.getHealthFacilityCode());

                    if (stateRecord != null && districtRecord != null && talukaRecord != null && healthBlockRecord != null) {
                        insertHealthSubFacilityData(newRecord);
                        result.incrementSuccessCount();
                        healthSubFacilityCsvRecordsDataService.delete(healthSubFacilityCsvRecord);
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
                errorDetails.setRecordDetails(healthSubFacilityCsvRecord.toString());
                errorDetails.setErrorCategory(dataValidationException.getErrorCode());
                errorDetails.setErrorDescription(dataValidationException.getErroneousField());
                bulkUploadErrLogService.writeBulkUploadErrLog(logFileName, errorDetails);
                healthSubFacilityCsvRecordsDataService.delete(healthSubFacilityCsvRecord);
            } catch (Exception e) {
                failedRecordCount++;
            }
        }
        bulkUploadErrLogService.writeBulkUploadProcessingSummary("userName", csvFileName, logFileName, result);
    }

    @MotechListener(subjects = {"mds.crud.masterdata.HealthSubFacilityCsv.csv-import.failed"})
    public void healthSubFacilityCsvFailed(MotechEvent event) {

        healthSubFacilityCsvRecordsDataService.deleteAll();

        logger.info("HealthSubFacility successfully deleted from temporary tables");
    }

    private HealthSubFacility mapHealthSubFacilityCsv(HealthSubFacilityCsv record) throws DataValidationException {
        HealthSubFacility newRecord = new HealthSubFacility();

        String healthSubFacilityName = ParseDataHelper.parseString("HealthSubFacilityName", record.getName(), true);
        Long stateCode = ParseDataHelper.parseLong("StateCode", record.getStateCode(), true);
        Long districtCode = ParseDataHelper.parseLong("DistrictCode", record.getDistrictCode(), true);
        String talukaCode = ParseDataHelper.parseString("TalukaCode", record.getTalukaCode(), true);
        Long healthBlockCode = ParseDataHelper.parseLong("HealthBlockCode", record.getHealthBlockCode(), true);
        Long healthfacilityCode = ParseDataHelper.parseLong("HealthFacilityCode", record.getHealthFacilityCode(), true);
        Long healthSubFacilityCode = ParseDataHelper.parseLong("HealthSubFacilityCode", record.getHealthSubFacilityCode(), true);


        newRecord.setName(healthSubFacilityName);
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setTalukaCode(talukaCode);
        newRecord.setHealthBlockCode(healthBlockCode);
        newRecord.setHealthFacilityCode(healthfacilityCode);
        newRecord.setHealthSubFacilityCode(healthSubFacilityCode);

        return newRecord;
    }

    private void insertHealthSubFacilityData(HealthSubFacility healthSubFacilityData) {

        HealthSubFacility existHealthFacilityData = healthSubFacilityRecordsDataService.findHealthSubFacilityByParentCode(
                healthSubFacilityData.getStateCode(),
                healthSubFacilityData.getDistrictCode(),
                healthSubFacilityData.getTalukaCode(),
                healthSubFacilityData.getHealthBlockCode(),
                healthSubFacilityData.getHealthFacilityCode(),
                healthSubFacilityData.getHealthSubFacilityCode()
        );

        if (existHealthFacilityData != null) {

            healthSubFacilityRecordsDataService.update(healthSubFacilityData);
            logger.info("HealthSubFacility Permanent data is successfully updated.");
        } else {
            healthSubFacilityRecordsDataService.create(healthSubFacilityData);
            logger.info("HealthSubFacility Permanent data is successfully updated.");
        }
    }
}