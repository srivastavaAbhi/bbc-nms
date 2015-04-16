package org.motechproject.nms.masterdata.event.handler;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.constants.LocationConstants;
import org.motechproject.nms.masterdata.domain.*;
import org.motechproject.nms.masterdata.service.CircleService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeServiceCsv;
import org.motechproject.nms.masterdata.service.LocationService;
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
 * This class handles the csv upload for success and failure events for Language LocationCode Csv.
 */
@Component
public class LanguageLocationCodeCsvHandler {

    private LanguageLocationCodeService languageLocationCodeService;

    private LanguageLocationCodeServiceCsv languageLocationCodeServiceCsv;

    private BulkUploadErrLogService bulkUploadErrLogService;

    private CircleService circleService;

    private LocationService locationService;

    private static Logger logger = LoggerFactory.getLogger(LanguageLocationCodeCsvHandler.class);

    @Autowired
    public LanguageLocationCodeCsvHandler(LanguageLocationCodeService languageLocationCodeService,
                                          LanguageLocationCodeServiceCsv languageLocationCodeServiceCsv,
                                          BulkUploadErrLogService bulkUploadErrLogService,
                                          CircleService circleService,
                                          LocationService locationService) {
        this.languageLocationCodeService = languageLocationCodeService;
        this.languageLocationCodeServiceCsv = languageLocationCodeServiceCsv;
        this.bulkUploadErrLogService = bulkUploadErrLogService;
        this.circleService = circleService;
        this.locationService = locationService;
    }

