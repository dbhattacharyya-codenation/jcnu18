package simplefixer.constant;

public class CypherQuery {
    public static final String FIND_INVALID_MODIFIERS_QUERY = "MATCH (file:File)-[:DEFINE]->(:TypeDeclaration {entity_type:\"class\"})-[:member]-> (m:MethodDeclaration) " +
            "WHERE NOT m.modifiers =~ \"\\\\[(public|private|protected)?(, )?(abstract)?(, )?(static)?(, )?(final)?(, )?(transient)?(, )?(volatile)?(, )?(synchronized)?(, )?(native)?(, )?(strictfp)?\\\\]\" " +
            "return id(m) AS methodId ,m.modifiers AS modifiers, m.file AS file, m.line AS line, m.col AS col, id(file) AS fileId";

    public static final String FIND_UNSYNCHRONIZED_STATIC_INITIALIZATIONS_QUERY = "MATCH (instance:SimpleName)<-[:SETS]-(method:MethodDeclaration)<-[:member]-" +
            "(class:TypeDeclaration {entity_type:\"class\"})-[:member]->(field:FieldDeclaration)-[:fragment]->" +
            "(:VariableDeclarationFragment)-[:SET_BY]->(instance:SimpleName) " +
            "WHERE field.modifiers CONTAINS \"static\" AND " +
            "NOT field.modifiers CONTAINS \"final\" AND " +
            "NOT method.modifiers CONTAINS \"synchronized\" " +
            "WITH method,class " +
            "MATCH (file:File)-[:DEFINE]->(class) " +
            "RETURN id(method) AS methodId, method.modifiers AS modifiers, method.file AS file, method.line AS line, method.col AS col, id(file) AS fileId";

}
