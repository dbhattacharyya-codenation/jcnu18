package simplefixer.model;

import javax.persistence.*;

@Entity
@Table(name = "issue_types")
public class IssueType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String issueDescription;

    public IssueType() {}

    public Integer getId() {
        return id;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    @Override
    public String toString() {
        return String.format("IssueId : %d  IssueDescription : %s", id, issueDescription);
    }
}
