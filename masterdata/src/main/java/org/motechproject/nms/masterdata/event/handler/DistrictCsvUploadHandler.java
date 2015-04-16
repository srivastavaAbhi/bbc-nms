package org.motechproject.nms.masterdata.event.handler;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.CsvDistrict;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.service.DistrictCsvService;
import org.motechproject.nms.masterdata.service.DistrictService;
import org.motechproject.nms.masterdata.service.StateService;
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
 * This class handles the csv upload for success and failure events for District Csv.
 */

@Component
public class DistrictCsvUploadHandler {

    private ValidatorService validatorService;

    private DistrictCsvService districtCsvService;

    private DistrictService districtService;

    private StateService stateService;

    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(DistrictCsvUploadHandler.class);

    @Autowired
    public DistrictCsvUploadHandler(ValidatorService validatorService, DistrictCsvService districtCsvService, DistrictService districtService, StateService stateService, BulkUploadErrLogService bulkUploadErrLogService) {
        this.validatorService = validatorService;
        this.districtCsvService = districtCsvService;
        this.districtService = districtService;
        this.stateService = stateService;
        this.bulkUploadErrLogService = bulkUploadErrLogService;
    }

    /**
     * This method handle the event which is raised after csv is uploaded successfully.
     * this method also populates the records in District table after checking its validity.
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = {LocationConstants.DISTRICT_CSV_SUCCESS})
    public void districtCsvSuccess(MotechEvent motechEvent) {

        logger.info("DISTRICT_CSV_SUCCESS event received");

        Map<String, Object> params = motechEvent.getParameters();
        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");
        String csvFileName = (String) params.get("csv-import.filename");
        logger.debug("Csv file name received in event : {}", csvFileName);

        processRecords(createdIds, csvFileName);
    }


    public void processRecords(List<Long> CreatedId,
                               String csvFileName) {
        logger.info("Record Processing Started for csv file: {}", csvFileName);

        districtService.getDistrictRecordsDataService()
                .doInTransaction(new TransactionCallback<District>() {

                    List<Long> districtCsvId;

                    String csvFileName;

                    private TransactionCallback<District> init(
                            List<Long> createdId,
                            String csvFileName) {
                        this.districtCsvId = createdId;
                        this.csvFileName = csvFileName;
                        return this;
                    }

                    @Override
                    public District doInTransaction(
                            TransactionStatus status) {
                        District transactionObject = null;
                        processDistrictCsvRecords(csvFileName, districtCsvId);
                        return transactionObject;
                    }
                }.init(CreatedId, csvFileName));
        logger.info("Record Processing complete for csv file: {}", csvFileName);
    }


    private void processDistrictCsvRecords(String csvFileName, List<Long> createdIds) {

        DateTime timeStamp = new DateTime();

        BulkUploadStatus bulkUploadStatus = new BulkUploadStatus();

        BulkUploadError errorDetails = new BulkUploadError();

        ErrorLog.setErrorDetails(errorDetails, bulkUploadStatus, csvFileName, timeStamp, RecordType.DISTRICT);

        CsvDistrict csvDistrictRecord = null;

        for (Long id : createdIds) {
            try {
                logger.debug("DISTRICT_CSV_SUCCESS event processing start for ID: {}", id);
                csvDistrictRecord = districtCsvService.findById(id);

                if (csvDistrictRecord != null) {
                    logger.info("Id exist in District Temporary Entity");
                    bulkUploadStatus.setUploadedBy(csvDistrictRecord.getOwner());
                    District record = mapDistrictCsv(csvDistrictRecord);
                    processDistrictData(record);
                    bulkUploadStatus.incrementSuccessCount();
                } else {
                    logger.info("Id do not exist in District Temporary Entity");
                    ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION, ErrorCategoryConstants.CSV_RECORD_MISSING, "Record is null");

                }
            } catch (DataValidationException stateDataException) {
                logger.error("DISTRICT_CSV_SUCCESS processing receive DataValidationException exception due to error field: {}", stateDataException.getErroneousField());

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, stateDataException.getErroneousField(), stateDataException.getErrorCode(), csvDistrictRecord.toString());
            } catch (Exception stateException) {

                ErrorLog.errorLog(errorDetails, bulkUploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.GENERAL_EXCEPTION_DESCRIPTION, ErrorCategoryConstants.GENERAL_EXCEPTION, "Exception occurred");
                logger.error("DISTRICT_CSV_SUCCESS processing receive Exception exception, message: {}", stateException);
            } finally {
                if (null != csvDistrictRecord) {
                    districtCsvService.delete(csvDistrictRecord);
                }
            }
        }
        bulkUploadErrLogService.writeBulkUploadProcessingSummary(bulkUploadStatus);

    }

    private District mapDistrictCsv(CsvDistrict record) throws DataValidationException {

        String districtName = ParseDataHelper.validateAndParseString("District Name", record.getName(), true);
        Long stateCode = ParseDataHelper.validateAndParseLong("StateCode", record.getStateCode(), true);
        Long districtCode = ParseDataHelper.validateAndParseLong("DistrictCode", record.getDistrictCode(), true);

        validateDistrictParent(stateCode);

        District newRecord = new District();

        newRecord.setName(districtName);
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setCreator(record.getCreator());
        newRecord.setOwner(record.getOwner());
        newRecord.setModifiedBy(record.getModifiedBy());

        return newRecord;
    }

    private void validateDistrictParent(Long stateCode) throws DataValidationException {
        validatorService.validateDistrictParent(stateCode);
    }

    private void processDistrictData(District districtData) throws DataValidationException {

        logger.debug("District data contains district code : {}", districtData.getDistrictCode());
        District existDistrictData = districtService.findDistrictByParentCode(districtData.getDistrictCode(), districtData.getStateCode());
        if (null != existDistrictData) {
            updateDistrict(existDistrictData, districtData);
        } else {
            insertDistrict(districtData);
        }
    }

    private void insertDistrict(District districtData) {
        State stateData = stateService.findRecordByStateCode(districtData.getStateCode());
        stateData.getDistrict().add(districtData);
        stateService.update(stateData);
        logger.info("District data is successfully inserted.");
    }

    private void updateDistrict(District existDistrictData, District districtData) {
        existDistrictData.setName(districtData.getName());
        existDistrictData.setModifiedBy(districtData.getModifiedBy());
        districtService.update(existDistrictData);
        logger.info("District data is successfully updated.");
    }
}