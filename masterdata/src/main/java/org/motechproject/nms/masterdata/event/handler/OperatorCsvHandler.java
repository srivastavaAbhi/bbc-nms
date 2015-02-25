package org.motechproject.nms.masterdata.event.handler;

import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.masterdata.constants.MasterDataConstants;
import org.motechproject.nms.masterdata.domain.OperationType;
import org.motechproject.nms.masterdata.domain.Operator;
import org.motechproject.nms.masterdata.domain.OperatorCsv;
import org.motechproject.nms.masterdata.service.OperatorCsvService;
import org.motechproject.nms.masterdata.service.OperatorService;
import org.motechproject.nms.util.BulkUploadError;
import org.motechproject.nms.util.CsvProcessingSummary;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.constants.ErrorDescriptionConstants;
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
 * This class handles the csv upload for success and failure events for OperatorCsv.
 */
@Component
public class OperatorCsvHandler {

    @Autowired
    private OperatorService operatorService;

    @Autowired
    private OperatorCsvService operatorCsvService;

    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;

    private static Logger logger = LoggerFactory.getLogger(OperatorCsvHandler.class);

    /**
     * This method handle the event which is raised after csv is uploaded successfully.
     * this method also populates the records in Operator table after checking its validity.
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = MasterDataConstants.OPERATOR_CSV_SUCCESS)
    public void operatorCsvSuccess(MotechEvent motechEvent) {

        logger.info("OPERATOR_CSV_SUCCESS event received");
        Map<String, Object> params = motechEvent.getParameters();

        OperatorCsv record = null;
        Operator persistentRecord = null;
        String userName = null;
        BulkUploadError errorDetail = new BulkUploadError();
        CsvProcessingSummary result = new CsvProcessingSummary();
        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");
        String csvFileName = (String) params.get("csv-import.filename");
        logger.debug("Csv file name received in event : {}", csvFileName);
        String errorFileName = BulkUploadError.createBulkUploadErrLogFileName(csvFileName);

        for (Long id : createdIds) {
            try {
                record = operatorCsvService.getRecord(id);
                if (record != null) {
                    userName = record.getOwner();
                    Operator newRecord = mapOperatorFrom(record);

                    persistentRecord = operatorService.getRecordByCode(newRecord.getCode());

                    if (persistentRecord != null) {
                        if (OperationType.DEL.toString().equals(record.getOperation())) {
                            operatorService.delete(persistentRecord);
                            logger.info("Record deleted successfully for operatorcode {}", newRecord.getCode());
                        } else {
                            newRecord.setId(persistentRecord.getId());
                            operatorService.update(newRecord);
                            logger.info("Record updated successfully for operatorcode {}", newRecord.getCode());
                        }
                    } else {
                        operatorService.create(newRecord);
                        logger.info("Record created successfully for operatorcode {}", newRecord.getCode());
                    }
                    result.incrementSuccessCount();
                } else {
                    logger.error("Record not found in the OperatorCsv table with id {}", id);
                    errorDetail.setErrorDescription(ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION);
                    errorDetail.setErrorCategory(ErrorCategoryConstants.CSV_RECORD_MISSING);
                    errorDetail.setRecordDetails("Record is null");
                    bulkUploadErrLogService.writeBulkUploadErrLog(errorFileName, errorDetail);
                    result.incrementFailureCount();
                }
            } catch (DataValidationException ex) {
                errorDetail.setErrorCategory(ex.getErrorCode());
                errorDetail.setRecordDetails(record.toString());
                errorDetail.setErrorDescription(ex.getErrorDesc());
                bulkUploadErrLogService.writeBulkUploadErrLog(errorFileName, errorDetail);
                result.incrementFailureCount();
            } catch (Exception e) {
                logger.error("OPERATOR_CSV_SUCCESS processing receive Exception exception, message: {}", e);
                errorDetail.setErrorCategory("");
                errorDetail.setRecordDetails("");
                errorDetail.setErrorDescription("");
                bulkUploadErrLogService.writeBulkUploadErrLog(errorFileName, errorDetail);
                result.incrementFailureCount();
            }
            finally {
                if(null != record){
                    operatorCsvService.delete(record);
                }
            }
        }

        bulkUploadErrLogService.writeBulkUploadProcessingSummary(userName, csvFileName, errorFileName, result);
        logger.info("Finished processing OperatorCsv-import success");
    }

    /**
     * This method handle the event which is raised after csv upload is failed.
     * This method also deletes all the csv records which get inserted in this upload..
     *
     * @param motechEvent This is the object from which required parameters are fetched.
     */
    @MotechListener(subjects = MasterDataConstants.OPERATOR_CSV_FAILED)
    public void operatorCsvFailure(MotechEvent motechEvent) {
        Map<String, Object> params = motechEvent.getParameters();
        logger.info("OPERATOR_CSV_FAILED event received");

        List<Long> createdIds = (ArrayList<Long>) params.get("csv-import.created_ids");

        for (Long id : createdIds) {
            OperatorCsv oldRecord = operatorCsvService.getRecord(id);
            if (oldRecord != null) {
                logger.debug("OPERATOR_CSV_FAILED event processing start for ID: {}", id);
                operatorCsvService.delete(oldRecord);
            }
        }
        logger.info("OPERATOR_CSV_FAILED event processing finished");
    }

    private Operator mapOperatorFrom(OperatorCsv record) throws DataValidationException {
        Operator newRecord = new Operator();
        newRecord.setName(ParseDataHelper.parseString("Name", record.getName(), true));
        newRecord.setCode(ParseDataHelper.parseString("Code", record.getCode(), true));
        newRecord.setCreator(record.getCreator());
        newRecord.setOwner(record.getOwner());
        newRecord.setModifiedBy(record.getModifiedBy());

        return newRecord;
    }
}

