package org.motechproject.nms.kilkari.service.impl;

import java.util.List;

import org.joda.time.DateTime;
import org.motechproject.nms.kilkari.domain.BeneficiaryType;
import org.motechproject.nms.kilkari.domain.Channel;
import org.motechproject.nms.kilkari.domain.DeactivationReason;
import org.motechproject.nms.kilkari.domain.MotherMctsCsv;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.repository.MotherMctsCsvDataService;
import org.motechproject.nms.kilkari.service.ConfigurationService;
import org.motechproject.nms.kilkari.service.LocationValidatorService;
import org.motechproject.nms.kilkari.service.MotherMctsCsvService;
import org.motechproject.nms.kilkari.service.SubscriberService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.constants.ErrorDescriptionConstants;
import org.motechproject.nms.util.domain.BulkUploadError;
import org.motechproject.nms.util.domain.BulkUploadStatus;
import org.motechproject.nms.util.domain.RecordType;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.NmsUtils;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("motherMctsCsvService")
public class MotherMctsCsvServiceImpl implements MotherMctsCsvService {

    @Autowired
    private MotherMctsCsvDataService motherMctsCsvDataService;
    
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private LocationValidatorService locationValidator;

    @Autowired
    private SubscriberService subscriberService;
    
    @Autowired
    private ConfigurationService configurationService;
    
    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;
    
    private static Logger logger = LoggerFactory.getLogger(ChildMctsCsvServiceImpl.class);
    
    public static final String STILL_BIRTH_ZERO = "0";
    public static final String MOTHER_DEATH_NINE = "9";
    public static final String ABORTION_NONE = "none";

    @Override
    public void processMotherMctsCsv(String csvFileName, List<Long> uploadedIDs){
        logger.info("Processing Csv file[{}]", csvFileName);
        BulkUploadStatus uploadedStatus = new BulkUploadStatus();
        BulkUploadError errorDetails = new BulkUploadError();
        DateTime timeOfUpload = NmsUtils.getCurrentTimeStamp();
        errorDetails.setCsvName(csvFileName);
        errorDetails.setTimeOfUpload(timeOfUpload);
        errorDetails.setRecordType(RecordType.MOTHER_MCTS);

        MotherMctsCsv motherMctsCsv = null;
        String userName = null;

        for (Long id : uploadedIDs) {
            try {
                logger.debug("Processing record id[{}]", id);
                motherMctsCsv = motherMctsCsvDataService.findById(id);
                if (motherMctsCsv != null) {
                    logger.debug("Record found in database for uploaded id[{}]", id);
                    userName = motherMctsCsv.getOwner();
                    Subscriber subscriber = mapMotherMctsToSubscriber(motherMctsCsv);
                    subscriptionService.handleMctsSubscriptionRequestForMother(subscriber, Channel.MCTS);
                    uploadedStatus.incrementSuccessCount();
                } else {
                    errorDetails.setErrorDescription(ErrorDescriptionConstants.CSV_RECORD_MISSING_DESCRIPTION);
                    errorDetails.setErrorCategory(ErrorCategoryConstants.CSV_RECORD_MISSING);
                    errorDetails.setRecordDetails("Record is null");
                    bulkUploadErrLogService.writeBulkUploadErrLog(errorDetails);
                    logger.error("Record not found for uploaded id [{}]", id);
                    uploadedStatus.incrementFailureCount();
                }
                logger.info("Processing finished for record id[{}]", id);
            } catch (DataValidationException dve) {
                logger.warn("DataValidationException ::::", dve.getMessage());
                errorDetails.setRecordDetails(motherMctsCsv.toString());
                errorDetails.setErrorCategory(dve.getErrorCode());
                errorDetails.setErrorDescription(dve.getErrorDesc());
                bulkUploadErrLogService.writeBulkUploadErrLog(errorDetails);

                uploadedStatus.incrementFailureCount();

            } catch (Exception e) {
                errorDetails.setRecordDetails("Some Error Occurred");
                errorDetails.setErrorCategory(ErrorCategoryConstants.GENERAL_EXCEPTION);
                errorDetails.setErrorDescription(ErrorDescriptionConstants.GENERAL_EXCEPTION_DESCRIPTION);
                bulkUploadErrLogService.writeBulkUploadErrLog(errorDetails);
                uploadedStatus.incrementFailureCount();
            }finally {
                logger.debug("Inside finally");
                if (motherMctsCsv != null) {
                    logger.info("Deleting motherMctsCsv record id[{}]", motherMctsCsv.getId());
                    motherMctsCsvDataService.delete(motherMctsCsv);
                }
            }
        }
        uploadedStatus.setUploadedBy(userName);
        uploadedStatus.setBulkUploadFileName(csvFileName);
        uploadedStatus.setTimeOfUpload(timeOfUpload);
        
        bulkUploadErrLogService.writeBulkUploadProcessingSummary(uploadedStatus);
        logger.info("Processed Csv file[{}]", csvFileName);
    }
    
