package org.motechproject.nms.kilkari.domain;


import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.UIDisplayable;
import org.motechproject.mds.domain.MdsEntity;

/**
 * This entity represents the content upload record in kilkari module
 */
@Entity(recordHistory = true)
public class ContentUpload extends MdsEntity {

    @Field(required = true)
    @UIDisplayable(position = 0)
    private Long contentId;

    @Field(required = true)
    @UIDisplayable(position = 1)
    private String circleCode;

    @Field(required = true)
    @UIDisplayable(position = 2)
    private Integer languageLocationCode;

    @Field(required = true)
    @UIDisplayable(position = 3)
    private String contentName;

    @Field(required = true)
    @UIDisplayable(position = 4)
    private ContentType contentType;

    @Field(required = true)
    @UIDisplayable(position = 5)
    private String contentFile;

    @Field(required = true)
    @UIDisplayable(position = 6)
    private Integer contentDuration;

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public String getCircleCode() {
        return circleCode;
    }

    public void setCircleCode(String circleCode) {
        this.circleCode = circleCode;
    }

    public Integer getLanguageLocationCode() {
        return languageLocationCode;
    }

    public void setLanguageLocationCode(Integer languageLocationCode) {
        this.languageLocationCode = languageLocationCode;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getContentFile() {
        return contentFile;
    }

    public void setContentFile(String contentFile) {
        this.contentFile = contentFile;
    }

    public Integer getContentDuration() {
        return contentDuration;
    }

    public void setContentDuration(Integer contentDuration) {
        this.contentDuration = contentDuration;
    }
}
