package org.motechproject.nms.masterdata.service.impl;

import org.motechproject.nms.masterdata.domain.OperatorCsv;
import org.motechproject.nms.masterdata.repository.OperatorCsvDataService;
import org.motechproject.nms.masterdata.service.OperatorCsvService;
import org.springframework.beans.factory.annotation.Autowired;

public class OperatorCsvServiceImpl implements OperatorCsvService {

    @Autowired
    private OperatorCsvDataService operatorCsvDataService;

    @Override
    public OperatorCsv findById(Long id) {
        return operatorCsvDataService.findById(id);
    }

    @Override
    public void delete(OperatorCsv record) {
        operatorCsvDataService.delete(record);
    }
}