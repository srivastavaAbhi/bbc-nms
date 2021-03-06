package org.motechproject.nms.kilkari.domain;

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * This entity represents the mothercsv mcts record.
 */
@Entity
public class MotherMctsCsv extends MctsCsv {
    
    @Field
    private String name;
    
    @Field
    private String lmpDate;
    
    @Field
    private String abortion;
    
    @Field
    private String outcomeNos;
    
    @Field
    private String age;
    
    @Field
    private String aadharNo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLmpDate() {
        return lmpDate;
    }

    public void setLmpDate(String lmpDate) {
        this.lmpDate = lmpDate;
    }

    public String getAbortion() {
        return abortion;
    }

    public void setAbortion(String abortion) {
        this.abortion = abortion;
    }

    public String getOutcomeNos() {
        return outcomeNos;
    }

    public void setOutcomeNos(String outcomeNos) {
        this.outcomeNos = outcomeNos;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(String aadharNo) {
        this.aadharNo = aadharNo;
    }

    /**
     * This method override the toString method to create string for Mcts Id and stateCode
     * @return String of Mcts Id and stateCode
     */
    @Override
    public String toString() {
        return "Mcts Id["+this.getIdNo()+"] stateCode["+this.getStateCode()+"]";
    }
}
