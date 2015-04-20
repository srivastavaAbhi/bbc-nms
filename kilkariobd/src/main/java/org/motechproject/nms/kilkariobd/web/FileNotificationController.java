package org.motechproject.nms.kilkariobd.web;

import org.motechproject.nms.kilkariobd.dto.request.CdrNotificationRequest;
import org.motechproject.nms.util.helper.DataValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FileNotificationController {
    Logger logger = LoggerFactory.getLogger(FileNotificationController.class);

    @RequestMapping(value = "/cdrFileNotification", method = RequestMethod.POST)
    @ResponseBody
    public void CDRFileNotification(@RequestBody CdrNotificationRequest apiRequest) throws DataValidationException{
        logger.debug("CDRFileNotification: started");
        logger.debug("CDRFileNotification Request Parameters");
        logger.debug("fileName : [" + apiRequest.getFileName() + "]");
        logger.debug("cdrSummary parameters");
        logger.debug("cdrFile : [" + apiRequest.getCdrSummary().getCdrFile() + "]");
        logger.debug("cdrChecksum : [" + apiRequest.getCdrSummary().getCdrChecksum() + "]");
        logger.debug("recordsCount : [%d]", apiRequest.getCdrSummary().getRecordsCount());
        logger.debug("cdrDetail parameters");
        logger.debug("cdrFile : [" + apiRequest.getCdrDetail().getCdrFile() + "]");
        logger.debug("cdrChecksum : [" + apiRequest.getCdrDetail().getCdrChecksum() + "]");
        logger.debug("recordsCount : [%d]", apiRequest.getCdrDetail().getRecordsCount());
        apiRequest.validateMandatoryParameters();
        //todo : update outboundcallflow record in DB.
        logger.debug("CDRFileNotification: Ended");

    }

    @RequestMapping(value = "/obdFileProcessedNotification", method = RequestMethod.POST)
    public void FileProcessedStatusNotification() {

    }
}