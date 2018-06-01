package categorizer;

import categorizer.constant.Category;
import categorizer.helper.IssueData;
import categorizer.helper.IssueLocation;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import simplefixer.utils.DbConnection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class VariableInfo {
    private String file;
    private Integer lineNumber;
    private Integer columnNumber;
    private String variableName;

    VariableInfo(String file, Integer lineNumber, Integer columnNumber, String variableName) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.variableName = variableName;
    }
}

class Response {
    private Integer statusCode;
    private String statusMessage;
    private List<VariableInfo> variables;

    Response(Integer statusCode, String statusMessage, List<VariableInfo> variables) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.variables = variables;
    }
}

@AllArgsConstructor @NoArgsConstructor
@Builder
public class Categorizer {
    private String boltURL;
    private String file1;
    private Integer startLine1;
    private Integer endLine1;

    private String file2;
    private Integer startLine2;
    private Integer endLine2;

    private Session session;

    private static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    private boolean isAstEdge(String edgeName){
        return ('a' <= edgeName.charAt(0) && edgeName.charAt(0) <= 'z') ;
    }

    private String getEdgesHash(Node node, Session session, Boolean isFile1) {
        String queryEdges;
        String hash = "";
        if (isFile1) {
            queryEdges = String.format(
                    "MATCH (n)-[rel]-(m {file:\"%s\"}) WHERE id(n) = %d AND m.line >= %d AND m.endLine <= %d RETURN type(rel) AS rel",
                    file1, node.id(), startLine1, endLine1
            );
        }
        else {
            queryEdges = String.format(
                    "MATCH (n)-[rel]-(m {file:\"%s\"}) WHERE id(n) = %d AND m.line >= %d AND m.endLine <= %d RETURN type(rel) AS rel",
                    file2, node.id(), startLine2, endLine2
            );
        }

        StatementResult result = session.run(queryEdges);
        List<String> edgeList = new ArrayList<>();
        while (result.hasNext()) {
            String edgeName = result.next().get("rel").asString();
            if (isAstEdge(edgeName)) {
                edgeList.add(edgeName);
            }
        }

        Collections.sort(edgeList);

        for (String edgeName : edgeList) {
            try {
                hash = hash + sha1(edgeName);
                hash = sha1(hash);
            }
            catch (NoSuchAlgorithmException e) {
                System.err.println("Error calculating SHA1");
            }
        }

        return hash;
    }

    private String getNodeHash(Node node) {
        List<String> labelNames = new ArrayList<>();
        Iterator<String> it = node.labels().iterator();
        while(it.hasNext()) {
            String label = it.next();
            labelNames.add(label);
        }

        Collections.sort(labelNames);

        String hash = "";
        for (String label : labelNames) {
            try {
                hash = hash + sha1(label);
                hash = sha1(hash);
            }
            catch (NoSuchAlgorithmException e) {
                System.err.println("Error calculating SHA1");
            }
        }

        List<String> excludedProperties = new ArrayList<>();
        excludedProperties.add("id");
        excludedProperties.add("line");
        excludedProperties.add("endLine");
        excludedProperties.add("col");
        excludedProperties.add("longname");
        excludedProperties.add("file");

        if (node.hasLabel("SimpleName") || node.hasLabel("StringLiteral") || node.hasLabel("NumberLiteral") || node.hasLabel("BooleanLiteral")) {
            excludedProperties.add("name");
        }
        if (node.hasLabel("VariableDeclarationFragment")) {
            excludedProperties.add("simplename");
        }
        if (node.hasLabel("SingleVariableDeclaration")) {
            excludedProperties.add("name");
            excludedProperties.add("simplename");
        }

        Iterator<String> iter = node.keys().iterator();
        List<String> propertyList = new ArrayList<>();

        whileLabel:
        while (iter.hasNext()) {
            String key = iter.next();

            for(String excludedProperty : excludedProperties) {
                if (key.equals(excludedProperty)) {
                    break whileLabel;
                }
            }

            String value = node.get(key).toString();
            propertyList.add(key + value);
        }

        Collections.sort(propertyList);

        for (String property : propertyList) {
            try {
                hash = hash + sha1(property);
                hash = sha1(hash);
            }
            catch (NoSuchAlgorithmException e) {
                System.err.println("Error calculating SHA1");
            }
        }

        return hash;
    }

    private String getAllEdgehash(Node n, Node m) {
        String query = String.format("MATCH (n)-[rel]->(m) WHERE id(n)=%s AND id(m)=%s RETURN type(rel) as rel", n.id(), m.id());
        String hash = "";
        StatementResult result = session.run(query);
        List<String> edgeList = new ArrayList<>();

        while (result.hasNext()) {
            String edgeName = result.next().get("rel").asString();
            if (isAstEdge(edgeName)) {
                edgeList.add(edgeName);
            }
        }

        Collections.sort(edgeList);

        for (String edgeName : edgeList) {
            try {
                hash = hash + sha1(edgeName);
                hash = sha1(hash);
            }
            catch (NoSuchAlgorithmException e) {
                System.err.println("Error calculating SHA1");
            }
        }

        return hash;
    }

