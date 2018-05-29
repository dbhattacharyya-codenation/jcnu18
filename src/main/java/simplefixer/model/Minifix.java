package simplefixer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "minifixes")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
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

    @Override
    public String toString() {
        return String.format("fixId : %d issueId : %d fileName : %s" +
                            "line : %d column : %d isFixed : %b" +
                            "sandboxURL : %s, s3Link : %s",
                            id, issueType.getId(), fileName, lineNumber, columnNumber, isFixed, sandBoxURL, s3Link);
    }
}
