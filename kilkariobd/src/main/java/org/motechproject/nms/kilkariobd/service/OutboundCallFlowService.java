package org.motechproject.nms.kilkariobd.service;

import org.joda.time.DateTime;
import org.motechproject.nms.kilkariobd.domain.CallFlowStatus;
import org.motechproject.nms.kilkariobd.domain.OutboundCallFlow;
import org.motechproject.nms.kilkariobd.dto.request.CdrNotificationRequest;
import org.motechproject.nms.kilkariobd.dto.request.FileProcessedStatusRequest;
import org.motechproject.nms.util.helper.DataValidationException;

/**
 * Interface to expose the CRUD operations for OutboundCallFlow Entity
 */
public interface OutboundCallFlowService {

    /**
     * Method to find OutboundCallFlow based on callFlow Status
     * @param status CallFlowStatus enum
     * @return OutboundCallFlow type object
     */
    public OutboundCallFlow findRecordByCallStatus(CallFlowStatus status);

    /**
     * Method to create record of type OutboundCallFlow
     * @param record OutboundCallFlow type object
     * @return OutboundCallFlow type object
     */
    public OutboundCallFlow create(OutboundCallFlow record);

    /**
     * Method to update OutboundCallFlow record
     * @param record OutboundCallFlow
     * @return OutboundCallFlow
     */
    public OutboundCallFlow update(OutboundCallFlow record);

    /**
     * Method to find OutboundCallFlow based on ObdFileName
     * @param filename ObdFileName
     * @return OutboundCallFlow type object
     */
    public OutboundCallFlow findRecordByFileName(String filename);

    /**
     * Method to find OutboundCallFlow based on creationDate
     * @param date
     * @return
     */
    public OutboundCallFlow findRecordByCreationDate(DateTime date);

     /**
     * Method to update OutboundCallFlow status based on CDR notification
     * @param cdrNotificationRequest Cdr notification object
     */
    public void handleCdrNotification(CdrNotificationRequest cdrNotificationRequest)
            throws DataValidationException;

    /**
     * Method to update OutboundCallFlow status based on OBD File processing status notification
     * @param fileProcessedStatusRequest Cdr notification object
     */
    public void handleFileProcessedStatusNotification(FileProcessedStatusRequest fileProcessedStatusRequest)
            throws DataValidationException;

    /**
     * Method to update call floe status fetch by fileName
     * @param obdFileName parameter to get OutboundCallFlow records from the database.
     * @param updateTo CallFlowStatus to be updated
     * @return OutboundCallFlow
     */
    public OutboundCallFlow updateCallFlowStatus(String  obdFileName, CallFlowStatus updateTo);

    /**
     * Updates the checksum and record count for Obd CallFlow identified by obdFileName
     * @param obdFileName parameter to get OutboundCallFlow records from the database.
     * @param obdChecksum checksum of the file
     * @param obdRecordsCount total number od records
     * @return OutboundCallFlow
     */
    public OutboundCallFlow updateChecksumAndRecordCount(String obdFileName, String obdChecksum,
                                                         Long obdRecordsCount);
}
