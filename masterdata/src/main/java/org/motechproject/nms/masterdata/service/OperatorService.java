package org.motechproject.nms.masterdata.service;

import org.motechproject.nms.masterdata.domain.Operator;

/**
 * This interface is used for crud operations on Operator
 */

public interface OperatorService {

    /**
     * creates Operator in database
     *
     * @param record of Operator to create
     */
    Operator create(Operator record);

    /**
     * updates Operator in database
     *
     * @param record of Operator to update
     */
    Operator update(Operator record);

    /**
     * deletes Operator from database
     *
     * @param record Operator from database
     */
    void delete(Operator record);

    /**
     * get Operator record for given Operator Census code
     *
     * @param operatorCode Operator Census Code
     * @return Operator object corresponding to the census code
     */
    Operator getRecordByCode(String operatorCode);
}
