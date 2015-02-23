package org.motechproject.nms.kilkari.service.impl;

import org.motechproject.nms.kilkari.domain.ContentUploadKKCsv;
import org.motechproject.nms.kilkari.repository.ContentUploadKKCsvDataService;
import org.motechproject.nms.kilkari.service.ContentUploadKKCsvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("contentUploadKKCsvService")
public class ContentUploadKKCsvServiceImpl implements ContentUploadKKCsvService {

    @Autowired
    private ContentUploadKKCsvDataService contentUploadKKCsvDataService;

    @Override
    public ContentUploadKKCsv getRecord(Long id) {
        return contentUploadKKCsvDataService.findById(id);
    }

    @Override
    public void delete(ContentUploadKKCsv record) {
        contentUploadKKCsvDataService.delete(record);
    }

}