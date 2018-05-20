package simplefixer.controller;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.neo4j.driver.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import simplefixer.model.IssueType;
import simplefixer.model.Minifix;
import simplefixer.repository.IssueTypeRepository;
import simplefixer.repository.MinifixRepository;

import java.io.IOException;
import java.util.*;

// Serializer class for post response
class PostResponse {
    private Integer statusCode;
    private String statusMessage;
    private Integer fixId;

    PostResponse(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = -1;
    }

    PostResponse(Integer statusCode, String statusMessage, Integer fixId) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = fixId;
    }
}

class IssueResponse {
    private Integer id;
    private Integer issueTypeId;
    private String issueTypeDesc;
    private String fileName;
    private Integer lineNumber;
    private Integer columnNumber;
    private Boolean isFixed;

    IssueResponse(Integer id, Integer issueTypeId, String issueTypeDesc,String fileName, Integer lineNumber,Integer columnNumber, Boolean isFixed) {
        this.id = id;
        this.issueTypeId = issueTypeId;
        this.issueTypeDesc = issueTypeDesc;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.isFixed = isFixed;
    }
}

// Serializer class for get response
class GetResponse {
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

    GetResponse(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    GetResponse(Integer statusCode, String statusMessage, Integer fixId, String s3Link, List<Minifix> minifixes) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.fixId = fixId;
        this.s3Link = s3Link;
        this.issueResponses = buildIssueResponses(minifixes);
    }
}

class CodeGenPostRequest {
    private String url;
    private String username;
    private String password;
    private Set<Integer> fileIds;

    CodeGenPostRequest(String url, String username,String password, Set<Integer> fileIds) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.fileIds = fileIds;
    }
}

class CodeGenPostResponse {
    private Integer id;

    CodeGenPostResponse(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}

// Serializer class for codegen response
class CodeGenGetResponse {
    private String status;
    private String Url;

