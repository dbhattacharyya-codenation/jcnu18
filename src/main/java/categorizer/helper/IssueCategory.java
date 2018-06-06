package categorizer.helper;

import categorizer.constant.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor @Getter
public class IssueCategory {
    private Integer id;
    private Category category;
    List<IssueLocation> issueLocations;
}
