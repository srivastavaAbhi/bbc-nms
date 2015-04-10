package org.motechproject.nms.mobileacademy.dto;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

//This is just a placeholder for save call details API.
//actual implementation would be done in sprint 1504
public class CallDetails implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String callingNumber;

    @JsonProperty
    private String callId;

    @JsonProperty
    private String operator;

    @JsonProperty
    private String circle;

    @JsonProperty
    private String callStartTime;

    @JsonProperty
    private String callEndTime;

    @JsonProperty
    private String callDurationInPulses;

    @JsonProperty
    private String endOfUsagePromptCounter;

    @JsonProperty
    private String callStatus;

    @JsonProperty
    private String callDisconnectReason;

    @JsonProperty
    private List<ContentDetails> content;

    public String getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(String callingNumber) {
        this.callingNumber = callingNumber;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getCircle() {
        return circle;
    }

    public void setCircle(String circle) {
        this.circle = circle;
    }

    public String getCallStartTime() {
        return callStartTime;
    }

    public void setCallStartTime(String callStartTime) {
        this.callStartTime = callStartTime;
    }

    public String getCallEndTime() {
        return callEndTime;
    }

    public void setCallEndTime(String callEndTime) {
        this.callEndTime = callEndTime;
    }

    public String getCallDurationInPulses() {
        return callDurationInPulses;
    }

    public void setCallDurationInPulses(String callDurationInPulses) {
        this.callDurationInPulses = callDurationInPulses;
    }

    public String getEndOfUsagePromptCounter() {
        return endOfUsagePromptCounter;
    }

    public void setEndOfUsagePromptCounter(String endOfUsagePromptCounter) {
        this.endOfUsagePromptCounter = endOfUsagePromptCounter;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(String callStatus) {
        this.callStatus = callStatus;
    }

    public String getCallDisconnectReason() {
        return callDisconnectReason;
    }

    public void setCallDisconnectReason(String callDisconnectReason) {
        this.callDisconnectReason = callDisconnectReason;
    }

    public List<ContentDetails> getCourseContentList() {
        return content;
    }

    public void setCourseContentList(List<ContentDetails> contentDetails) {
        this.content = contentDetails;
    }

    @Override
    public String toString() {
        return "CallDetails{callingNumber=" + callingNumber + ", callId="
                + callId + ", operator=" + operator + ", circle=" + circle
                + ", callStartTime=" + callStartTime + ", callEndTime="
                + callEndTime + ", callDurationInPulses="
                + callDurationInPulses + ", endOfUsagePromptCounter="
                + endOfUsagePromptCounter + ", callStatus=" + callStatus
                + ", callDisconnectReason=" + callDisconnectReason
                + ", content=" + content + "}";
    }
}