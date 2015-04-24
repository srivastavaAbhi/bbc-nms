package org.motechproject.nms.kilkariobd.event.handler;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mds.service.impl.csv.CsvImporterExporter;
import org.motechproject.nms.kilkari.domain.DeactivationReason;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.service.ContentUploadService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.kilkariobd.helper.MD5Checksum;
import org.motechproject.nms.kilkariobd.helper.SecureCopy;
import org.motechproject.nms.kilkariobd.client.HttpClient;
import org.motechproject.nms.kilkariobd.client.ex.CDRFileProcessingFailedException;
import org.motechproject.nms.kilkariobd.commons.Constants;
import org.motechproject.nms.kilkariobd.domain.*;
import org.motechproject.nms.kilkariobd.mapper.ReadByCSVMapper;
import org.motechproject.nms.kilkariobd.service.ConfigurationService;
import org.motechproject.nms.kilkariobd.service.OutboundCallDetailService;
import org.motechproject.nms.kilkariobd.service.OutboundCallFlowService;
import org.motechproject.nms.kilkariobd.service.OutboundCallRequestService;
import org.motechproject.nms.kilkariobd.settings.Settings;
import org.motechproject.nms.masterdata.domain.LanguageLocationCode;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.motechproject.scheduler.contract.RunOnceSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.motechproject.nms.kilkariobd.commons.Constants.*;

/**
 * This class handle the events for prepare the target file and notify the target file to the IVR.
 */
public class OBDTargetFileHandler {

    @Autowired
    private OutboundCallRequestService requestService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LanguageLocationCodeService llcService;

    @Autowired
    private OutboundCallFlowService callFlowService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private OutboundCallDetailService callDetailService;

    @Autowired
    private CsvImporterExporter csvImporterExporter;

    @Autowired
    private ContentUploadService contentUploadService;

    @Autowired
    private SettingsFacade kilkariObdSettings;

    Logger logger = LoggerFactory.getLogger(OBDTargetFileHandler.class);

    /**
     * This method defines a daily event to be raised by scheduler to prepare target file.
     */
    @MotechListener(subjects = PREPARE_OBD_TARGET_EVENT_SUBJECT)
    public void prepareOBDTargetEventHandler() {

        HttpClient client = new HttpClient();

        /*
        delete all the records from outboundCallRequest before preparing fresh ObdTargetFile
         */
        requestService.deleteAll();
        String obdFileName = "OBD_NMS" + DateTime.now().toDateMidnight().toString();
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByFileName(obdFileName);
        if (todayCallFlow != null) {
            logger.error("Duplicate event received for Subject :" + PREPARE_OBD_TARGET_EVENT_SUBJECT);
            return;
        } else {
            /*
            create new record for OutboundCallFlow to track status of files processing for current day
            */
            OutboundCallFlow callFlow = new OutboundCallFlow();
            callFlow.setStatus(CallFlowStatus.OUTBOUND_FILE_PREPARATION_EVENT_RECEIVED);
            callFlow.setObdFileName(obdFileName);
            callFlowService.create(callFlow);

            prepareObdTargetFile(obdFileName, null, FIRST_ATTEMPT);
        }
    }

    @MotechListener(subjects = RETRY_PREPARE_OBD_TARGET_EVENT_SUBJECT)
    public void ObdRetryHandler(MotechEvent motechEvent) {
        Object cdrObdFile = motechEvent.getParameters().get(Constants.CURRENT_CDR_OBD_FILE);
        Object obdFile = motechEvent.getParameters().get(Constants.CURRENT_OBD_FILE);
        String obdFileName = null;
        String cdrObdFileName = null;
        if (obdFile != null) {
            obdFileName = obdFile.toString();
        }

        if (cdrObdFile != null) {
            cdrObdFileName = cdrObdFile.toString();
        }

        Integer retryNumber = (Integer)motechEvent.getParameters().get(Constants.OBD_PREPARATION_RETRY_NUMBER);
        prepareObdTargetFile(obdFileName, cdrObdFileName, retryNumber);
    }