    /**
     *  This method is used to validate csv uploaded record 
     *  and map Mother mcts to subscriber
     * 
     *  @param motherMctsCsv csv uploaded subscriber
     */
    private Subscriber mapMotherMctsToSubscriber(MotherMctsCsv motherMctsCsv) throws DataValidationException {

        Subscriber motherSubscriber = new Subscriber();

        logger.trace("mapMotherMctsToSubscriber method start");
        motherSubscriber = locationValidator.validateAndMapMctsLocationToSubscriber(motherMctsCsv, motherSubscriber);
        
        String msisdn = ParseDataHelper.parseString("Whom Phone Num", motherMctsCsv.getWhomPhoneNo(), true);
        motherSubscriber.setMsisdn(NmsUtils.trimMsisdn(msisdn));
        
        motherSubscriber.setMotherMctsId(ParseDataHelper.parseString("idNo", motherMctsCsv.getIdNo(), true));
        motherSubscriber.setAge(ParseDataHelper.parseInt("Age", motherMctsCsv.getAge(), false));
        motherSubscriber.setAadharNumber(ParseDataHelper.parseString("AAdhar Num", motherMctsCsv.getAadharNo(), true));
        motherSubscriber.setName(ParseDataHelper.parseString("Name", motherMctsCsv.getName(),false));
        motherSubscriber.setLmp(ParseDataHelper.parseDate("Lmp Date", motherMctsCsv.getLmpDate(), true));
        
        String abortion = ParseDataHelper.parseString("Abortion", motherMctsCsv.getAbortion(), false);
        if (STILL_BIRTH_ZERO.equalsIgnoreCase(motherMctsCsv.getOutcomeNos())) {
            motherSubscriber.setDeactivationReason(DeactivationReason.STILL_BIRTH);
        } else if (!(abortion == null || ABORTION_NONE.equalsIgnoreCase(abortion))) {
            motherSubscriber.setDeactivationReason(DeactivationReason.ABORTION);
        } else if (MOTHER_DEATH_NINE.equalsIgnoreCase(ParseDataHelper.parseString("Entry Type", motherMctsCsv.getEntryType(), false))) {
            motherSubscriber.setDeactivationReason(DeactivationReason.MOTHER_DEATH);
        } else {
            motherSubscriber.setDeactivationReason(DeactivationReason.NONE);
        }
        
        motherSubscriber.setBeneficiaryType(BeneficiaryType.MOTHER);
        motherSubscriber.setModifiedBy(motherMctsCsv.getModifiedBy());
        motherSubscriber.setCreator(motherMctsCsv.getCreator());
        motherSubscriber.setOwner(motherMctsCsv.getOwner());

        logger.trace("mapMotherMctsToSubscriber method finished");
        return motherSubscriber;
    }
    
}
