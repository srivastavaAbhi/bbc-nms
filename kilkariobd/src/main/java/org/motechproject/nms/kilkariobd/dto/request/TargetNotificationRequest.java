package org.motechproject.nms.kilkariobd.dto.request;

public class TargetNotificationRequest {

    private String fileName;

    private String checksum;

    private Integer recordsCount;

    public TargetNotificationRequest() {}

    public TargetNotificationRequest(String fileName, String checksum, Integer recordsCount) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.recordsCount = recordsCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Integer getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(Integer recordsCount) {
        this.recordsCount = recordsCount;
    }
}