package org.motechproject.nms.kilkariobd.dto.request;

/**
 * Entity represents the TargetNotificationRequest.
 */
public class TargetNotificationRequest {

    private String fileName;

    private String checksum;

    private Long recordsCount;

    public TargetNotificationRequest() {}

    public TargetNotificationRequest(String fileName, String checksum, Long recordsCount) {
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

    public Long getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(Long recordsCount) {
        this.recordsCount = recordsCount;
    }
}
