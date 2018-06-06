package simplefixer.controller;

import categorizer.Categorizer;
import categorizer.constant.Category;
import categorizer.helper.*;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.neo4j.driver.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import simplefixer.model.IssueType;
import simplefixer.model.Minifix;
import simplefixer.repository.IssueTypeRepository;
import simplefixer.repository.MinifixRepository;
import simplefixer.helper.*;
import simplefixer.constant.Constants;
import simplefixer.constant.CypherQuery;
import simplefixer.utils.DbConnection;

import java.io.IOException;
import java.util.*;

@RestController
public class MinifixController {

    // helper method to get neo4j query by issue id
    private String getQueryString(Integer issueId) throws InvalidIssueIdException {
        switch(issueId) {
            case 1: return CypherQuery.FIND_INVALID_MODIFIERS_QUERY;
            case 2: return "MATCH (n) RETURN n LIMIT 5";

            case 3: return CypherQuery.FIND_UNSYNCHRONIZED_STATIC_INITIALIZATIONS_QUERY;
        }
        throw new InvalidIssueIdException();
    }

    // helper method to fix modifier order for method declaration
    private String getUpdatedModifierOrder(String currentModifiers) {
        String updatedModifiers = "";

        for (String modifier : Constants.MODIFIERS_LIST) {
            if (currentModifiers.contains(modifier)) {
                updatedModifiers = updatedModifiers + modifier + ", ";
            }
        }

        if (!StringUtils.isEmpty(updatedModifiers)) {
            updatedModifiers = updatedModifiers.substring(0, updatedModifiers.length() - 2);
        }

        return "[" + updatedModifiers + "]";
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
        Integer fixId = -1;
        try (DbConnection conn = new DbConnection(sandBoxURL)) {

            try (Session session = conn.getDriver().session()) {

                String queryString;
                try {
                    queryString = getQueryString(issueId);
                } catch (InvalidIssueIdException e) {
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
                        String fixModifierQuery = CypherQuery.GET_MODIFIER_FIX_QUERY(
                                record.get("methodId").asInt(),
                                getUpdatedModifierOrder(record.get("modifiers").asString())
                        );
                        session.run(fixModifierQuery);
                    } else if (issueId == 3) {
                        String addSynchronizedToModifierQuery = CypherQuery.GET_MODIFIER_FIX_QUERY(
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

                    Minifix minifix = Minifix.builder()
                            .issueType(issueType)
                            .sandBoxURL(sandBoxURL)
                            .fileName(record.get("file").asString())
                            .lineNumber(record.get("line").asInt())
                            .columnNumber(record.get("col").asInt())
                            .fixId(fixId)
                            .isFixed(true)
                            .build();

                    // Add record to SQL database
                    minifixRepository.save(minifix);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(
                        new PostResponse(500, e.toString())
                );
            }
        }
        catch (Exception e) {
            System.out.println(e.toString());
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

    @PostMapping(value = "/testdup", produces = "application/json")
    public String testDuplicate(
            @RequestParam String boltURL,
            @RequestParam String file1, @RequestParam Integer startLine1, @RequestParam Integer endLine1,
            @RequestParam String file2, @RequestParam Integer startLine2, @RequestParam Integer endLine2
    ) {
        return Categorizer.builder()
                .boltURL(boltURL)
                .file1(file1)
                .startLine1(startLine1)
                .endLine1(endLine1)
                .file2(file2)
                .startLine2(startLine2)
                .endLine2(endLine2)
                .build()
                .testMethod();
    }

    @PostMapping(value = "/categorize")
    @ResponseBody
    public CategorizerResponse testRoute(@RequestBody FinderRequest finderRequest) {
        Sandbox sandbox = finderRequest.getSandbox();
        List<IssueData> issueDataList = finderRequest.getIssuesData();
        List<IssueCategory> issueCategories = new ArrayList<>();

        try (DbConnection conn = new DbConnection(sandbox.getBoltUrl(), sandbox.getUsername(), sandbox.getPassword())) {
            for (IssueData issueData : issueDataList) {
                Category category  = new Categorizer().getCategory(conn.getDriver(), issueData);
                issueCategories.add(new IssueCategory(issueData.getId(), category, issueData.getIssueLocations()));
            }
        }
        catch (Exception e) {
            System.err.println(e.toString());
            return new CategorizerResponse(500, e.toString(), null);
        }
        return new CategorizerResponse(200, "Success", issueCategories);
    }
}
