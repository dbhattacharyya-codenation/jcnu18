package categorizer.constant;

import java.util.List;

public final class CypherQuery {
    public static String GET_NODES_FROM_NODE_ID_MAPPINGS_QUERY(List<List<Long>> nodeIdMappings) {
        String nodeIdArray = "";
        for(List<Long> nodeIdMapping : nodeIdMappings) {
            for(Long nodeId : nodeIdMapping) {
                nodeIdArray = nodeIdArray + nodeId.toString() + ",";
            }
        }
        nodeIdArray = "[" + nodeIdArray.substring(0,nodeIdArray.length()-1) + "]";
        String query = String.format("MATCH (n) WHERE id(n) IN %s RETURN id(n) AS id, n", nodeIdArray);
        return query;
    }
}
