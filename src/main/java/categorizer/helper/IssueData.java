package categorizer.helper;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter @ToString
public class IssueData {
    private Integer id;
    private List<IssueLocation> issueLocations;
    private List<List<Long>> nodeIdMappings;
}