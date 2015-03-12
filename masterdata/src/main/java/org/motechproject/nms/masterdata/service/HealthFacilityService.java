package org.motechproject.nms.masterdata.service;

import org.motechproject.nms.masterdata.domain.HealthFacility;

/**
 * Created by root on 17/3/15.
 */
public interface HealthFacilityService {
    /**
     * create HealthFacility type object
     *
     * @param record of the HealthFacility
     */
    HealthFacility create(HealthFacility record);

    /**
     * update Circle type object
     *
     * @param record of the HealthFacility
     */
    void update(HealthFacility record);

    /**
     * delete HealthFacility type object
     *
     * @param record of the HealthFacility
     */
    void delete(HealthFacility record);

    /**
     * delete All HealthFacility type object
     */
    void deleteAll();

    /**
     * @param id
     * @return
     */
    HealthFacility findById(Long id);

    HealthFacility findHealthFacilityByParentCode(Long stateCode, Long districtCode,
                                                  Long talukaCode,Long healthBlockCode,
                                                  Long healthFacilityCode);

}