    /**
     * This method handle the event which is raised after csv is uploaded successfully.
     * this method also populates the records in LanguageLocationCode table after checking its validity.
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = LocationConstants.LANGUAGE_LOCATION_CODE_CSV_SUCCESS)
    public void languageLocationCodeCsvSuccess(MotechEvent motechEvent) {

        Map<String, Object> params = motechEvent.getParameters();
        logger.info("CIRCLE_CSV_SUCCESS event received");

        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");

        String csvFileName = (String) params.get("csv-import.filename");
        logger.debug("Csv file name received in event : {}", csvFileName);

        processRecords(createdIds, csvFileName);
    }


    public void processRecords(List<Long> CreatedId,
                               String csvFileName) {
        logger.info("Record Processing Started for csv file: {}", csvFileName);

        languageLocationCodeService.getLanguageLocationCodeDataService()
                .doInTransaction(new TransactionCallback<State>() {

                    List<Long> lLcCsvId;

                    String csvFileName;

                    private TransactionCallback<State> init(
                            List<Long> createdId,
                            String csvFileName) {
                        this.lLcCsvId = createdId;
                        this.csvFileName = csvFileName;
                        return this;
                    }

                    @Override
                    public State doInTransaction(
                            TransactionStatus status) {
                        State transactionObject = null;
                        processLanguageLocationRecords(csvFileName, lLcCsvId);
                        return transactionObject;
                    }
                }.init(CreatedId, csvFileName));
        logger.info("Record Processing complete for csv file: {}", csvFileName);
    }


    private void processLanguageLocationRecords(String csvFileName, List<Long> createdIds) {
        CsvLanguageLocationCode record = null;
        DateTime timeStamp = new DateTime();
        BulkUploadError errorDetail = new BulkUploadError();

        BulkUploadStatus uploadStatus = new BulkUploadStatus();

        ErrorLog.setErrorDetails(errorDetail, uploadStatus, csvFileName, timeStamp, RecordType.LANGUAGE_LOCATION_CODE);

        for (Long id : createdIds) {
            try {
                record = languageLocationCodeServiceCsv.getRecord(id);
                if (record != null) {
                    uploadStatus.setUploadedBy(record.getOwner());
                    LanguageLocationCode newRecord = mapLanguageLocationCodeFrom(record);

                    LanguageLocationCode oldRecord = languageLocationCodeService.getRecordByLocationCode(
                            newRecord.getStateCode(), newRecord.getDistrictCode());
                    if (oldRecord != null) {
                        oldRecord = copyLanguageLocationCodeForUpdate(newRecord, oldRecord);
                        languageLocationCodeService.update(oldRecord);
                        logger.info("Record updated successfully for statecode : {} and districtcode : {}", newRecord.getStateCode(), newRecord.getDistrictCode());

                    } else {
                        languageLocationCodeService.create(newRecord);
                        logger.info("Record created successfully for statecode : {} and districtcode : {}", newRecord.getStateCode(), newRecord.getDistrictCode());

                    }
                    uploadStatus.incrementSuccessCount();
                } else {
                    logger.error("Record not found in the LanguageLocationCodeCsv table with id {}", id);
                    ErrorLog.errorLog(errorDetail, uploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION, ErrorCategoryConstants.CSV_RECORD_MISSING, "Record is null");

                }
            } catch (DataValidationException languageLocationDataException) {

                ErrorLog.errorLog(errorDetail, uploadStatus, bulkUploadErrLogService, languageLocationDataException.getErrorDesc(), languageLocationDataException.getErrorCode(), record.toString());

            } catch (Exception languageLocationException) {
                logger.error("LANGUAGE_LOCATION_CSV_SUCCESS processing receive Exception exception, message: {}", languageLocationException);

                ErrorLog.errorLog(errorDetail, uploadStatus, bulkUploadErrLogService, ErrorDescriptionConstants.GENERAL_EXCEPTION_DESCRIPTION, ErrorCategoryConstants.GENERAL_EXCEPTION, "Some Error Occurred");

            } finally {
                if (null != record) {
                    languageLocationCodeServiceCsv.delete(record);
                }
            }
        }

        bulkUploadErrLogService.writeBulkUploadProcessingSummary(uploadStatus);
        logger.info("Finished processing LanguageLocationCodeCsv-import success");
    }

    /**
     * This method is used to validate csv uploaded record
     * and map LanguageLocationCodeCsv to LanguageLocationCode
     *
     * @param record of LanguageLocationCodeCsv type
     * @return Operator record after the mapping
     * @throws DataValidationException
     */
    private LanguageLocationCode mapLanguageLocationCodeFrom(CsvLanguageLocationCode record)
            throws DataValidationException {

        LanguageLocationCode newRecord = null;

        Long stateCode = ParseDataHelper.validateAndParseLong("StateCode", record.getStateCode(), true);
        Long districtCode = ParseDataHelper.validateAndParseLong("DistrictCode", record.getDistrictCode(), true);
        String circleCode = ParseDataHelper.validateAndParseString("CircleCode", record.getCircleCode(), true);

        State state = locationService.getStateByCode(stateCode);
        if (state == null) {
            ParseDataHelper.raiseInvalidDataException("stateCode", record.getStateCode());
        }

        District district = locationService.getDistrictByCode(state.getId(), districtCode);
        if (district == null) {
            ParseDataHelper.raiseInvalidDataException("districtCode", record.getDistrictCode());
        }

        Circle circle = circleService.getRecordByCode(circleCode);
        if (circle == null) {
            ParseDataHelper.raiseInvalidDataException("circleCode", record.getCircleCode());
        }


        newRecord = new LanguageLocationCode();

        /* Fill newRecord with values from CSV */
        newRecord.setStateCode(stateCode);
        newRecord.setDistrictCode(districtCode);
        newRecord.setCircleCode(circleCode);

        newRecord.setCircle(circle);
        newRecord.setState(state);
        newRecord.setDistrict(district);
        newRecord.setCreator(record.getCreator());
        newRecord.setOwner(record.getOwner());
        newRecord.setModifiedBy(record.getModifiedBy());

        newRecord.setLanguageLocationCode(ParseDataHelper.validateAndParseInt("LanguageLocationCode",
                record.getLanguageLocationCode(), true));
        newRecord.setLanguageKK(ParseDataHelper.validateAndParseString("LanguageKK", record.getLanguageKK(), true));
        newRecord.setLanguageMK(ParseDataHelper.validateAndParseString("LanguageMK", record.getLanguageMK(), true));
        newRecord.setLanguageMA(ParseDataHelper.validateAndParseString("LanguageMA", record.getLanguageMA(), true));


        /* Update the Default Language Location Codes in Circle entity */

        Boolean isDefLangLocCode = ParseDataHelper.validateAndParseBoolean("isDefaultLanguageLocationCode",
                record.getIsDefaultLanguageLocationCode(), true);

        if (isDefLangLocCode) {
            circle.setDefaultLanguageLocationCode(newRecord.getLanguageLocationCode());
            circleService.update(circle);
        }

        return newRecord;
    }

    /**
     * Copies the field values from new Record to oldRecord for update in DB
     *
     * @param newRecord mapped from CSV values
     * @param oldRecord to be updated in DB
     * @return oldRecord after copied values
     */
    private LanguageLocationCode copyLanguageLocationCodeForUpdate(LanguageLocationCode newRecord,
                                                                   LanguageLocationCode oldRecord) {

        oldRecord.setStateCode(newRecord.getStateCode());
        oldRecord.setDistrictCode(newRecord.getDistrictCode());
        oldRecord.setCircleCode(newRecord.getCircleCode());

        oldRecord.setCircle(newRecord.getCircle());
        oldRecord.setState(newRecord.getState());
        oldRecord.setDistrict(newRecord.getDistrict());
        oldRecord.setModifiedBy(newRecord.getModifiedBy());

        oldRecord.setLanguageLocationCode(newRecord.getLanguageLocationCode());

        oldRecord.setLanguageMA(newRecord.getLanguageMA());
        oldRecord.setLanguageMK(newRecord.getLanguageMK());
        oldRecord.setLanguageKK(newRecord.getLanguageKK());

        return oldRecord;
    }

}

