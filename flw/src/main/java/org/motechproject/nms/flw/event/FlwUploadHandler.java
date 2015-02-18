package org.motechproject.nms.flw.event;

/**
 * Created by abhishek on 2/2/15.
 */


import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.flw.domain.FrontLineWorker;
import org.motechproject.nms.flw.domain.FrontLineWorkerCsv;
import org.motechproject.nms.flw.repository.FlwCsvRecordsDataService;
import org.motechproject.nms.flw.repository.FlwRecordDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

import static org.motechproject.nms.flw.FrontLineWorkerConstants.*;

@Component
public class FlwUploadHandler {

    @Autowired
    private FlwRecordDataService flwRecordDataService;

    @Autowired
    private FlwCsvRecordsDataService flwCsvRecordsDataService;

    private static Logger logger = LoggerFactory.getLogger(FlwUploadHandler.class);





    @MotechListener(subjects = {FLW_UPLOAD_SUCCESS})
    public void flwDataHandler(MotechEvent event) {

        logger.error("entered flwDataHandler");

        List<FrontLineWorkerCsv> frontLineWorkerCsvList = flwCsvRecordsDataService.retrieveAll();

        for (Iterator<FrontLineWorkerCsv> itr = frontLineWorkerCsvList.iterator(); itr.hasNext();) {

            logger.error("entered loop");

            FrontLineWorkerCsv flwCsvRecord = itr.next();

            FrontLineWorker flwRecord = getFlwData(flwCsvRecord);

            logger.error("about to create record in db");

            flwRecordDataService.create(flwRecord);
        }
    }

    private FrontLineWorker getFlwData(FrontLineWorkerCsv flwCsvData) {
                logger.error("entered getFlwData");

                FrontLineWorker flwData = new FrontLineWorker(Long.valueOf(flwCsvData.getFlwId()).longValue(), Long.valueOf(flwCsvData.getStateId()).longValue(), flwCsvData.getContactNumber(), flwCsvData.getName(), flwCsvData.getType(), 1, Long.valueOf(flwCsvData.getDistrictId()).longValue(), "true", "true");

        return flwData;
    }

    @MotechListener(subjects = {FLW_UPLOAD_FAILED})
    public void frontLineWorkerFailed(MotechEvent event) {

        logger.error("entered frontLineWorkerFailed");
        flwCsvRecordsDataService.deleteAll();
    }

}


