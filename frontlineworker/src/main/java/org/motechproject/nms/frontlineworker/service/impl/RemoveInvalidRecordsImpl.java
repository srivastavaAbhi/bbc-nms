package org.motechproject.nms.frontlineworker.service.impl;

import org.motechproject.nms.frontlineworker.domain.FrontLineWorker;
import org.motechproject.nms.frontlineworker.repository.CustomQueries;
import org.motechproject.nms.frontlineworker.repository.FrontLineWorkerRecordDataService;
import org.motechproject.nms.frontlineworker.service.FrontLineWorkerService;
import org.motechproject.nms.frontlineworker.service.RemoveInvalidRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by abhishek on 21/4/15.
 */
@Service("removeInvalidRecordsService")
public class RemoveInvalidRecordsImpl implements RemoveInvalidRecords {

    @Autowired
    FrontLineWorkerRecordDataService frontLineWorkerRecordDataService;

    @Autowired
    FrontLineWorkerService frontLineWorkerService;

    @Override
    public List<FrontLineWorker> invalidFrontLineWorkerList() {
        return frontLineWorkerRecordDataService.executeQuery(new CustomQueries.DeleteFrontLineWorkerQuery());

    }

    @Override
    public void deleteInvalidFrontLineWorkerRecords() {
        List<FrontLineWorker> frontLineWorkerListToBeDeleted = invalidFrontLineWorkerList();

        for(FrontLineWorker frontLineWorker: frontLineWorkerListToBeDeleted) {
            frontLineWorkerService.deleteFrontLineWorker(frontLineWorker);
        }



    }
}