package org.motechproject.nms.kilkari.service;

import org.motechproject.nms.kilkari.domain.ContentUpload;

/**
 * This interface provides methods to perform crud operations on ContentUpload
 */
public interface ContentUploadService {

    /**
     * This method creates record in the database of type ContentUploadKK
     *
     * @param record object of type ContentUploadKK
     */
    void create(ContentUpload record);

    /**
     * This method update record in the database of type ContentUploadKK
     *
     * @param record object of type ContentUploadKK
     */
    void update(ContentUpload record);

    /**
     * This method get ContentUploadKK type record based on content id
     *
     * @param contentId Unique key for the record
     * @return ContentUploadKK object
     */
    ContentUpload getRecordByContentId(Long contentId);

    /**
     * This method get ContentUpload type record based on content name and language location code
     *
     * @param contentName String type object
     * @param languageLocationCode Integer type object
     * @return ContentUpload ContentUpload type object
     */
    String getContentFileName(String contentName, String languageLocationCode);

}