    CodeGenGetResponse(String status, String s3Link) {
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

@RestController
public class MinifixController {

    private Driver driver;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String CODEGEN_POST_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/";
    private static final String CODEGEN_GET_URL = "http://codegen-cnu.ey.devfactory.com/api/codegen/status/";
    private static final String USERNAME = "neo4j";
    private static final String PASSWORD = "password";

    // helper method to create neo4j database driver
    private void createNeo4jDriver(String boltURL) {
        String username = "neo4j";
        String password = "password";
        Config noSSL = Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig();
        driver = GraphDatabase.driver(boltURL, AuthTokens.basic(username, password),noSSL);
    }

    // helper method to get neo4j query by issue id
    private String getQueryString(Integer issueId) {
        switch(issueId) {
            case 1: return "MATCH (file:File)-[:DEFINE]->(:TypeDeclaration {entity_type:\"class\"})-[:member]-> (m:MethodDeclaration) " +
                    "WHERE NOT m.modifiers =~ \"\\\\[(public|private|protected)?(, )?(abstract)?(, )?(static)?(, )?(final)?(, )?(synchronized)?\\\\]\" " +
                    "return id(m) AS methodId ,m.modifiers AS modifiers, m.file AS file, m.line AS line, m.col AS col, id(file) AS fileId";

            case 2: return "MATCH (n) RETURN n LIMIT 5";

            case 3: return "MATCH (n) RETURN n LIMIT 5";
        }
        return null;
    }

    // helper method to return query string for modifier order check
    private String getModifierFixQuery(Integer methodId, String updatedModifier) {
        return String.format("MATCH (m:MethodDeclaration) WHERE id(m) = %d SET m.modifiers = \"%s\"", methodId, updatedModifier);
    }

    // helper method to fix modifier order for method declaration
    private String getUpdatedModifierOrder(String currentModifiers) {
        String updatedModifiers = "";
        if (currentModifiers.contains("public")) {
            updatedModifiers += "public, ";
        }
        else if (currentModifiers.contains("private")) {
            updatedModifiers += "private, ";
        }
        else if (currentModifiers.contains("protected")) {
            updatedModifiers += "protected, ";
        }

        if (currentModifiers.contains("abstract")) {
            updatedModifiers += "abstract, ";
        }

        if (currentModifiers.contains("static")) {
            updatedModifiers += "static, ";
        }

        if (currentModifiers.contains("final")) {
            updatedModifiers += "final, ";
        }

        if (currentModifiers.contains("synchronized")) {
            updatedModifiers += "synchronized, ";
        }

        if (updatedModifiers.length() > 2) {
            updatedModifiers = updatedModifiers.substring(0, updatedModifiers.length() - 2);
        }

        updatedModifiers = "[" + updatedModifiers + "]";
        return updatedModifiers;
    }

    // helper method to send HTTP Post request to CodeGen API on updated Codegraph
    private String makeCodeGenPostRequest(String url, CodeGenPostRequest requestObject) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String json = new Gson().toJson(requestObject);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // helper method to send HTTP Get request to CodeGen API to fetch status
    private String makeCodeGenGetRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Autowired
    private MinifixRepository minifixRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @PostMapping(value = "/fix", produces = "application/json")
    public @ResponseBody String handleIssues(@RequestParam Integer issueId, @RequestParam String sandBoxURL) {
        Gson gson = new Gson();
        createNeo4jDriver(sandBoxURL);
        Integer fixId;

        try
        {
            Session session = driver.session();
            String queryString = getQueryString(issueId);

            // Check for valid issueId
            if (queryString == null) {
                return gson.toJson(
                        new PostResponse(400, "Invalid IssueId")
                );
            }

            // Run query on Codegraph
            List<Record> result = session.run(queryString).list();

            IssueType issueType = issueTypeRepository.findById(issueId).get();
            Set<Integer> fileIds = new HashSet<>();

            // Iterate over retrieved nodes to get fileIds for CodeGen
            for (Record record : result) {
                // Store distinct fileIds in a Set
                fileIds.add(record.get("fileId").asInt());
            }

            // Send request to CodeGen API with fileIds
            String jsonResponse = makeCodeGenPostRequest(
                    CODEGEN_POST_URL,
                    new CodeGenPostRequest(
                            sandBoxURL,
                            USERNAME,
                            PASSWORD,
                            fileIds
                    )
            );

            // Deserialize JSON response
            fixId = new Gson().fromJson(jsonResponse, CodeGenPostResponse.class).getId();

            // Iterate over retrieved nodes
            for (Record record : result) {
                // System.out.println(record.get("modifiers").asString());

                Minifix minifix = new Minifix()
                        .setIssueType(issueType)
                        .setSandBoxURL(sandBoxURL)
                        .setFileName(record.get("file").asString())
                        .setLineNumber(record.get("line").asInt())
                        .setColumnNumber(record.get("col").asInt())
                        .setFixId(fixId);

                // Update Codegraph to fix issue
                if (issueId == 1) {
                    String fixModifierQuery = getModifierFixQuery(
                            record.get("methodId").asInt(),
                            getUpdatedModifierOrder(record.get("modifiers").asString())
                    );
                    session.run(fixModifierQuery);
                }

                // Set isFixed
                minifix = minifix.setIsFixed();

                // Add record to SQL database
                minifixRepository.save(minifix);
            }

            // Close driver and session
            session.close();
            driver.close();
        }
        catch (Exception e) {
            driver.close();
            e.printStackTrace();
            return gson.toJson(
                    new PostResponse(500, e.toString())
            );
        }

        return gson.toJson(
                new PostResponse(200, "OK", fixId)
        );
    }

    @GetMapping(value = "/fix/{fixId}", produces = "application/json")
    public @ResponseBody String getIssueStatus(@PathVariable(value="fixId") Integer fixId) {
        Gson gson = new Gson();
        List<Minifix> minifixes;
        minifixes = minifixRepository.findAllByFixId(fixId);

        // Check for valid fixId
        if (minifixes.isEmpty()) {
            return gson.toJson(
                    new GetResponse(400, "FixId not found")
            );
        }

        CodeGenGetResponse response;
        // Hit CodeGen API to get status
        try {
            String jsonResponse = makeCodeGenGetRequest(CODEGEN_GET_URL + fixId.toString());
            response = new Gson().fromJson(jsonResponse, CodeGenGetResponse.class);
        }
        catch (IOException e) {
            return gson.toJson(
                    new GetResponse(500, e.toString())
            );
        }

        // Update s3link in SQL database
        minifixRepository.updateS3LinkByFixId(response.getUrl(), fixId);
        return gson.toJson(
                new GetResponse(200, response.getStatus(), fixId, response.getUrl(), minifixes)
        );
    }
}
