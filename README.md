- Find all Singleton classes

```cypher
MATCH (:SimpleType {name:toString(class.simplename)}) <-[:return]-(getInstanceMethod:MethodDeclaration {isConstructor:"False"})
<-[:member]-(class:TypeDeclaration {entity_type:"class"})-[:member]->(constructor:MethodDeclaration {isConstructor:"True"})
WHERE
constructor.modifiers CONTAINS "private" AND
getInstanceMethod.modifiers CONTAINS "public" AND getInstanceMethod.modifiers CONTAINS "static"
WITH class
MATCH (class)-[:member]->(singleInstance:FieldDeclaration) -[:type]->(:SimpleType {name:toString(class.simplename)})
WHERE singleInstance.modifiers CONTAINS "private" AND
singleInstance.modifiers CONTAINS "static"
RETURN class;
```


- Find all Builder patterns

```cypher
MATCH (:SimpleType {name:toString(nestedBuilderClass.simplename)})<-[:type]-(:SingleVariableDeclaration)
<-[:parameter]-(constructor:MethodDeclaration {isConstructor:"True"})<-[:member]-(class:TypeDeclaration {entity_type:"class"})
-[:member]->(nestedBuilderClass:TypeDeclaration {entity_type:"class"})-[:member]->(buildMethod:MethodDeclaration)
-[:return]->(:SimpleType {name:toString(class.simplename)})
WHERE nestedBuilderClass.modifiers CONTAINS "static" AND
nestedBuilderClass.modifiers CONTAINS "public" AND
constructor.modifiers CONTAINS "private"
WITH class, nestedBuilderClass
MATCH (class)-[:member]->(:FieldDeclaration)-[:type]->(classFieldType:SimpleType)
WITH class, nestedBuilderClass, collect(classFieldType.name) AS classFieldType
MATCH (nestedBuilderClass)-[:member]->(:FieldDeclaration)-[:type]->(builderFieldType:SimpleType)
WHERE builderFieldType.name IN classFieldType
RETURN class, nestedBuilderClass;
```
