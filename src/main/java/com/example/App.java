package com.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import javassist.bytecode.analysis.ControlFlow.Block;

public class App {
    private static final String ORAM_FIELD_NAME = "oram";
    private static final String PATH_ORAM_CLASS_NAME = "PathORAM";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Please provide a file path as an argument.");
            System.exit(1);
        }

        String filePathStr = args[0];
        Path filePath = Paths.get(filePathStr);

        TypeSolver typeSolver = new ReflectionTypeSolver();

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser
                .getParserConfiguration()
                .setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(filePath));

        // Imports
        addImports(cu);

        // ORAM field and initializer
        String className = getClassNameFromPath(filePath);
        ClassOrInterfaceDeclaration classDeclaration = cu.getClassByName(className).orElseThrow();
        FieldDeclaration pathORAMFieldDeclaration = createPathORAMFieldDeclaration(10000);
        classDeclaration.addMember(pathORAMFieldDeclaration);

        // Array accesses
        ModifierVisitor<?> arrayAccessVisitor = new ArrayAccessModifier();
        arrayAccessVisitor.visit(cu, null);

        // Array methods
        ModifierVisitor<?> arrayMethodVisitor = new ArrayMethodModifier();
        arrayMethodVisitor.visit(cu, null);

        // Array initializers
        // Note: Removes array variable declarations so only call this Visitor after
        // those variable declarations are no longer needed
        ModifierVisitor<?> arrayInitializerModifier = new ArrayInitializerModifier();
        arrayInitializerModifier.visit(cu, null);

        // Args array
        // Note: This needs to be after array access modifications so that args array
        // accesses **when inserting its elements into the ORAM tree** do not get
        // modified
        Optional<MethodDeclaration> maybeMainMethodDecl = getMainMethodFromClassDecl(classDeclaration);
        if (maybeMainMethodDecl.isPresent()) {
            MethodDeclaration mainMethodDecl = maybeMainMethodDecl.get();
            BlockStmt mainMethodBody = mainMethodDecl.getBody().orElseThrow();
            ForStmt writeArgsArrayToORAMStmt = createWriteArrayToORAM("args");
            mainMethodBody.getStatements().addFirst(writeArgsArrayToORAMStmt);
        }

        System.out.println(cu.toString());
    }

    private static Optional<MethodDeclaration> getMainMethodFromClassDecl(ClassOrInterfaceDeclaration classDecl) {
        List<MethodDeclaration> methods = classDecl.getMethods();
        for (MethodDeclaration method : methods) {
            if (method.getNameAsString().equals("main") &&
                    method.isStatic() &&
                    method.getParameters().size() == 1 &&
                    method.getParameter(0).getType().asString().equals("String[]")) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private static ForStmt createWriteArrayToORAM(String arrayName) {
        ForStmt forStmt = new ForStmt();
        // int i = 0
        VariableDeclarator iEqualsZero = new VariableDeclarator(PrimitiveType.intType(), "i",
                new IntegerLiteralExpr("0"));
        NodeList<Expression> initializationExprs = NodeList
                .<Expression>nodeList(new VariableDeclarationExpr(iEqualsZero));
        forStmt.setInitialization(
                initializationExprs);

        // i < args.length
        BinaryExpr compareExpr = new BinaryExpr(
                new NameExpr("i"),
                new FieldAccessExpr(new NameExpr(arrayName), "length"),
                BinaryExpr.Operator.LESS);
        forStmt.setCompare(compareExpr);

        // i++
        UnaryExpr iIncrement = new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT);
        NodeList<Expression> updateExprs = NodeList.<Expression>nodeList(iIncrement);
        forStmt.setUpdate(updateExprs);

        // TODO: DRY
        Expression arrayAccessExpr = new ArrayAccessExpr(new NameExpr(arrayName), new NameExpr("i"));
        Expression valueByteArrayExpr = createStringToByteArrayExpr(arrayAccessExpr);
        Expression optionalValueByteArrayExpr = new MethodCallExpr(
                new NameExpr("Optional"),
                "ofNullable",
                NodeList.nodeList(valueByteArrayExpr));

        MethodCallExpr stringValueOfCall = new MethodCallExpr(null, "String.valueOf",
                NodeList.<Expression>nodeList(new NameExpr("i")));

        // "arrayName["
        StringLiteralExpr arrayNameAndOpenBracket = new StringLiteralExpr(arrayName + "[");
        // "arrayName[" + index.toString()
        BinaryExpr firstPart = new BinaryExpr(arrayNameAndOpenBracket, stringValueOfCall,
                BinaryExpr.Operator.PLUS);
        // "]"
        StringLiteralExpr closingBracketLiteral = new StringLiteralExpr("]");
        // "arrayName[" + index.toString() + "]"
        BinaryExpr blockIdExpr = new BinaryExpr(firstPart, closingBracketLiteral, BinaryExpr.Operator.PLUS);

        MethodCallExpr oramAccessMethodCall = createORAMAccessMethodCall(blockIdExpr, optionalValueByteArrayExpr, true);
        ExpressionStmt oramAccessStmt = new ExpressionStmt(oramAccessMethodCall);
        forStmt.setBody(oramAccessStmt);

        return forStmt;
    }

    private static FieldDeclaration createPathORAMFieldDeclaration(int numBlocks) {
        ClassOrInterfaceType classType = new ClassOrInterfaceType(null, PATH_ORAM_CLASS_NAME);
        ObjectCreationExpr initializer = new ObjectCreationExpr()
                .setType(PATH_ORAM_CLASS_NAME)
                .addArgument(String.valueOf(numBlocks));
        VariableDeclarator variableDeclarator = new VariableDeclarator(classType, ORAM_FIELD_NAME, initializer);
        FieldDeclaration fieldDeclaration = new FieldDeclaration()
                .addVariable(variableDeclarator)
                .addModifier(Modifier.Keyword.PRIVATE)
                .addModifier(Modifier.Keyword.STATIC);
        return fieldDeclaration;
    }

    private static void addImports(CompilationUnit cu) {
        cu.addImport(new ImportDeclaration("java.util.Optional", false, false));
        cu.addImport(new ImportDeclaration("java.nio.ByteBuffer", false, false));
    }

    private static String getClassNameFromPath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        return className;
    }

    private static MethodCallExpr createORAMAccessMethodCall(Expression blockIdExpr, Expression valueExpr,
            boolean isWrite) {
        MethodCallExpr methodCall = new MethodCallExpr(
                new NameExpr(ORAM_FIELD_NAME),
                "access",
                NodeList.nodeList(
                        blockIdExpr,
                        valueExpr,
                        new BooleanLiteralExpr(isWrite)));

        return methodCall;
    }

    private static class ArrayMethodModifier extends ModifierVisitor<Void> {
        private final Map<String, Expression> arrayLengths = new HashMap<>();
        private final Set<String> arrayNamesToSkip = new HashSet<>(Arrays.asList("args"));

        // Array initializers
        // Do not remove the initializer yet because visit(FieldAccessExpr n, Void arg)
        // expects the array variable to exist
        @Override
        public Visitable visit(VariableDeclarator n, Void arg) {
            Optional<Expression> initializer = n.getInitializer();
            String arrayName = n.getName().asString();

            if (initializer.isPresent() && initializer.get() instanceof ArrayCreationExpr) {
                ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) initializer.get();
                NodeList<ArrayCreationLevel> dimensions = arrayCreationExpr.getLevels();

                if (!dimensions.isEmpty()) {
                    ArrayCreationLevel firstDimension = dimensions.get(0);
                    Optional<Expression> dimension = firstDimension.getDimension();
                    Expression sizeExpression = dimension.get();
                    arrayLengths.put(arrayName, sizeExpression);
                }
            }

            return super.visit(n, arg);
        }

        // Array length
        @Override
        public Visitable visit(FieldAccessExpr n, Void arg) {
            String fieldName = n.getName().asString();
            if (fieldName.equals("length")) {
                Expression scope = n.getScope();
                ResolvedType scopeType = scope.calculateResolvedType();
                if (scopeType.isArray()) {
                    String arrayName = scope.toString();
                    if (!arrayNamesToSkip.contains(arrayName) && arrayLengths.containsKey(arrayName)) {
                        return arrayLengths.get(arrayName);
                    }
                }
            }
            return super.visit(n, arg);
        }
    }

    private static class ArrayInitializerModifier extends ModifierVisitor<Void> {
        // Only call this Visitor if you no longer need the array variables to exist
        // Visitors that depend on array variables existing will not work if this
        // Visitor is called before them
        @Override
        public Visitable visit(VariableDeclarator n, Void arg) {
            if (n.getType() instanceof ArrayType) {
                Optional<Expression> initializer = n.getInitializer();

                if (initializer.isPresent()) {
                    Expression expr = initializer.get();
                    if (expr instanceof ArrayCreationExpr) {
                        // If initializing an array using 'new' keyword, remove the variable declaration
                        return null;
                    } else if (expr instanceof MethodCallExpr) {
                        System.out.println("Variable initialized by a method call: " + n.getName());
                        Expression variableDeclExpr = (Expression) n.getParentNode().get();
                        ExpressionStmt variableDeclExprStmt = (ExpressionStmt) variableDeclExpr.getParentNode().get();
                        BlockStmt blockStmt = (BlockStmt) variableDeclExprStmt.getParentNode().get();
                        NodeList<Statement> stmts = blockStmt.getStatements();
                        int indexOfVariableDeclStmt = stmts.indexOf(variableDeclExprStmt);

                        ForStmt writeArrayToORAMStmt = createWriteArrayToORAM(n.getName().toString());
                        stmts.add(indexOfVariableDeclStmt + 1, writeArrayToORAMStmt);
                    }
                }
            }

            return super.visit(n, arg);
        }
    }

    private static class ArrayAccessModifier extends ModifierVisitor<Void> {

        // Array reads
        @Override
        public Visitable visit(ArrayAccessExpr n, Void arg) {
            if (!isWrite(n)) {
                Expression arrayName = n.getName();
                Expression index = n.getIndex();

                MethodCallExpr stringValueOfCall = new MethodCallExpr(null, "String.valueOf", NodeList.nodeList(index));

                // "arrayName["
                StringLiteralExpr arrayNameAndOpenBracket = new StringLiteralExpr(arrayName + "[");
                // "arrayName[" + index.toString()
                BinaryExpr firstPart = new BinaryExpr(arrayNameAndOpenBracket, stringValueOfCall,
                        BinaryExpr.Operator.PLUS);
                // "]"
                StringLiteralExpr closingBracketLiteral = new StringLiteralExpr("]");
                // "arrayName[" + index.toString() + "]"
                BinaryExpr blockIdExpr = new BinaryExpr(firstPart, closingBracketLiteral, BinaryExpr.Operator.PLUS);

                Expression optionalEmptyExpr = new MethodCallExpr(
                        new NameExpr("Optional"),
                        "empty").setTypeArguments(
                                new ClassOrInterfaceType()
                                        .setName("byte[]"));

                MethodCallExpr accessMethodCall = createORAMAccessMethodCall(blockIdExpr, optionalEmptyExpr, false);

                MethodCallExpr getMethodCall = new MethodCallExpr(accessMethodCall, "get");
                ResolvedType resolvedType = arrayName.calculateResolvedType();
                Expression byteArrayParseExpr = createByteArrayDecodeExpr(getMethodCall, resolvedType);

                return byteArrayParseExpr;
            }

            return super.visit(n, arg);
        }

        // Array write
        @Override
        public Visitable visit(AssignExpr n, Void arg) {
            Expression target = n.getTarget();
            if (target instanceof ArrayAccessExpr) {
                ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) target;
                Expression arrayName = arrayAccessExpr.getName();
                Expression index = arrayAccessExpr.getIndex();

                // Value conversions
                Expression value = n.getValue();
                ResolvedType resolvedType = arrayName.calculateResolvedType();
                Expression valueByteArrayExpr = createByteArrayExpr(value, resolvedType);
                Expression optionalValueByteArrayExpr = new MethodCallExpr(
                        new NameExpr("Optional"),
                        "ofNullable",
                        NodeList.nodeList(valueByteArrayExpr));

                MethodCallExpr stringValueOfCall = new MethodCallExpr(null, "String.valueOf", NodeList.nodeList(index));

                // "arrayName["
                StringLiteralExpr arrayNameAndOpenBracket = new StringLiteralExpr(arrayName + "[");
                // "arrayName[" + index.toString()
                BinaryExpr firstPart = new BinaryExpr(arrayNameAndOpenBracket, stringValueOfCall,
                        BinaryExpr.Operator.PLUS);
                // "]"
                StringLiteralExpr closingBracketLiteral = new StringLiteralExpr("]");
                // "arrayName[" + index.toString() + "]"
                BinaryExpr blockIdExpr = new BinaryExpr(firstPart, closingBracketLiteral, BinaryExpr.Operator.PLUS);

                MethodCallExpr methodCall = createORAMAccessMethodCall(blockIdExpr, optionalValueByteArrayExpr, true);

                return methodCall;
            }

            return super.visit(n, arg);
        }

        private static boolean isWrite(ArrayAccessExpr n) {
            Node parent = n.getParentNode().orElse(null);
            if (parent instanceof AssignExpr) {
                AssignExpr assignment = (AssignExpr) parent;
                return assignment.getTarget().equals(n);
            }
            return false;
        }

    }

    private static Expression createByteArrayExpr(Expression valueExpr, ResolvedType arrayResolvedType) {
        String arrayTypeDescription = arrayResolvedType.describe();
        System.out.println("createByteArrayExpr " + arrayTypeDescription);

        if (arrayTypeDescription.equals("int[]")) {
            return createIntToByteArrayExpr(valueExpr);
        } else if (arrayTypeDescription.equals("java.lang.String[]")) {
            return createStringToByteArrayExpr(valueExpr);
        }

        return valueExpr;
    }

    private static Expression createIntToByteArrayExpr(Expression valueExpr) {
        // ByteBuffer.allocate(4)
        MethodCallExpr allocateCall = new MethodCallExpr(
                new NameExpr("ByteBuffer"),
                "allocate",
                NodeList.<Expression>nodeList(new IntegerLiteralExpr("4")));

        // ByteBuffer.allocate(4).putInt(valueExpression)
        MethodCallExpr putIntCall = new MethodCallExpr(
                allocateCall,
                "putInt",
                NodeList.nodeList(valueExpr));

        // ByteBuffer.allocate(4).putInt(valueExpression).array()
        MethodCallExpr arrayCall = new MethodCallExpr(
                putIntCall,
                "array");

        return arrayCall;
    }

    private static Expression createStringToByteArrayExpr(Expression valueExpr) {
        // valueExpr.getBytes()
        MethodCallExpr getBytesCall = new MethodCallExpr(
                valueExpr,
                "getBytes");

        return getBytesCall;
    }

    private static Expression createByteArrayDecodeExpr(Expression valueExpr, ResolvedType arrayResolvedType) {
        String arrayTypeDescription = arrayResolvedType.describe();
        System.out.println("createByteArrayDecodeExpr " + arrayTypeDescription);
        if (arrayTypeDescription.equals("int[]")) {
            return createByteArrayToIntExpr(valueExpr);
        } else if (arrayTypeDescription.equals("java.lang.String[]")) {
            return createByteArrayToStringExpr(valueExpr);
        }

        return valueExpr;
    }

    private static Expression createByteArrayToIntExpr(Expression valueExpr) {
        // ByteBuffer.wrap(valueExpr)
        MethodCallExpr wrapCall = new MethodCallExpr(
                new NameExpr("ByteBuffer"),
                "wrap",
                NodeList.nodeList(valueExpr));

        // ByteBuffer.wrap(valueExpr).getInt()
        MethodCallExpr getIntCall = new MethodCallExpr(
                wrapCall,
                "getInt");

        return getIntCall;
    }

    private static Expression createByteArrayToStringExpr(Expression valueExpr) {
        ObjectCreationExpr newStringExpr = new ObjectCreationExpr();
        newStringExpr.setType("String");
        newStringExpr.addArgument(valueExpr);

        return newStringExpr;
    }

}
