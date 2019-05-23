package simplefixer.helper;

public class IssueResponse {
    private Integer id;
    private Integer issueTypeId;
    private String issueTypeDesc;
    private String fileName;
    private Integer lineNumber;
    private Integer columnNumber;
    private Boolean isFixed;

    public IssueResponse(Integer id, Integer issueTypeId, String issueTypeDesc,String fileName, Integer lineNumber,Integer columnNumber, Boolean isFixed) {
        this.id = id;
        this.issueTypeId = issueTypeId;
        this.issueTypeDesc = issueTypeDesc;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.isFixed = isFixed;
    }
}
