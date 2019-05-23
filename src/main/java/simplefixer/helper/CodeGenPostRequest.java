package simplefixer.helper;

import java.util.Set;

public class CodeGenPostRequest {
    private String url;
    private String username;
    private String password;
    private Set<Integer> fileIds;

    public CodeGenPostRequest(String url, String username,String password, Set<Integer> fileIds) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.fileIds = fileIds;
    }
}