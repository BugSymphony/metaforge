package com.metaforge.metadata.api.dto.response;

public class ValidationErrorDetailDto {
    private String jsonPath;
    private String violationType;
    private String ruleReference;
    private String message;

    public ValidationErrorDetailDto() {}

    public String getJsonPath() { return jsonPath; }
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }
    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public String getRuleReference() { return ruleReference; }
    public void setRuleReference(String ruleReference) { this.ruleReference = ruleReference; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