    /* Daily event to be raised by scheduler to notify IVr of target file has been copied. */
    @MotechListener(subjects = NOTIFY_OBD_TARGET_EVENT_SUBJECT)
    public void copyAndNotifyOBDTargetFile() {
        Settings settings = new Settings(kilkariObdSettings);
        Configuration configuration = configurationService.getConfiguration();
        String obdFileName = "OBD_NMS" + DateTime.now().toDateMidnight().toString();
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByFileName(obdFileName);

        /* update the callFlow status with notify outbound file event received.*/
        updateCallFlowStatus(CallFlowStatus.CDR_FILES_PROCESSED,
                CallFlowStatus.NOTIFY_OUTBOUND_FILE_EVENT_RECEIVED);
        String localFileName = settings.getObdFileLocalPath() + obdFileName;
        String remoteFileName = configuration.getObdFilePathOnServer();
        Long recordsCount = todayCallFlow.getObdRecordCount();

        /* copy this target file to remote location. */
        SecureCopy.toRemote(localFileName, remoteFileName);

        /* notify IVR */
        HttpClient client = new HttpClient();
        client.notifyTargetFile(remoteFileName, todayCallFlow.getObdChecksum(), recordsCount);
        todayCallFlow = callFlowService.findRecordByCallStatus(CallFlowStatus.OUTBOUND_CALL_REQUEST_FILE_COPIED);
        todayCallFlow.setStatus(CallFlowStatus.OBD_FILE_NOTIFICATION_SENT_TO_IVR);
        callFlowService.update(todayCallFlow);
    }

    public void prepareObdTargetFile(String freshObdFile, String oldObdFileName, Integer retryNumber) {
        Configuration configuration  = configurationService.getConfiguration();
        Settings settings = new Settings();
        OutboundCallFlow oldCallFlow =  null;

        OutboundCallFlow todayCallStatus = callFlowService.findRecordByFileName(freshObdFile);
        CallFlowStatus callFlowStatus = todayCallStatus.getStatus();

        if( oldObdFileName == null) {
        /* etch the OutboundCallFlow record of previous day to update the status of CDR files processing */
        oldCallFlow = callFlowService.findRecordByCallStatus(CallFlowStatus.CDR_FILE_NOTIFICATION_RECEIVED); //todo: find order by createDate ?
        } else {
            oldCallFlow = callFlowService.findRecordByFileName(oldObdFileName);
        }

        if (oldCallFlow == null || oldCallFlow.getStatus().equals(CallFlowStatus.CDR_FILES_PROCESSING_FAILED)) {
                if (retryNumber < configuration.getMaxObdPreparationRetryCount()) {
                    retryPrepareOBDTargetFile(freshObdFile, oldObdFileName, ++retryNumber, configuration);
                    return;
                } else {
                    /* Process Fresh */
                    createFreshOBDRecords(configuration, settings);
                    return;
                }
        }

        try {
            switch (callFlowStatus) {
                case CDR_SUMMARY_PROCESSING_FAILED :
                case OUTBOUND_FILE_PREPARATION_EVENT_RECEIVED:
                    /* processCDRSummary */
                    downloadAndProcessCdrSummaryFile(oldCallFlow.getObdFileName(), configuration, settings);
                    processCdrDetail(oldCallFlow, configuration, settings);
                    createFreshOBDRecords(configuration, settings);
                    break;

                case CDR_DETAIL_PROCESSING_FAILED:
                    processCdrDetail(oldCallFlow, configuration, settings);
                    createFreshOBDRecords(configuration, settings);
                    break;

                default:
                    logger.error("Invalid State in OBD Call Flow");
                    break;

            }

        } catch (CDRFileProcessingFailedException cdrEx) {
            oldCallFlow.setStatus(CallFlowStatus.CDR_FILES_PROCESSING_FAILED);
            callFlowService.update(oldCallFlow);
            /* repeat Event */
            if (retryNumber < configuration.getMaxObdPreparationRetryCount()) {
                retryPrepareOBDTargetFile(freshObdFile, oldCallFlow.getObdFileName(), ++retryNumber, configuration);
            } else {
                /* Process Fresh */
                createFreshOBDRecords(configuration, settings);
            }
        }
    }

