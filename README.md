- Find nodes for classes and interfaces.

```cypher
MATCH (n:TypeDeclaration)
WHERE n.entity_type IN ["class", "interface"]
RETURN n.entity_type, n.simplename;
```

- Find all classes of a package.

```cypher
MATCH (:PackageDeclaration {simplename:"filter"}) -[:CONTAIN]->
(c:TypeDeclaration {entity_type:"class"})
RETURN c.simplename;
```

- Find methods of a class both inherited and declared ones.

> Query using `UNION` operator
```cypher
MATCH (declaredMethod:MethodDeclaration)<-[:member]-(c:TypeDeclaration {entity_type:"class",simplename:"ElfWarlord"})
RETURN c, declaredMethod as accessibleMethod

UNION ALL

MATCH (c:TypeDeclaration {entity_type:"class",simplename:"ElfWarlord"})-[:EXTENDS*1..]->(:TypeDeclaration {entity_type:"class"})-[:member]->(inheritedMethod:MethodDeclaration {isConstructor:"False"})
WHERE NOT inheritedMethod.modifiers CONTAINS "private"
WITH c, inheritedMethod
WHERE not exists((inheritedMethod)-[:OVERRIDDEN_BY]->())
RETURN c, inheritedMethod as accessibleMethod;
```

> Updated query without `UNION`, thereby reduced redundant check for class
```cypher
MATCH path=(c:TypeDeclaration {entity_type:"class",simplename:"AccountCreateEvent"})-[:EXTENDS*0..50]->(:TypeDeclaration {entity_type:"class"})-[:member]->(method:MethodDeclaration)
WHERE EXISTS((c)-[:member]->(method)) OR
((method.isConstructor = "False") AND (NOT method.modifiers CONTAINS "private")) AND
NOT EXISTS((method)-[:OVERRIDDEN_BY]->(:MethodDeclaration))
return path;
```

- Find transitive closure of types (find all ancestors of a type).

```cypher
MATCH (c:TypeDeclaration {entity_type:"class", simplename:"ElfWarlord"})-[:EXTENDS | :IMPLEMENTS*]->(p:TypeDeclaration) RETURN c,p;
```


- Find If statements where the condition is boolean (true/false) value. Eg: if(false){}

```cypher
MATCH (ifstmt:IfStatement)-[c:condition]->(bl:BooleanLiteral)
RETURN ifstmt,c,bl;
```

- Find If statements where the condition is not boolean value (an expression).

```cypher
MATCH (ifstmt:IfStatement)-[c:condition]->(expr)
WHERE NOT "BooleanLiteral" IN labels(expr)
RETURN ifstmt,c,expr;
```

- Find package of a given class.

```cypher
MATCH (p:PackageDeclaration)<-[:CONTAIN_IN]-(:TypeDeclaration {simplename:"App",entity_type:"class"}) RETURN p;
```

- Find total cyclomatic complexity of a class as sum of cyclomatic complexity of each method that belongs to it, the methods include both inherited and derived.

```cypher
MATCH path=(c:TypeDeclaration {entity_type:"class",simplename:"AccountCreateEvent"})-[:EXTENDS*0..50]->(:TypeDeclaration {entity_type:"class"})-[:member]->(method:MethodDeclaration)
WHERE EXISTS((c)-[:member]->(method)) OR
((method.isConstructor = "False") AND (NOT method.modifiers CONTAINS "private")) AND
NOT EXISTS((method)-[:OVERRIDDEN_BY]->(:MethodDeclaration))
RETURN sum(toInteger(method.Cyclomatic)) AS cycloSum;
```


- Find all methods with 10 or more statements.

```cypher
MATCH (m:MethodDeclaration) WHERE toInteger(m.CountStmt) >= 10 RETURN m;
```

- Find all methods with 4 or more parameters.

```cypher
MATCH (m:MethodDeclaration)-[:parameter]->(p:SingleVariableDeclaration)
WITH m, count(p) as param_count, collect(p) as params
WHERE param_count >= 4
RETURN m,param_count,params;
```


- Find all methods with 50 or more lines of code.

```cypher
MATCH (m:MethodDeclaration) WHERE toInteger(m.CountLineCode) >= 50 RETURN m;
```