    private String getSubtreeHash(Node node) {
        String hash = getNodeHash(node);
        try {
            String query = String.format("MATCH (n)-[:tree_edge]->(m) WHERE id(n) = %d RETURN m", node.id());
            StatementResult result = session.run(query);
            while (result.hasNext()) {
                Node v = result.next().get("m").asNode();
                String childHash = getSubtreeHash(v);
                childHash = childHash + getAllEdgehash(node, v);

                try {
                    childHash = sha1(childHash);
                    hash = hash + childHash;
                    hash = sha1(hash);
                }
                catch (NoSuchAlgorithmException e) {
                    System.err.println(e.toString());
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
        return hash;
    }

    public String testMethod() {
        boolean isCategory1 = true;

        Gson gson = new Gson();
        List<VariableInfo> variables = new ArrayList<>();
        try (DbConnection conn = new DbConnection(boltURL)) {
            try (Session session = conn.getDriver().session()) {
                String query1 = String.format(
                        "MATCH (n {file:\"%s\"}) WHERE n.line >= %d AND n.endLine <= %d RETURN n",
                        file1, startLine1, endLine1
                );

                StatementResult subgraph1 = session.run(query1);

                String query2 = String.format(
                        "MATCH (n {file:\"%s\"}) WHERE n.line >= %d AND n.endLine <= %d RETURN n",
                        file2, startLine2, endLine2
                );

                StatementResult subgraph2 = session.run(query2);

                while (subgraph1.hasNext() && subgraph2.hasNext()) {

                    Node node1 = subgraph1.next().get("n").asNode();
                    Node node2 = subgraph2.next().get("n").asNode();

                    if (!getNodeHash(node1).equals(getNodeHash(node2)) || !getEdgesHash(node1, session, true).equals(getEdgesHash(node2, session, false))) {
                        isCategory1 = false;
                        break;
                    }


                    if (node1.hasLabel("SimpleName") && node2.hasLabel("SimpleName") && !node1.get("name").asString().equals(node2.get("name").asString())) {
                        variables.add(new VariableInfo(
                                node1.get("file").asString(),
                                node1.get("line").asInt(),
                                node1.get("col").asInt(),
                                node1.get("name").asString()
                        ));
                        variables.add(new VariableInfo(
                                node2.get("file").asString(),
                                node2.get("line").asInt(),
                                node2.get("col").asInt(),
                                node2.get("name").asString()
                        ));
                    }
                }
                if (subgraph1.hasNext() || subgraph2.hasNext()) {
                    isCategory1 = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(new Response(500, e.toString(), null));
            }
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }

        if (!isCategory1) {
            return gson.toJson(new Response(200, "Not category1", null));
        }

        return gson.toJson(new Response(200, "Success", variables));
    }

    private <T> List<List<T>> transpose(List<List<T>> table) {
        List<List<T>> ret = new ArrayList<List<T>>();
        final int N = table.get(0).size();
        for (int i = 0; i < N; i++) {
            List<T> col = new ArrayList<T>();
            for (List<T> row : table) {
                col.add(row.get(i));
            }
            ret.add(col);
        }
        return ret;
    }

    private boolean isContiguousCodeSegment(IssueData issueData) {
        List<List<Long>> transposeMappings = transpose(issueData.getNodeIdMappings());
        Integer fileNumber = 0;

        for (IssueLocation issueLocation : issueData.getIssueLocations()) {
            String query = String.format("MATCH (n {file:\"%s\"}) WHERE n.line >= %d AND n.endLine <= %d AND ANY(label IN labels(n) WHERE label =~ \".*Statement\") RETURN id(n) AS id ORDER BY n.line", issueLocation.getFileName(), issueLocation.getStartLine(),issueLocation.getEndLine());
            List<Long> orderedIDs = session.run(query).list(record -> record.get("id").asLong());
            if(!orderedIDs.equals(transposeMappings.get(fileNumber))) {
                return false;
            }
            fileNumber++;
        }
        return true;
    }

    private boolean isDiffVariableNamesCategory(Driver driver, IssueData issueData) {
        try (Session session = driver.session()) {
            this.session = session;

            if (!isContiguousCodeSegment(issueData)) {
                return false;
            }

            for (List<Long> nodeIdMapping : issueData.getNodeIdMappings()) {
                Set<String> subtreeHashes = new HashSet<>();
                for(Long nodeId : nodeIdMapping) {
                    StatementResult result = session.run(String.format("MATCH (n) WHERE id(n) = %d RETURN n", nodeId));
                    if (result.hasNext()) {
                        subtreeHashes.add(getSubtreeHash(result.next().get("n").asNode()));
                    }
                    if (subtreeHashes.size() > 1) {
                        return false;
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.toString());
            throw e;
        }
        return true;
    }

    public Category getCategory(Driver driver, IssueData issueData) {
        for (Category category : Category.values()) {
            switch (category) {
                case DIFF_VARNAMES_AND_LITERALS:
                    if (isDiffVariableNamesCategory(driver, issueData)) {
                        return Category.DIFF_VARNAMES_AND_LITERALS;
                    }
                    break;
            }
        }
        return Category.NOT_RESOLVABLE;
    }
}
