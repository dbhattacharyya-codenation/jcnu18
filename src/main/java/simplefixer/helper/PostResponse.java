package simplefixer.helper;

public class PostResponse {
    private Integer statusCode;
    private String statusMessage;
    private Integer fixId;

    public PostResponse(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = -1;
    }

    public PostResponse(Integer statusCode, String statusMessage, Integer fixId) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = fixId;
    }
}