    private void processCdrDetail(OutboundCallFlow oldCallFlow, Configuration configuration, Settings settings)
            throws CDRFileProcessingFailedException{
        HttpClient client = new HttpClient();

        /* ProcessCDRDetail */
        downloadAndProcessCdrDetailFile(oldCallFlow.getObdFileName(), configuration, settings);
        oldCallFlow.setStatus(CallFlowStatus.CDR_FILES_PROCESSED);
        callFlowService.update(oldCallFlow);

        /* Notify IVR of file processing finished successfully */
        client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_PROCESSED_SUCCESSFULLY, oldCallFlow.getObdFileName());
    }

    private void downloadAndProcessCdrSummaryFile(String obdFileName, Configuration configuration, Settings settings) throws CDRFileProcessingFailedException {
        HttpClient client = new HttpClient();
        String cdrSummaryFileName = settings.getObdFileLocalPath() + "/Cdr_Summary_" + obdFileName;
        String remoteFileName = configuration.getObdFilePathOnServer() + "/Cdr_Summary_" + obdFileName;
        try {
            /* Copy cdrSummaryFile from remote location to local */
            SecureCopy.fromRemote(obdFileName, remoteFileName);
            processCDRSummaryCSV(cdrSummaryFileName, configuration);
        } catch (FileNotFoundException fex) {
            client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_NOT_ACCESSIBLE, cdrSummaryFileName);
            DateTime date = DateTime.now().withTimeAtStartOfDay();
            OutboundCallFlow todayCallFlow = callFlowService.findRecordByCreationDate(date);
            todayCallFlow.setStatus(CallFlowStatus.CDR_SUMMARY_PROCESSING_FAILED);
            callFlowService.update(todayCallFlow);
            throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_NOT_ACCESSIBLE);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    @Transactional
    private void processCDRSummaryCSV(String cdrFileName, Configuration configuration) throws CDRFileProcessingFailedException {
        HttpClient client = new HttpClient();
        List<Map<String, Object>> cdrSummaryRecords;
        Integer retryDayNumber;
        //todo apply null checks where ever possible.
        DateTime date = DateTime.now().withTimeAtStartOfDay();
        OutboundCallFlow oldCallFlow = callFlowService.findRecordByFileName(cdrFileName);
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByCreationDate(date);
        try {
            cdrSummaryRecords = ReadByCSVMapper.readWithCsvMapReader(cdrFileName);
            /*
            send error if cdr summary processing has errors for invalid records count
             */
            if (oldCallFlow != null && oldCallFlow.getCdrSummaryRecordCount() != cdrSummaryRecords.size()) {
                client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_RECORDCOUNT_ERROR, cdrFileName);
                todayCallFlow.setStatus(CallFlowStatus.CDR_SUMMARY_PROCESSING_FAILED);
                callFlowService.update(todayCallFlow);
                throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_RECORDCOUNT_ERROR);
            }

            /*
            send error if cdr summary processing has errors for invalid checksum.
             */
            if (oldCallFlow != null && oldCallFlow.getCdrSummaryChecksum().equals(MD5Checksum.findChecksum(cdrFileName))) {
                client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_CHECKSUM_ERROR, cdrFileName);
                todayCallFlow.setStatus(CallFlowStatus.CDR_SUMMARY_PROCESSING_FAILED);
                callFlowService.update(todayCallFlow);
                throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_CHECKSUM_ERROR);
            }

            /*
            read and parse CDRSummary CSV and create entry in OutboundCallRequest table for each record.
             */
            for (Map<String, Object> map : cdrSummaryRecords) {
                CallStatus finalStatus = (CallStatus) map.get(Constants.FINAL_STATUS);
                ObdStatusCode statusCode = (ObdStatusCode) map.get(Constants.STATUS_CODE);
                Long subscriptionId = ParseDataHelper.validateAndParseLong(Constants.REQUEST_ID, map.get(Constants.REQUEST_ID).toString(), true);
                String weekId = map.get(Constants.WEEK_ID).toString();
                String[] weekNoMsgNo = weekId.split("_");
                retryDayNumber = getRetryDayNumber(subscriptionId);
                Integer weekNumber = Integer.parseInt(weekNoMsgNo[0]);

                if (isValidForRetry(finalStatus, statusCode, subscriptionId, weekNumber)) {

                    OutboundCallRequest callRequestRetry = new OutboundCallRequest();
                    callRequestRetry.setRequestId(map.get(Constants.REQUEST_ID).toString());

                    callRequestRetry.setMsisdn(map.get(Constants.MSISDN).toString());

                    callRequestRetry.setLanguageLocationCode((Integer) map.get(Constants.LANGUAGE_LOCATION_CODE));
                    callRequestRetry.setCircleCode(map.get(Constants.CIRCLE).toString());

                    callRequestRetry.setContentFileName(map.get(Constants.CONTENT_FILE_NAME).toString());
                    callRequestRetry.setCli(map.get(Constants.CLI).toString());
                    callRequestRetry.setCallFlowURL(map.get(Constants.CALL_FLOW_URL).toString());
                    if (retryDayNumber.equals(Constants.RETRY_DAY_ONE)) {
                        callRequestRetry.setPriority(configuration.getRetryDay1ObdPriority());
                        callRequestRetry.setServiceId(configuration.getRetryDay1ObdServiceId());
                    } else if(retryDayNumber.equals(Constants.RETRY_DAY_TWO)) {
                        callRequestRetry.setPriority(configuration.getRetryDay2ObdPriority());
                        callRequestRetry.setServiceId(configuration.getRetryDay2ObdServiceId());
                    } else {
                        callRequestRetry.setPriority(configuration.getRetryDay3ObdPriority());
                        callRequestRetry.setServiceId(configuration.getRetryDay3ObdServiceId());
                    }

                    callRequestRetry.setWeekId(map.get(Constants.WEEK_ID).toString());
                    requestService.create(callRequestRetry);
                } else {
                    markCompleteForSuccessRecords(finalStatus, weekNumber, subscriptionId);
                }

                /*
                If for any cdrSummary record callStatus is FAILED and status code is either OBD_FAILED_INVALIDNUMBER
                or OBD_DNIS_IN_DND then deactivate the subscription for that record.
                 */
                if (finalStatus.equals(CallStatus.FAILED) && statusCode.equals(ObdStatusCode.OBD_FAILED_INVALIDNUMBER)) {
                    subscriptionService.deactivateSubscription(
                            (Long) map.get(Constants.REQUEST_ID), DeactivationReason.INVALID_MSISDN);
                } else if (finalStatus.equals(CallStatus.FAILED) && statusCode.equals(ObdStatusCode.OBD_DNIS_IN_DND)) {
                    subscriptionService.deactivateSubscription(
                            (Long) map.get(Constants.REQUEST_ID), DeactivationReason.MSISDN_IN_DND);
                }
                /*
                if for any cdrSummary record final status is Failed and statusCode is OBD_FAILED_NOATTEMPT then
                create a record in OutboundCallDetail table.
                 */
                if (finalStatus.equals(CallStatus.FAILED) && statusCode.equals(ObdStatusCode.OBD_FAILED_NOATTEMPT)) {
                    OutboundCallDetail record = new OutboundCallDetail();
                    record.setWeekId(map.get(Constants.WEEK_ID).toString());
                    record.setPriority((Integer) map.get(Constants.PRIORITY));
                    record.setContentFile(map.get(Constants.CONTENT_FILE_NAME).toString());
                    record.setCircleCode(map.get(Constants.CIRCLE).toString());
                    record.setCallStatus(((CallStatus) map.get(Constants.FINAL_STATUS)).ordinal());
                    record.setLanguageLocationCode((Integer) map.get(Constants.LANGUAGE_LOCATION_CODE));
                    record.setMsisdn(map.get(Constants.MSISDN).toString());
                    record.setRequestId(map.get(Constants.REQUEST_ID).toString());
                    record.setCallStartTime(null);
                    record.setCallAnswerTime(null);
                    record.setCallEndTime(null);
                    callDetailService.create(record);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

    }

    private void downloadAndProcessCdrDetailFile(String fileName, Configuration configuration, Settings settings) throws CDRFileProcessingFailedException {
        HttpClient client = new HttpClient();
        String cdrDetailFileName = settings.getObdFileLocalPath() + "/CDR_detail_" + fileName;
        String remoteFileName = configuration.getObdFilePathOnServer() + "/CDR_detail_" + fileName;
        try {
            /*
            Copy cdrDetailFile from remote location to local
             */
            SecureCopy.fromRemote(settings.getObdFileLocalPath(), remoteFileName);
            processCDRDetail(cdrDetailFileName);
        } catch (FileNotFoundException fex) {
            client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_NOT_ACCESSIBLE, cdrDetailFileName);
            DateTime date = DateTime.now().withTimeAtStartOfDay();
            OutboundCallFlow todayCallFlow = callFlowService.findRecordByCreationDate(date);
            todayCallFlow.setStatus(CallFlowStatus.CDR_DETAIL_PROCESSING_FAILED);
            callFlowService.update(todayCallFlow);
            throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_NOT_ACCESSIBLE);
        } catch (IOException ex) {
            logger.error((ex.getMessage()));
        }
    }

    @Transactional
    private void processCDRDetail(String cdrFileName) {
        HttpClient client = new HttpClient();
        List<Map<String, Object>> cdrDetailRecords;
        DateTime date = DateTime.now().withTimeAtStartOfDay();
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByCreationDate(date);
        OutboundCallFlow oldCallFlow = callFlowService.findRecordByFileName(cdrFileName);

        try {
            cdrDetailRecords = ReadByCSVMapper.readWithCsvMapReader(cdrFileName);
            /*
            send error if cdr summary processing has errors.
             */
            if (oldCallFlow.getCdrDetailRecordCount() != cdrDetailRecords.size()) {
                client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_RECORDCOUNT_ERROR, cdrFileName);
                todayCallFlow.setStatus(CallFlowStatus.CDR_DETAIL_PROCESSING_FAILED);
                callFlowService.update(todayCallFlow);
                throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_RECORDCOUNT_ERROR);
            }

            /*
            send error if cdr summary processing has errors.
             */
            if (oldCallFlow.getCdrDetailChecksum().equals(MD5Checksum.findChecksum(cdrFileName))) {
                client.notifyCDRFileProcessedStatus(FileProcessingStatus.FILE_CHECKSUM_ERROR, cdrFileName);
                todayCallFlow.setStatus(CallFlowStatus.CDR_DETAIL_PROCESSING_FAILED);
                callFlowService.update(todayCallFlow);
                throw new CDRFileProcessingFailedException(FileProcessingStatus.FILE_CHECKSUM_ERROR);
            }

            /*
            read and parse CDRDetail CSV and create entry in CdrCallDetail table for each record.
             */
            for (Map<String, Object> cdrDetailMap : cdrDetailRecords) {
                OutboundCallDetail callDetail = new OutboundCallDetail();
                callDetail.setRequestId(cdrDetailMap.get(Constants.REQUEST_ID).toString());
                callDetail.setMsisdn(cdrDetailMap.get(Constants.MSISDN).toString());
                callDetail.setCallId(cdrDetailMap.get(Constants.CALL_ID).toString());
                callDetail.setAttemptNo((Integer) cdrDetailMap.get(Constants.ATTEMPT_NO));
                callDetail.setCallStartTime((Long) cdrDetailMap.get(Constants.CALL_START_TIME));
                callDetail.setCallAnswerTime((Long) cdrDetailMap.get(Constants.CALL_ANSWER_TIME));
                callDetail.setCallEndTime((Long) cdrDetailMap.get(Constants.CALL_END_TIME));
                callDetail.setCallDurationInPulse((Long) cdrDetailMap.get(Constants.CALL_DURATION_IN_PULSE));
                callDetail.setCallStatus((Integer) cdrDetailMap.get(Constants.CALL_STATUS));
                callDetail.setLanguageLocationCode((Integer) cdrDetailMap.get(Constants.LANGUAGE_LOCATION_ID));
                callDetail.setContentFile(cdrDetailMap.get(Constants.CONTENT_FILE).toString());
                callDetail.setMsgPlayEndTime((Integer) cdrDetailMap.get(Constants.MSG_PLAY_END_TIME));
                callDetail.setMsgPlayStartTime((Integer) cdrDetailMap.get(Constants.MSG_PLAY_START_TIME));
                callDetail.setCircleCode(cdrDetailMap.get(Constants.CIRCLE_ID).toString());
                callDetail.setOperatorCode(cdrDetailMap.get(Constants.OPERATOR_ID).toString());
                callDetail.setPriority((Integer) cdrDetailMap.get(Constants.PRIORITY));
                CallDisconnectReason disconnectReason = CallDisconnectReason.getByString(
                        cdrDetailMap.get(Constants.CALL_DISCONNECT_REASON).toString());
                callDetail.setCallDisconnectReason(disconnectReason);
                callDetail.setWeekId(cdrDetailMap.get(Constants.WEEK_ID).toString());
                callDetailService.create(callDetail);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    /*
    This method checks if a particular call valid for retry based on callStatus and final status.
     */
    private Boolean isValidForRetry(CallStatus finalStatus, ObdStatusCode statusCode, Long subscriptionId, Integer weekNumber) {
        Integer retryDayNumber = getRetryDayNumber(subscriptionId);
        if (retryDayNumber.equals(-1) && weekNumber.equals(Constants.MAX_WEEK_NUMBER)) {
            //todo :call subscription api to mark complete.
            return false;
        } else if(finalStatus.equals(CallStatus.REJECTED) ||
                (finalStatus.equals(CallStatus.FAILED) &&
                        !(statusCode.equals(ObdStatusCode.OBD_FAILED_INVALIDNUMBER) || statusCode.equals(ObdStatusCode.OBD_DNIS_IN_DND)))){
            return true;
        }
        return false;
    }

    private void markCompleteForSuccessRecords(CallStatus finalStatus, Integer weekNumber, Long subscriptionId) {
        if(finalStatus.equals(CallStatus.SUCCESS) && weekNumber.equals(Constants.MAX_WEEK_NUMBER)) {
            //todo :call subscription api to mark complete.
        }
    }
    /*
    Method to get retryDayNumber.
     */
    private int getRetryDayNumber(Long subscriptionId) {
        //todo : invoke subscription api to get retryDayNumber
        return 0;
    }

    /*
    This method exports the records from OutboundCallRequest
     */
    private String exportOutBoundCallRequest(Settings settings) {
        updateCallFlowStatus(CallFlowStatus.OUTBOUND_FILE_PREPARATION_EVENT_RECEIVED,
                CallFlowStatus.OUTBOUND_CALL_REQUEST_FILE_CREATED);

        String fileName = "OBD_NMS_" + getCsvFileName();
        String absoluteFileName = settings.getObdFileLocalPath() + "/" + fileName;
        Long recordsCount = 0l;
        File file = new File(absoluteFileName);
        try {
            FileWriter fos = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bf = new BufferedWriter(fos);

            recordsCount = csvImporterExporter.exportCsv("OUTBOUNDCALLREQUEST", bf);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        String obdChecksum = MD5Checksum.findChecksum(absoluteFileName);
        updateCallFlowStatus(CallFlowStatus.OUTBOUND_FILE_PREPARATION_EVENT_RECEIVED,
                CallFlowStatus.OUTBOUND_CALL_REQUEST_FILE_CREATED, obdChecksum, recordsCount, fileName);
        return fileName;
    }

    private String getCsvFileName() {
        DateTime date = new DateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date) + ".csv";
    }

    private void createFreshOBDRecords(Configuration configuration, Settings settings) {
        List<Subscription> scheduledActiveSubscription = subscriptionService.getScheduledSubscriptions();
        for (Subscription subscription : scheduledActiveSubscription) {
            OutboundCallRequest callRequest = new OutboundCallRequest();
            callRequest.setRequestId(subscription.getId().toString());
            callRequest.setServiceId(configuration.getFreshObdServiceId());
            callRequest.setMsisdn(subscription.getMsisdn());
            callRequest.setPriority(configuration.getFreshObdPriority());

            /*
            set languageLocationCode and circleCode in callRequest
             */
            Integer llcCode = subscription.getSubscriber().getLanguageLocationCode();
            if (llcCode != null) {
                String contentFileName = contentUploadService.getContentFileName("W" + callRequest.getWeekId(), llcCode);
                if (contentFileName != null) {
                    callRequest.setContentFileName(contentFileName);
                }
                else {
                    /*
                    if this file name is returned null then create an error log for this record and don't add this record.
                     */
                    logger.error(Constants.CONTENT_FILE_NAME + " not found");
                    continue;
                }
                callRequest.setLanguageLocationCode(subscription.getSubscriber().getLanguageLocationCode());
                LanguageLocationCode record = llcService.findLLCByCode(llcCode);
                if (record.getCircleCode() != null) {
                    callRequest.setCircleCode(record.getCircleCode());
                }
            }
            callRequest.setWeekId(
                    subscription.getWeekNumber().toString() + "_" + subscription.getMessageNumber().toString());
            callRequest.setCli(null);
            callRequest.setCallFlowURL(null);
            requestService.create(callRequest);
        }
        exportOutBoundCallRequest(settings);
    }

    private void updateCallFlowStatus(CallFlowStatus fetchBy, CallFlowStatus updateTo) {
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByCallStatus(fetchBy);
        todayCallFlow.setStatus(updateTo);
        callFlowService.update(todayCallFlow);
    }

    private void updateCallFlowStatus(
            CallFlowStatus fetchBy, CallFlowStatus updateTo, String obdChecksum, Long obdRecordsCount, String obdFileName) {
        OutboundCallFlow todayCallFlow = callFlowService.findRecordByCallStatus(fetchBy);
        todayCallFlow.setStatus(updateTo);
        todayCallFlow.setObdChecksum(obdChecksum);
        todayCallFlow.setObdRecordCount(obdRecordsCount);
        todayCallFlow.setObdFileName(obdFileName);
        callFlowService.update(todayCallFlow);
    }

    private void retryPrepareOBDTargetFile(
            String currentObdFile, String currentCdrObdFile, Integer retryNumber, Configuration configuration) {
        int retryInterval = configuration.getRetryIntervalForObdPreparationInMins();
        Map<String, Object> params = new HashMap<>();
        params.put(MotechSchedulerService.JOB_ID_KEY, Constants.OBD_PREPARATION_RETRY_JOB);
        params.put(Constants.CURRENT_OBD_FILE, currentObdFile);
        params.put(Constants.CURRENT_CDR_OBD_FILE, currentCdrObdFile);
        params.put(Constants.OBD_PREPARATION_RETRY_NUMBER, retryNumber);
        MotechEvent motechEvent = new MotechEvent(Constants.RETRY_PREPARE_OBD_TARGET_EVENT_SUBJECT, params);

        Date retryTime = DateTime.now().plusMinutes(retryInterval).toDate();
        RunOnceSchedulableJob oneTimeJob = new RunOnceSchedulableJob(motechEvent, retryTime);
    }
}

