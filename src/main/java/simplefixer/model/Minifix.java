package simplefixer.model;

import javax.persistence.*;

@Entity
@Table(name = "minifixes")
public class Minifix {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "issue_type_id", referencedColumnName = "id")
    private IssueType issueType;

    private String sandBoxURL;

    private String s3Link;

    private String fileName;

    private Integer lineNumber;

    private Integer columnNumber;

    private Integer fixId;

    private Boolean isFixed;

    public Minifix() {}

    public Integer getId() {
        return id;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public String getSandBoxURL() {
        return sandBoxURL;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

    public Integer getFixId() {
        return fixId;
    }

    public Boolean getIsFixed() {
        return isFixed;
    }

    public String getS3Link() {
        return s3Link;
    }

    public Minifix setIssueType(IssueType issueType) {
        this.issueType = issueType;
        return this;
    }

    public Minifix setSandBoxURL(String sandBoxURL) {
        this.sandBoxURL = sandBoxURL;
        return this;
    }

    public Minifix setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Minifix setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    public Minifix setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
        return this;
    }

    public Minifix setFixId(Integer fixId) {
        this.fixId = fixId;
        return this;
    }

    public Minifix setIsFixed() {
        isFixed = true;
        return this;
    }

    public Minifix setS3Link(String s3Link) {
        this.s3Link = s3Link;
        return this;
    }

    @Override
    public String toString() {
        return String.format("fixId : %d issueId : %d fileName : %s" +
                            "line : %d column : %d isFixed : %b" +
                            "sandboxURL : %s, s3Link : %s",
                            id, issueType.getId(), fileName, lineNumber, columnNumber, isFixed, sandBoxURL, s3Link);
    }
}
