package simplefixer.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder @NoArgsConstructor @AllArgsConstructor
public class IssueResponse {
    private Integer id;
    private Integer issueTypeId;
    private String issueTypeDesc;
    private String fileName;
    private Integer lineNumber;
    private Integer columnNumber;
    private Boolean isFixed;
}
