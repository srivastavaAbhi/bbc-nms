package org.motechproject.nms.kilkari.domain;

import java.util.Set;

import javax.jdo.annotations.Persistent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.joda.time.DateTime;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.Ignore;
import org.motechproject.mds.annotations.UIDisplayable;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.HealthBlock;
import org.motechproject.nms.masterdata.domain.HealthFacility;
import org.motechproject.nms.masterdata.domain.HealthSubFacility;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.domain.Taluka;
import org.motechproject.nms.masterdata.domain.Village;

/**
 * This entity represents the subscriber record.
 */
@Entity(recordHistory=true)
public class Subscriber extends MdsEntity {
    
    @Field(required = true)
    @UIDisplayable(position = 0)
    private String msisdn;
    
    @Field(required = true)
    @UIDisplayable(position = 1)
    private BeneficiaryType beneficiaryType;
    
    @Field
    @UIDisplayable(position = 2)
    private String name;
    
    @Field
    @UIDisplayable(position = 3)
    private Integer age;
    
    @Field
    @UIDisplayable(position = 4)
    private Long stateCode;
    
    @Field
    @UIDisplayable(position = 5)
    private String motherMctsId;
    
    @Field
    @UIDisplayable(position = 6)
    private String childMctsId;
    
    @Field
    @Min(value=1)
    @Max(value=99)
    @UIDisplayable(position = 7)
    private Integer languageLocationCode;
    
    @Field
    @UIDisplayable(position = 8)
    private DateTime dob;
    
    @Field
    @UIDisplayable(position = 9)
    private DateTime lmp;
    
    @Field
    @UIDisplayable(position = 10)
    private String aadharNumber; 
    
    @Field(name = "stateId")
    @UIDisplayable(position = 11)
    private State state;
    
    @Field(name = "districtId")
    @UIDisplayable(position = 12)
    private District district;
    
    @Field(name = "talukaId")
    @UIDisplayable(position = 13)
    private Taluka taluka;
    
    @Field(name = "healthBlockId")
    @UIDisplayable(position = 14)
    private HealthBlock healthBlock;
    
    @Field(name = "phcId")
    @UIDisplayable(position = 15)
    private HealthFacility phc;
    
    @Field(name = "subCentreId")
    @UIDisplayable(position = 16)
    private HealthSubFacility subCentre;
    
    @Field(name = "villageId")
    @UIDisplayable(position = 17)
    private Village village;
    
    @Persistent(mappedBy = "subscriber")
    private Set<Subscription> subscriptionList;

    /* Ignoring this field in entity, so that it is not created as a column,
    This field is used in mapping deactivation reason from Mother/Child Csv.
    And it will further be used to update deactivationReason in Subscription Entity.
     */
    @Ignore
    private DeactivationReason deactivationReason;

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getChildMctsId() {
        return childMctsId;
    }

    public void setChildMctsId(String childMctsId) {
        this.childMctsId = childMctsId;
    }

    public String getMotherMctsId() {
        return motherMctsId;
    }

    public void setMotherMctsId(String motherMctsId) {
        this.motherMctsId = motherMctsId;
    }

    public BeneficiaryType getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(BeneficiaryType beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Taluka getTaluka() {
        return taluka;
    }

    public void setTaluka(Taluka taluka) {
        this.taluka = taluka;
    }

    public HealthBlock getHealthBlock() {
        return healthBlock;
    }

    public void setHealthBlock(HealthBlock healthBlock) {
        this.healthBlock = healthBlock;
    }

    public HealthFacility getPhc() {
        return phc;
    }

    public void setPhc(HealthFacility phc) {
        this.phc = phc;
    }

    public HealthSubFacility getSubCentre() {
        return subCentre;
    }

    public void setSubCentre(HealthSubFacility subCentre) {
        this.subCentre = subCentre;
    }

    public Village getVillage() {
        return village;
    }

    public void setVillage(Village village) {
        this.village = village;
    }

    public Integer getLanguageLocationCode() {
        return languageLocationCode;
    }

    public void setLanguageLocationCode(Integer languageLocationCode) {
        this.languageLocationCode = languageLocationCode;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

    public DateTime getLmp() {
        return lmp;
    }

    public void setLmp(DateTime lmp) {
        this.lmp = lmp;
    }

    public DateTime getDob() {
        return dob;
    }

    public void setDob(DateTime dob) {
        this.dob = dob;
    }

    public Set<Subscription> getSubscriptionList() {
        return subscriptionList;
    }

    public void setSubscriptionList(Set<Subscription> subscriptionList) {
        this.subscriptionList = subscriptionList;
    }
    
    public DeactivationReason getDeactivationReason() {
        return deactivationReason;
    }

    public void setDeactivationReason(DeactivationReason deactivationReason) {
        this.deactivationReason = deactivationReason;
    }

    @Ignore
    public SubscriptionPack getSuitablePackName(){
        if (BeneficiaryType.MOTHER.equals(this.beneficiaryType)) {
            return SubscriptionPack.PACK_72_WEEKS;
        } else {
            return SubscriptionPack.PACK_48_WEEKS;
        }
    }

    @Ignore
    public String getSuitableMctsId() {
        if (BeneficiaryType.MOTHER.equals(this.beneficiaryType)) {
            return getMotherMctsId();
        } else {
            return getChildMctsId();
        }
    }
    
    @Ignore
    public DateTime getDobLmp() {
        if (BeneficiaryType.MOTHER.equals(this.beneficiaryType)) {
            return getLmp();
        } else {
            return getDob();
        }
    }

    public Long getStateCode() {
        return stateCode;
    }

    public void setStateCode(Long stateCode) {
        this.stateCode = stateCode;
    }
}
