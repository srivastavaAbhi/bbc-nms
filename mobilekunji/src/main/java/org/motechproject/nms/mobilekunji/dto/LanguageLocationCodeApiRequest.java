package org.motechproject.nms.mobilekunji.dto;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * API request for Language Location Code
 */
public class LanguageLocationCodeApiRequest {

    @JsonProperty
    private String callingNumber;

    @JsonProperty
    private Integer languageLocationCode;

    @JsonProperty
    private String callId;

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(String callingNumber) {
        this.callingNumber = callingNumber;
    }

    public Integer getLanguageLocationCode() {
        return languageLocationCode;
    }

    public void setLanguageLocationCode(Integer languageLocationCode) {
        this.languageLocationCode = languageLocationCode;
    }
}
