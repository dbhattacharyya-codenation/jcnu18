package simplefixer.helper;

import simplefixer.model.Minifix;

import java.util.ArrayList;
import java.util.List;

// Serializer class for get response
public class GetResponse {
    private Integer statusCode;
    private String statusMessage;
    private Integer fixId;
    private String s3Link;
    List<IssueResponse> issueResponses;

    private List<IssueResponse> buildIssueResponses(List<Minifix> minifixes) {
        List<IssueResponse> issueResponses = new ArrayList<>();
        for (Minifix minifix : minifixes) {
            issueResponses.add(new IssueResponse(
                    minifix.getId(),
                    minifix.getIssueType().getId(),
                    minifix.getIssueType().getIssueDescription(),
                    minifix.getFileName(),
                    minifix.getLineNumber(),
                    minifix.getColumnNumber(),
                    minifix.getIsFixed())
            );
        }
        return issueResponses;
    }

    public GetResponse(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public GetResponse(Integer statusCode, String statusMessage, Integer fixId, String s3Link, List<Minifix> minifixes) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = fixId;
        this.s3Link = s3Link;
        this.issueResponses = buildIssueResponses(minifixes);
    }
}
