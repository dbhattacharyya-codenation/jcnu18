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
            issueResponses.add(IssueResponse.builder()
                    .id(minifix.getId())
                    .issueTypeId(minifix.getIssueType().getId())
                    .issueTypeDesc(minifix.getIssueType().getIssueDescription())
                    .fileName(minifix.getFileName())
                    .lineNumber(minifix.getLineNumber())
                    .columnNumber(minifix.getColumnNumber())
                    .isFixed(minifix.getIsFixed())
                    .build()
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
