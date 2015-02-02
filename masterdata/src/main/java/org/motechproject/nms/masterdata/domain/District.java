package org.motechproject.nms.masterdata.domain;

import org.motechproject.mds.annotations.Cascade;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

import java.util.Set;

/**
 * This class Models data for District location records
 */
@Entity(recordHistory = true)
public class District extends LocationUnitMetaData {

    @Field
    private Long districtCode;

    @Field
    @Cascade(delete = true)
    private Set<Taluka> taluka;

    @Field
    private Long stateCode;


    public District() {
        super();
    }

    public Long getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Long districtCode) {
        this.districtCode = districtCode;
    }

    public Set<Taluka> getTaluka() {
        return taluka;
    }

    public void setTaluka(Set<Taluka> taluka) {
        this.taluka = taluka;
    }

    public Long getStateCode() {
        return stateCode;
    }

    public void setStateCode(Long stateCode) {
        this.stateCode = stateCode;
    }

    @Override
    public String toString() {
        return "District{" +
                "districtCode=" + districtCode +
                ", taluka=" + taluka +
                ", stateCode=" + stateCode +
                '}';
    }
}
