package simplefixer.controller;

import com.google.gson.Gson;
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
import simplefixer.helper.*;
import simplefixer.constant.Constants;

import java.io.IOException;
import java.util.*;

@RestController
public class MinifixController {

    private Driver driver;

    // helper method to create neo4j database driver
    private void createNeo4jDriver(String boltURL) {
        Config noSSL = Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig();
        driver = GraphDatabase.driver(boltURL, AuthTokens.basic(Constants.USERNAME, Constants.PASSWORD),noSSL);
    }

    // helper method to get neo4j query by issue id
    private String getQueryString(Integer issueId) {
        switch(issueId) {
            case 1: return "MATCH (file:File)-[:DEFINE]->(:TypeDeclaration {entity_type:\"class\"})-[:member]-> (m:MethodDeclaration) " +
                    "WHERE NOT m.modifiers =~ \"\\\\[(public|private|protected)?(, )?(abstract)?(, )?(static)?(, )?(final)?(, )?(transient)?(, )?(volatile)?(, )?(synchronized)?(, )?(native)?(, )?(strictfp)?\\\\]\" " +
                    "return id(m) AS methodId ,m.modifiers AS modifiers, m.file AS file, m.line AS line, m.col AS col, id(file) AS fileId";

            case 2: return "MATCH (n) RETURN n LIMIT 5";

            case 3: return "MATCH (instance:SimpleName)<-[:SETS]-(method:MethodDeclaration)<-[:member]-" +
                    "(class:TypeDeclaration {entity_type:\"class\"})-[:member]->(field:FieldDeclaration)-[:fragment]->" +
                    "(:VariableDeclarationFragment)-[:SET_BY]->(instance:SimpleName) " +
                    "WHERE field.modifiers CONTAINS \"static\" AND " +
                    "NOT field.modifiers CONTAINS \"final\" AND " +
                    "NOT method.modifiers CONTAINS \"synchronized\" " +
                    "WITH method,class " +
                    "MATCH (file:File)-[:DEFINE]->(class) " +
                    "RETURN id(method) AS methodId, method.modifiers AS modifiers, method.file AS file, method.line AS line, method.col AS col, id(file) AS fileId";
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

        for (String modifier : Constants.MODIFIERS_LIST) {
            if (currentModifiers.contains(modifier)) {
                updatedModifiers = updatedModifiers + modifier + ", ";
            }
        }

        if (updatedModifiers.length() > 2) {
            updatedModifiers = updatedModifiers.substring(0, updatedModifiers.length() - 2);
        }

        updatedModifiers = "[" + updatedModifiers + "]";
        return updatedModifiers;
    }

    // helper method to add synchronized to modifiers for lazy initialization of static fields
    private String addSynchronizedToModifiers(String currentModifiers) {
        String updatedModifiers = currentModifiers.substring(0,currentModifiers.length() - 1) +
                ", synchronized]";
        return getUpdatedModifierOrder(updatedModifiers);
    }

    // helper method to send HTTP Post request to CodeGen API on updated Codegraph
    private String makeCodeGenPostRequest(String url, CodeGenPostRequest requestObject) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String json = new Gson().toJson(requestObject);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(Constants.JSON, json);
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

                // Update Codegraph to fix issue
                if (issueId == 1) {
                    String fixModifierQuery = getModifierFixQuery(
                            record.get("methodId").asInt(),
                            getUpdatedModifierOrder(record.get("modifiers").asString())
                    );
                    session.run(fixModifierQuery);
                }
                else if (issueId == 3) {
                    String addSynchronizedToModifierQuery = getModifierFixQuery(
                            record.get("methodId").asInt(),
                            addSynchronizedToModifiers(record.get("modifiers").asString())
                    );
                    session.run(addSynchronizedToModifierQuery);
                }
            }

            // Send request to CodeGen API with fileIds
            String jsonResponse = makeCodeGenPostRequest(
                    Constants.CODEGEN_POST_URL,
                    new CodeGenPostRequest(
                            sandBoxURL,
                            Constants.USERNAME,
                            Constants.PASSWORD,
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
                        .setFixId(fixId)
                        .setIsFixed();

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
                    new GetResponse(404, "FixId not found")
            );
        }

        CodeGenGetResponse response;
        // Hit CodeGen API to get status
        try {
            String jsonResponse = makeCodeGenGetRequest(Constants.CODEGEN_GET_URL + fixId.toString());
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
