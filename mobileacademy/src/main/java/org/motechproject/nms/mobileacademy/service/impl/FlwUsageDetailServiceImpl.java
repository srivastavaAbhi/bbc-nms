package org.motechproject.nms.mobileacademy.service.impl;

import org.motechproject.nms.mobileacademy.domain.FlwUsageDetail;
import org.motechproject.nms.mobileacademy.repository.FlwUsageDetailDataService;
import org.motechproject.nms.mobileacademy.service.FlwUsageDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * class having implementation of methods to perform operations on
 * FlwUsageDetail table.
 *
 */
@Service("FlwUsageDetailService")
public class FlwUsageDetailServiceImpl implements FlwUsageDetailService {

    @Autowired
    private FlwUsageDetailDataService flwUsageDetailDataService;

    /*
     * (non-Javadoc)
     * 
     * @see org.motechproject.nms.mobileacademy.service.FlwUsageDetailService#
     * createFlwUsageRecord
     * (org.motechproject.nms.mobileacademy.domain.FlwUsageDetail)
     */
    @Override
    public FlwUsageDetail createFlwUsageRecord(FlwUsageDetail flwUsageDetail) {
        return flwUsageDetailDataService.create(flwUsageDetail);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.motechproject.nms.mobileacademy.service.FlwUsageDetailService#findByFlwId
     * (java.lang.Long)
     */
    @Override
    public FlwUsageDetail findByFlwId(Long flwId) {
        return flwUsageDetailDataService.findByFlwId(flwId);
    }

}
