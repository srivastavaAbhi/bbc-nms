package org.motechproject.nms.kilkari.service.impl;

import org.motechproject.nms.kilkari.domain.ContentUpload;
import org.motechproject.nms.kilkari.repository.ContentUploadDataService;
import org.motechproject.nms.kilkari.service.ContentUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This class is used to perform crud operations on ContentUpload
 */
@Service("contentUploadService")
public class ContentUploadServiceImpl implements ContentUploadService {

    @Autowired
    private ContentUploadDataService contentUploadDataService;

    /**
     * This method creates record in the database of type ContentUploadKK
     *
     * @param record object of type ContentUploadKK
     */
    @Override
    public void create(ContentUpload record) {
        contentUploadDataService.create(record);
    }

    /**
     * This method update record in the database of type ContentUploadKK
     *
     * @param record object of type ContentUploadKK
     */
    @Override
    public void update(ContentUpload record) {
        contentUploadDataService.update(record);
    }

    /**
     * This method get ContentUploadKK type record based on content id
     *
     * @param contentId Unique key for the record
     * @return ContentUploadKK object
     */
    @Override
    public ContentUpload getRecordByContentId(Long contentId) {
        return contentUploadDataService.findByContentId(contentId);
    }
    
    /**
     * This method get Content File name based on content name and language location code
     *
     * @param contentName String type object
     * @param languageLocationCode Integer type object
     * @return ContentUpload ContentUpload type object
     */
    @Override
    public String getContentFileName(String contentName, String languageLocationCode) {
        String contentFile = null;
        List<ContentUpload> contentUploadList = contentUploadDataService.findContentFileName(contentName, languageLocationCode);
        if(contentUploadList != null && !contentUploadList.isEmpty()){
            contentFile  = contentUploadList.get(0).getContentFile();
        }
        return contentFile; 
    }
}
