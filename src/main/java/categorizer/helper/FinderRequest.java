package categorizer.helper;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter @ToString
public class FinderRequest {
    private Integer requestId;
    private Integer insightId;
    private String language;
    private Sandbox sandbox;
    private List<IssueData> issuesData;
}
