package simplefixer.helper;

// Serializer class for codegen response
public class CodeGenGetResponse {
    private String status;
    private String Url;

    public CodeGenGetResponse(String status, String s3Link) {
        this.status = status;
        this.Url = s3Link;
    }

    public String getStatus() {
        return status;
    }

    public String getUrl() {
        return Url;
    }
}
