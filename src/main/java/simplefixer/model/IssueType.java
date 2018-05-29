package simplefixer.model;

import lombok.Getter;

import javax.persistence.*;

@Entity
@Table(name = "issue_types")
@Getter
public class IssueType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String issueDescription;

    public IssueType() {}

    @Override
    public String toString() {
        return String.format("IssueId : %d  IssueDescription : %s", id, issueDescription);
    }
}
