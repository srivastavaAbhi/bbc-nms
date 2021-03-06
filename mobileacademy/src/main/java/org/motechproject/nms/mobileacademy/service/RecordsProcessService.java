package org.motechproject.nms.mobileacademy.service;

import java.util.List;

import org.motechproject.nms.mobileacademy.domain.CourseContentCsv;

/**
 * This interface is used to process the CSV Records uploaded by users. It
 * populates MTraining and course content tables based on the operations
 */
public interface RecordsProcessService {

    /**
     * 
     * This service is used to process the CSV records uploaded by a user and
     * hence populate the course content tables and MTraining course structure
     * 
     * @param listOfRecords List of CourseContentCsv records to be processed
     * @param csvFileName csv file name for error logging
     * 
     * @return Confirmation String
     */
    public String processRawRecords(List<CourseContentCsv> listOfRecords,
            String csvFileName);
}
