package org.motechproject.nms.masterdata.event.handler;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.CsvHealthBlock;
import org.motechproject.nms.masterdata.domain.HealthBlock;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.domain.Taluka;
import org.motechproject.nms.masterdata.service.HealthBlockCsvService;
import org.motechproject.nms.masterdata.service.HealthBlockService;
import org.motechproject.nms.masterdata.service.TalukaService;
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

/**
 * This class handles the csv upload for success and failure events for Health Block Csv.
 */
@Component
public class HealthBlockCsvUploadHandler {

    private ValidatorService validatorService;

    private TalukaService talukaService;

    private HealthBlockCsvService healthBlockCsvService;

    private HealthBlockService healthBlockService;

    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(HealthBlockCsvUploadHandler.class);

    @Autowired
    public HealthBlockCsvUploadHandler(ValidatorService validatorService, TalukaService talukaService, HealthBlockCsvService healthBlockCsvService, HealthBlockService healthBlockService, BulkUploadErrLogService bulkUploadErrLogService) {
        this.validatorService = validatorService;
        this.talukaService = talukaService;
        this.healthBlockCsvService = healthBlockCsvService;
        this.healthBlockService = healthBlockService;
        this.bulkUploadErrLogService = bulkUploadErrLogService;
    }

    /**
     * This method handle the event which is raised after csv is uploaded successfully.
     * this method also populates the records in HealthBlock table after checking its validity.
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = {LocationConstants.HEALTH_BLOCK_CSV_SUCCESS})
    public void healthBlockCsvSuccess(MotechEvent motechEvent) {

        logger.info("HEALTH_BLOCK_CSV_SUCCESS event received");

        Map<String, Object> params = motechEvent.getParameters();

        String csvFileName = (String) params.get("csv-import.filename");

        logger.debug("Csv file name received in event : {}", csvFileName);

        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");

        processRecords(createdIds, csvFileName);

    }


    private void processRecords(List<Long> CreatedId,
                               String csvFileName) {
        logger.info("Record Processing Started for csv file: {}", csvFileName);

        healthBlockService.getHealthBlockRecordsDataService()
                .doInTransaction(new TransactionCallback<State>() {

                    List<Long> healthBlockCsvId;

                    String csvFileName;

                    private TransactionCallback<State> init(
                            List<Long> createdId,
                            String csvFileName) {
                        this.healthBlockCsvId = createdId;
                        this.csvFileName = csvFileName;
                        return this;
                    }

                    @Override
                    public State doInTransaction(
                            TransactionStatus status) {
                        State transactionObject = null;
                        processHealthBlockRecords(csvFileName, healthBlockCsvId);
                        return transactionObject;
                    }
                }.init(CreatedId, csvFileName));
        logger.info("Record Processing complete for csv file: {}", csvFileName);
    }


    private void processHealthBlockRecords(String csvFileName, List<Long> createdIds) {
        DateTime timeStamp = new DateTime();

        BulkUploadStatus bulkUploadStatus = new BulkUploadStatus();

        BulkUploadError errorDetails = new BulkUploadError();

        ErrorLog.setErrorDetails(errorDetails, bulkUploadStatus, csvFileName, timeStamp, RecordType.HEALTH_BLOCK);


        CsvHealthBlock csvHealthBlockRecord = null;

        for (Long id : createdIds) {
            try {
                logger.debug("HEALTH_BLOCK_CSV_SUCCESS event processing start for ID: {}", id);
                csvHealthBlockRecord = healthBlockCsvService.findById(id);

                if (csvHealthBlockRecord != null) {
                    logger.info("Id exist in HealthBlock Temporary Entity");
                    bulkUploadStatus.setUploadedBy(csvHealthBlockRecord.getOwner());
                    HealthBlock record = mapHealthBlockCsv(csvHealthBlockRecord);
                    processHealthBlockData(record);
                    bulkUploadStatus.incrementSuccessCount();
                } else {
                    logger.info("Id do not exist in HealthBlock Temporary Entity");
                    ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION, ErrorCategoryConstants.CSV_RECORD_MISSING, "Record is null");

                }
            } catch (DataValidationException healthBlockDataException) {
                logger.error("HEALTH_BLOCK_CSV_SUCCESS processing receive DataValidationException exception due to error field: {}", healthBlockDataException.getErroneousField());

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, healthBlockDataException.getErroneousField(), healthBlockDataException.getErrorCode(), csvHealthBlockRecord.toString());
            } catch (Exception healthBlockException) {

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.GENERAL_EXCEPTION_DESCRIPTION, ErrorCategoryConstants.GENERAL_EXCEPTION, "Exception occurred");

                logger.error("HEALTH_BLOCK_CSV_SUCCESS processing receive Exception exception, message: {}", healthBlockException);
            } finally {
                if (null != csvHealthBlockRecord) {
                    healthBlockCsvService.delete(csvHealthBlockRecord);
                }
            }
        }

        bulkUploadErrLogService.writeBulkUploadProcessingSummary(bulkUploadStatus);
    }


    private HealthBlock mapHealthBlockCsv(CsvHealthBlock record) throws DataValidationException {
        HealthBlock newRecord = new HealthBlock();

        String healthBlockName = ParseDataHelper.validateAndParseString("HealthBlockName", record.getName(), true);
        Long stateCode = ParseDataHelper.validateAndParseLong("StateCode", record.getStateCode(), true);
        Long districtCode = ParseDataHelper.validateAndParseLong("DistrictCode", record.getDistrictCode(), true);
        Long talukaCode = ParseDataHelper.validateAndParseLong("TalukaCode", record.getTalukaCode(), true);
        Long healthBlockCode = ParseDataHelper.validateAndParseLong("HealthBlockCode", record.getHealthBlockCode(), true);

        validatorService.validateHealthBlockParent(stateCode,districtCode,talukaCode);

        newRecord.setName(healthBlockName);
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setTalukaCode(talukaCode);
        newRecord.setHealthBlockCode(healthBlockCode);
        newRecord.setCreator(record.getCreator());
        newRecord.setOwner(record.getOwner());
        newRecord.setModifiedBy(record.getModifiedBy());

        return newRecord;
    }

    private void processHealthBlockData(HealthBlock healthBlockData) throws DataValidationException {

        logger.debug("Health Block data contains Health Block code : {}", healthBlockData.getHealthBlockCode());
        HealthBlock existHealthBlockData = healthBlockService.findHealthBlockByParentCode(
                healthBlockData.getStateCode(),
                healthBlockData.getDistrictCode(),
                healthBlockData.getTalukaCode(),
                healthBlockData.getHealthBlockCode());

        if (existHealthBlockData != null) {
            updateHealthBlock(existHealthBlockData, healthBlockData);
        } else {
            insertHealthBlock(healthBlockData);
        }
    }

    private void insertHealthBlock(HealthBlock healthBlockData) {
        Taluka talukaRecord = talukaService.findTalukaByParentCode(healthBlockData.getStateCode(), healthBlockData.getDistrictCode(), healthBlockData.getTalukaCode());
        talukaRecord.getHealthBlock().add(healthBlockData);
        talukaService.update(talukaRecord);
        logger.info("HealthBlock data is successfully inserted.");
    }

    private void updateHealthBlock(HealthBlock existHealthBlockData, HealthBlock healthBlockData) {
        existHealthBlockData.setName(healthBlockData.getName());
        existHealthBlockData.setModifiedBy(healthBlockData.getModifiedBy());
        healthBlockService.update(existHealthBlockData);
        logger.info("HealthBlock data is successfully updated.");
    }
}
