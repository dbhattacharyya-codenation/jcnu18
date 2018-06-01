package categorizer.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor @Getter
public class CategorizerResponse {
    private Integer statusCode;
    private String statusMessage;
    private List<IssueCategory> issueCategories;
}
