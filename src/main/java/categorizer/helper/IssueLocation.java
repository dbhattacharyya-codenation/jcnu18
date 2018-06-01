package categorizer.helper;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class IssueLocation {
    private String fileName;
    private Integer startLine;
    private Integer endLine;
    private Integer startColumn;
}