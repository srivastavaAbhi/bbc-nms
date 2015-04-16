package org.motechproject.nms.masterdata.event.handler;

/**
 * This class handles the csv upload for success and failure events for Health SubFacility Csv.
 */

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.CsvHealthSubFacility;
import org.motechproject.nms.masterdata.domain.HealthFacility;
import org.motechproject.nms.masterdata.domain.HealthSubFacility;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.service.HealthFacilityService;
import org.motechproject.nms.masterdata.service.HealthSubFacilityCsvService;
import org.motechproject.nms.masterdata.service.HealthSubFacilityService;
import org.motechproject.nms.masterdata.service.ValidatorService;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.constants.ErrorDescriptionConstants;
import org.motechproject.nms.util.domain.BulkUploadError;
import org.motechproject.nms.util.domain.BulkUploadStatus;
import org.motechproject.nms.util.domain.RecordType;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HealthSubFacilityCsvUploadHandler {

    private ValidatorService validatorService;

    private HealthFacilityService healthFacilityService;

    private HealthSubFacilityCsvService healthSubFacilityCsvService;

    private HealthSubFacilityService healthSubFacilityService;

    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(HealthSubFacilityCsvUploadHandler.class);


    @Autowired
    public HealthSubFacilityCsvUploadHandler(ValidatorService validatorService, HealthFacilityService healthFacilityService, HealthSubFacilityCsvService healthSubFacilityCsvService, HealthSubFacilityService healthSubFacilityService, BulkUploadErrLogService bulkUploadErrLogService) {
        this.validatorService = validatorService;
        this.healthFacilityService = healthFacilityService;
        this.healthSubFacilityCsvService = healthSubFacilityCsvService;
        this.healthSubFacilityService = healthSubFacilityService;
        this.bulkUploadErrLogService = bulkUploadErrLogService;
    }

    /**
     * This method handle the event which is raised after csv is uploaded successfully.
     * this method also populates the records in HealthSubFacility table after checking its validity.
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = {LocationConstants.HEALTH_SUB_FACILITY_CSV_SUCCESS})
    public void healthSubFacilityCsvSuccess(MotechEvent motechEvent) {

        logger.info("HEALTH_SUB_FACILITY_CSV_SUCCESS event received");

        Map<String, Object> params = motechEvent.getParameters();

        String csvFileName = (String) params.get("csv-import.filename");
        logger.debug("Csv file name received in event : {}", csvFileName);
        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");

        processRecords(createdIds, csvFileName);
    }


    private void processRecords(List<Long> CreatedId,
                               String csvFileName) {
        logger.info("Record Processing Started for csv file: {}", csvFileName);

        healthSubFacilityService.getHealthSubFacilityRecordsDataService().doInTransaction(
                new TransactionCallback<State>() {

                    List<Long> healthSubFacilityCsvId;

                    String csvFileName;

                    private TransactionCallback<State> init(
                            List<Long> createdId,
                            String csvFileName) {
                        this.healthSubFacilityCsvId = createdId;
                        this.csvFileName = csvFileName;
                        return this;
                    }

                    @Override
                    public State doInTransaction(
                            TransactionStatus status) {
                        State transactionObject = null;
                        processHealthSubFacilityRecords(csvFileName, healthSubFacilityCsvId);
                        return transactionObject;
                    }
                }.init(CreatedId, csvFileName));
        logger.info("Record Processing complete for csv file: {}", csvFileName);
    }


    private void processHealthSubFacilityRecords(String csvFileName, List<Long> createdIds) {
        DateTime timeStamp = new DateTime();

        BulkUploadStatus bulkUploadStatus = new BulkUploadStatus();

        BulkUploadError errorDetails = new BulkUploadError();

        ErrorLog.setErrorDetails(errorDetails, bulkUploadStatus, csvFileName, timeStamp, RecordType.HEALTH_SUB_FACILITY);


        CsvHealthSubFacility csvHealthSubFacilityRecord = null;

        for (Long id : createdIds) {
            try {
                logger.debug("HEALTH_SUB_FACILITY_CSV_SUCCESS event processing start for ID: {}", id);
                csvHealthSubFacilityRecord = healthSubFacilityCsvService.findById(id);

                if (csvHealthSubFacilityRecord != null) {
                    logger.info("Id exist in Health Sub Facility Temporary Entity");
                    bulkUploadStatus.setUploadedBy(csvHealthSubFacilityRecord.getOwner());
                    HealthSubFacility record = mapHealthSubFacilityCsv(csvHealthSubFacilityRecord);
                    processHealthSubFacilityData(record);
                    bulkUploadStatus.incrementSuccessCount();
                } else {
                    logger.info("Id do not exist in Health Sub Facility Temporary Entity");

                    ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION, ErrorCategoryConstants.CSV_RECORD_MISSING, "Record is null");

                }
            } catch (DataValidationException healthSubFacilityDataException) {
                logger.error("HEALTH_SUB_FACILITY_CSV_SUCCESS processing receive DataValidationException exception due to error field: {}", healthSubFacilityDataException.getErroneousField());

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, healthSubFacilityDataException.getErroneousField(), healthSubFacilityDataException.getErrorCode(), csvHealthSubFacilityRecord.toString());

            } catch (Exception healthSubFacilityException) {

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.GENERAL_EXCEPTION_DESCRIPTION, ErrorCategoryConstants.GENERAL_EXCEPTION, "Exception occurred");

                logger.error("HEALTH_SUB_FACILITY_CSV_SUCCESS processing receive Exception exception, message: {}", healthSubFacilityException);
            } finally {
                if (null != csvHealthSubFacilityRecord) {
                    healthSubFacilityCsvService.delete(csvHealthSubFacilityRecord);
                }
            }
        }
        bulkUploadErrLogService.writeBulkUploadProcessingSummary(bulkUploadStatus);
    }

    private HealthSubFacility mapHealthSubFacilityCsv(CsvHealthSubFacility record) throws DataValidationException {
        HealthSubFacility newRecord = new HealthSubFacility();

        String healthSubFacilityName = ParseDataHelper.validateAndParseString("HealthSubFacilityName", record.getName(), true);
        Long stateCode = ParseDataHelper.validateAndParseLong("StateCode", record.getStateCode(), true);
        Long districtCode = ParseDataHelper.validateAndParseLong("DistrictCode", record.getDistrictCode(), true);
        Long talukaCode = ParseDataHelper.validateAndParseLong("TalukaCode", record.getTalukaCode(), true);
        Long healthBlockCode = ParseDataHelper.validateAndParseLong("HealthBlockCode", record.getHealthBlockCode(), true);
        Long healthfacilityCode = ParseDataHelper.validateAndParseLong("HealthFacilityCode", record.getHealthFacilityCode(), true);
        Long healthSubFacilityCode = ParseDataHelper.validateAndParseLong("HealthSubFacilityCode", record.getHealthSubFacilityCode(), true);

        validateHealthSubFacilityParent(stateCode, districtCode, talukaCode, healthBlockCode, healthfacilityCode);

        newRecord.setName(healthSubFacilityName);
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setTalukaCode(talukaCode);
        newRecord.setHealthBlockCode(healthBlockCode);
        newRecord.setHealthFacilityCode(healthfacilityCode);
        newRecord.setHealthSubFacilityCode(healthSubFacilityCode);
        newRecord.setCreator(record.getCreator());
        newRecord.setOwner(record.getOwner());
        newRecord.setModifiedBy(record.getModifiedBy());

        return newRecord;
    }

    private void processHealthSubFacilityData(HealthSubFacility healthSubFacilityData) throws DataValidationException {

        logger.debug("Health Sub Facility data contains Sub Facility code : {}", healthSubFacilityData.getHealthSubFacilityCode());

        HealthSubFacility existHealthSubFacilityData = healthSubFacilityService.findHealthSubFacilityByParentCode(
                healthSubFacilityData.getStateCode(),
                healthSubFacilityData.getDistrictCode(),
                healthSubFacilityData.getTalukaCode(),
                healthSubFacilityData.getHealthBlockCode(),
                healthSubFacilityData.getHealthFacilityCode(),
                healthSubFacilityData.getHealthSubFacilityCode()
        );

        if (existHealthSubFacilityData != null) {
            updateHealthSubFacilityData(existHealthSubFacilityData, healthSubFacilityData);
        } else {
            insertHealthSubFacility(healthSubFacilityData);
        }
    }

    private void insertHealthSubFacility(HealthSubFacility healthSubFacilityData) {
        HealthFacility healthFacilityData = healthFacilityService.findHealthFacilityByParentCode(
                healthSubFacilityData.getStateCode(), healthSubFacilityData.getDistrictCode(),
                healthSubFacilityData.getTalukaCode(), healthSubFacilityData.getHealthBlockCode(),
                healthSubFacilityData.getHealthFacilityCode());

        healthFacilityData.getHealthSubFacility().add(healthSubFacilityData);
        healthFacilityService.update(healthFacilityData);
        logger.info("HealthSubFacility Permanent data is successfully inserted.");
    }

    private void updateHealthSubFacilityData(HealthSubFacility existHealthSubFacilityData, HealthSubFacility healthSubFacilityData) {
        existHealthSubFacilityData.setName(healthSubFacilityData.getName());
        existHealthSubFacilityData.setModifiedBy(healthSubFacilityData.getModifiedBy());
        healthSubFacilityService.update(existHealthSubFacilityData);
        logger.info("HealthSubFacility Permanent data is successfully updated.");
    }

    private void validateHealthSubFacilityParent(Long stateCode, Long districtCode, Long talukaCode, Long healthBlockCode, Long healthfacilityCode) throws DataValidationException {

        validatorService.validateHealthSubFacilityParent(stateCode, districtCode, talukaCode, healthBlockCode, healthfacilityCode);
    }
}
