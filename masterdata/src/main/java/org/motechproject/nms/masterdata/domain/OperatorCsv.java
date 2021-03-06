package org.motechproject.nms.masterdata.domain;


import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.domain.MdsEntity;

/**
 * This class Models data records provided in the Operator Csv Upload
 */

@Entity
public class OperatorCsv extends MdsEntity {

    @Field
    private String name;

    @Field
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * This method override the toString method to create string for name and code the instance variables
     *
     * @return The string of the name and code for the instance variables
     */
    public String toString() {

        StringBuffer recordStr = new StringBuffer();
        recordStr.append("name [" + this.name);

        recordStr.append("] code" + this.code);
        return recordStr.toString();

    }
}
