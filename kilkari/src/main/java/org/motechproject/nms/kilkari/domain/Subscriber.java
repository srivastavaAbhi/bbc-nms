package org.motechproject.nms.kilkari.domain;

import java.util.Set;

import javax.jdo.annotations.Persistent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.joda.time.DateTime;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.Ignore;
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
    private String msisdn;
    
    @Field
    private String childMctsId;
    
    @Field
    private String motherMctsId;
    
    @Field(required = true)
    private BeneficiaryType beneficiaryType;
    
    @Field
    private String name;
    
    @Field
    private Integer age;
    
    @Field(name = "stateId")
    private State state;
    
    @Field(name = "districtId")
    private District district;
    
    @Field(name = "talukaId")
    private Taluka taluka;
    
    @Field(name = "healthBlockId")
    private HealthBlock healthBlock;
    
    @Field(name = "phcId")
    private HealthFacility phc;
    
    @Field(name = "subCentreId")
    private HealthSubFacility subCentre;
    
    @Field(name = "villageId")
    private Village village;
    
    @Field
    @Min(value=1)
    @Max(value=99)
    private Integer languageLocationCode;
    
    @Field
    private String aadharNumber;
    
    @Field
    private DateTime lmp;
    
    @Field
    private DateTime dob;
    
    @Persistent(mappedBy = "subscriber")
    private Set<Subscription> subscriptionList;

    @Field
    private Long stateCode;

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
