package categorizer.helper;

import categorizer.constant.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class IssueCategory {
    private Integer id;
    private Category category;
}
