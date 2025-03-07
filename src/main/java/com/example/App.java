package com.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class App {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java App <input_file> <output_class_name>");
            System.exit(1);
        }

        String filePathStr = args[0];
        String outputClassName = args[1];
        Path filePath = Paths.get(filePathStr);

        TypeSolver typeSolver = new ReflectionTypeSolver();

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser
                .getParserConfiguration()
                .setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(filePath));

        // Output the total size of all arrays
        // TODO: How to calculate ORAM size if array sizes depend on user input?
        // ArraySizeVisitor arraySizeVisitor = new ArraySizeVisitor();
        // cu.accept(arraySizeVisitor, null);
        // int oramSize = arraySizeVisitor.getTotalArraySize();
        int oramSize = 1_048_576;

        // Imports
        addImports(cu);

        // Postfix and prefix operators
        ModifierVisitor<?> unaryOpModifier = new UnaryOperatorModifier();
        unaryOpModifier.visit(cu, null);

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

        // ModifierVisitor<?> variableWriteModifier = new com.example.LocalVariableAccessModifier();
        // variableWriteModifier.visit(cu, null);
        // ModifierVisitor<?> variableReadModifier = new LocalVariableReadModifier();
        // variableReadModifier.visit(cu, null);
        // Local variable initializations
        // ModifierVisitor<?> localVariableInitializationVisitor = new LocalVariableInitializationModifier();
        // localVariableInitializationVisitor.visit(cu, null);
        // Args array
        // Note: This needs to be after array access modifications so that args array
        // accesses **when inserting its elements into the ORAM tree** do not get
        // modified
        // Optional<MethodDeclaration> maybeMainMethodDecl = getMainMethodFromClassDecl(classDeclaration);
        // if (maybeMainMethodDecl.isPresent()) {
        //     MethodDeclaration mainMethodDecl = maybeMainMethodDecl.get();
        //     BlockStmt mainMethodBody = mainMethodDecl.getBody().orElseThrow();
        //     ResolvedType argsType = mainMethodDecl.getParameter(0).getType().resolve();
        //     ResolvedType argsElementType = argsType.asArrayType().getComponentType();
        //     ForStmt writeArgsArrayToORAMStmt = createWriteArrayToORAM("args", argsElementType);
        //     mainMethodBody.getStatements().add(1, writeArgsArrayToORAMStmt);
        // }
        // For loops
        // Two pass approach which is encapsulated within ForLoopVisitor::transform
        ForLoopVisitor visitor = new ForLoopVisitor();
        visitor.transform(cu);

        // If statements
        VoidVisitor<?> ifStmtVisitor = new IfStmtDummyVisitor();
        ifStmtVisitor.visit(cu, null);

        // Get type info again
        String modifiedCode = cu.toString();
        cu = StaticJavaParser.parse(modifiedCode);

        ModifierVisitor<?> localVariableReadModifier = new LocalVariableReadModifier();
        localVariableReadModifier.visit(cu, null);

        ModifierVisitor<?> localVariableWriteModifier = new LocalVariableWriteModifier();
        localVariableWriteModifier.visit(cu, null);

        String className = getClassNameFromPath(filePath);
        ClassOrInterfaceDeclaration classDeclaration = cu.getClassByName(className).orElseThrow();
        // Add ORAM local variable to each method
        for (MethodDeclaration method : classDeclaration.getMethods()) {
            addORAMLocalVariableToMethod(method, oramSize);
        }

        // Method return values
        // ModifierVisitor<?> methodReturnVisitor = new MethodReturnValueVisitor();
        // methodReturnVisitor.visit(cu, null);
        // Replace the class name
        ClassOrInterfaceDeclaration classDecl = cu.getClassByName(className).orElseThrow();
        classDecl.setName(outputClassName);

        // Create output file path in the same directory as input file
        Path outputPath = filePath.resolveSibling(outputClassName + ".java");

        // Write the transformed code to the output file
        Files.writeString(outputPath, cu.toString());
        System.out.println("Transformed code written to: " + outputPath);
    }

    private static void addORAMLocalVariableToMethod(MethodDeclaration method, int oramSize) {
        if (!method.getBody().isPresent()) {
            return;
        }
        BlockStmt body = method.getBody().get();
        ObjectCreationExpr initializer = new ObjectCreationExpr()
                .setType(ORAMConstants.PATH_ORAM_CLASS_NAME)
                .addArgument(new IntegerLiteralExpr(String.valueOf(oramSize)));
        VariableDeclarator varDeclarator = new VariableDeclarator(
                new ClassOrInterfaceType(ORAMConstants.PATH_ORAM_CLASS_NAME),
                ORAMConstants.ORAM_FIELD_NAME,
                initializer);
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDeclarator);
        ExpressionStmt varDeclStmt = new ExpressionStmt(varDeclExpr);
        body.addStatement(0, varDeclStmt);

        int insertIndex = 1;
        for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
            String paramName = param.getNameAsString();
            ResolvedType paramType;
            try {
                paramType = param.getType().resolve();
            } catch (Exception e) {
                System.err.println("Could not resolve type for parameter " + paramName + " in method " + method.getName());
                continue;
            }

            if (paramType.isArray()) {
                ResolvedType elementType = paramType.asArrayType().getComponentType();
                ForStmt loopStmt = ORAMUtils.createWriteArrayToORAM(paramName, elementType);
                body.addStatement(insertIndex, loopStmt);
                insertIndex++;
            } else {
                StringLiteralExpr keyExpr = new StringLiteralExpr(paramName);
                Expression paramValueExpr = new NameExpr(paramName);
                Expression byteArrayExpr = ORAMUtils.createByteArrayExpr(paramValueExpr, paramType);
                Expression optionalValueExpr = new MethodCallExpr(
                        new NameExpr("Optional"),
                        "ofNullable",
                        NodeList.nodeList(byteArrayExpr));
                MethodCallExpr oramAccessCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalValueExpr, true);
                ExpressionStmt stmt = new ExpressionStmt(oramAccessCall);
                body.addStatement(insertIndex, stmt);
                insertIndex++;
            }
        }
    }

    private static FieldDeclaration createPathORAMFieldDeclaration(int numBlocks) {
        ClassOrInterfaceType classType = new ClassOrInterfaceType(null, ORAMConstants.PATH_ORAM_CLASS_NAME);
        ObjectCreationExpr initializer = new ObjectCreationExpr()
                .setType(ORAMConstants.PATH_ORAM_CLASS_NAME)
                .addArgument(String.valueOf(numBlocks));
        VariableDeclarator variableDeclarator = new VariableDeclarator(classType, ORAMConstants.ORAM_FIELD_NAME, initializer);
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

    private static class LocalVariableInitializationModifier extends ModifierVisitor<Void> {

        private final Set<String> variablesToSkip = new HashSet<>(Arrays.asList("i", "j", "k", ORAMConstants.ORAM_FIELD_NAME));

        @Override
        public Visitable visit(VariableDeclarationExpr n, Void arg) {
            // Process each variable declarator
            NodeList<VariableDeclarator> declarators = n.getVariables();
            for (VariableDeclarator declarator : declarators) {
                String varName = declarator.getNameAsString();

                // Skip loop counters and ORAM field
                if (variablesToSkip.contains(varName)) {
                    continue;
                }

                // Skip array declarations (handled separately by ArrayInitializerModifier)
                if (declarator.getType() instanceof ArrayType) {
                    continue;
                }

                // Only process if there's an initializer
                if (declarator.getInitializer().isPresent()) {
                    Expression initializer = declarator.getInitializer().get();

                    // Get parent statement
                    Optional<Node> parentNode = n.getParentNode();
                    if (parentNode.isPresent() && parentNode.get() instanceof ExpressionStmt) {
                        ExpressionStmt parentStmt = (ExpressionStmt) parentNode.get();
                        Optional<Node> blockNode = parentStmt.getParentNode();

                        if (blockNode.isPresent() && blockNode.get() instanceof BlockStmt) {
                            BlockStmt blockStmt = (BlockStmt) blockNode.get();
                            int stmtIndex = blockStmt.getStatements().indexOf(parentStmt);

                            // Create ORAM access to store the value
                            StringLiteralExpr keyExpr = new StringLiteralExpr(varName);

                            try {
                                ResolvedType resolvedType = declarator.getType().resolve();
                                Expression byteArrayExpr = ORAMUtils.createByteArrayExpr(initializer, resolvedType);

                                // Build ORAM access
                                MethodCallExpr oramAccessCall = ORAMUtils.createORAMAccessMethodCall(
                                        keyExpr,
                                        new MethodCallExpr(
                                                new NameExpr("Optional"),
                                                "of",
                                                NodeList.nodeList(byteArrayExpr)
                                        ),
                                        true
                                );

                                // Replace variable declaration with ORAM access
                                blockStmt.getStatements().set(stmtIndex, new ExpressionStmt(oramAccessCall));
                            } catch (Exception e) {
                                System.err.println("Could not resolve type for variable: " + varName);
                            }
                        }
                    }
                }
            }
            // Skip normal processing since we're replacing these statements
            return n;
        }
    }

    private static class LocalVariableAccessModifier extends ModifierVisitor<Void> {

        private final Set<String> knownVars = new HashSet<>();

        @Override
        public Visitable visit(VariableDeclarator n, Void arg) {
            // Track variable declarations to know which variables exist
            knownVars.add(n.getNameAsString());
            return super.visit(n, arg);
        }

        @Override
        public Visitable visit(NameExpr n, Void arg) {
            String varName = n.getNameAsString();

            if (!knownVars.contains(varName)) {
                return super.visit(n, arg);
            }

            // Skip if part of array name, b/c array accesses are handled by separate visitor
            if (isArrayBase(n)) {
                return super.visit(n, arg);
            }

            // This is a read of a local variable, replace with ORAM access
            StringLiteralExpr keyExpr = new StringLiteralExpr(varName);
            Expression optionalEmptyExpr = new MethodCallExpr(
                    new NameExpr("Optional"),
                    "empty")
                    .setTypeArguments(new ClassOrInterfaceType().setName("byte[]"));

            MethodCallExpr accessMethodCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalEmptyExpr, false);
            MethodCallExpr getMethodCall = new MethodCallExpr(accessMethodCall, "get");

            // Get variable type
            ResolvedType resolvedType;
            try {
                resolvedType = n.calculateResolvedType();
                Expression byteArrayParseExpr = ORAMUtils.createByteArrayDecodeExpr(getMethodCall, resolvedType);
                return byteArrayParseExpr;
            } catch (Exception e) {
                System.err.println("Could not resolve type for variable: " + varName);
                return super.visit(n, arg);
            }
        }

        // TODO: Handle increment operations e.g. count++
        // Helper method to check if NameExpr is the base of an array access
        private boolean isArrayBase(NameExpr n) {
            Optional<Node> parent = n.getParentNode();
            if (parent.isPresent() && parent.get() instanceof ArrayAccessExpr) {
                ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) parent.get();
                return arrayAccessExpr.getName().equals(n);
            }
            return false;
        }
    }

    private static class MethodReturnValueVisitor extends ModifierVisitor<Void> {

        @Override
        public Visitable visit(VariableDeclarator declarator, Void arg) {
            if (declarator.getInitializer().isPresent()
                    && declarator.getInitializer().get() instanceof MethodCallExpr) {

                MethodCallExpr methodCall = (MethodCallExpr) declarator.getInitializer().get();
                String varName = declarator.getNameAsString();

                // Get parent statement and its containing block
                ExpressionStmt parentStmt = ORAMUtils.getGrandParentStatement(declarator);

                if (parentStmt != null) {
                    BlockStmt block = (BlockStmt) parentStmt.getParentNode().orElseThrow();
                    int stmtIndex = block.getStatements().indexOf(parentStmt);

                    // Generate ORAM access with inlined method call
                    StringLiteralExpr keyExpr = new StringLiteralExpr(varName);
                    ResolvedType resolvedType = declarator.getType().resolve();

                    // Directly use methodCall in the value conversion
                    Expression byteArrayExpr = ORAMUtils.createByteArrayExpr(methodCall, resolvedType);

                    // Build the ORAM access statement
                    MethodCallExpr oramAccessCall = ORAMUtils.createORAMAccessMethodCall(
                            keyExpr,
                            new MethodCallExpr(new NameExpr("Optional"), "of", NodeList.nodeList(byteArrayExpr)),
                            true
                    );

                    // Replace variable declaration with ORAM access
                    block.getStatements().set(stmtIndex, new ExpressionStmt(oramAccessCall));
                }
            }
            return super.visit(declarator, arg);
        }
    }

    private static class ArraySizeVisitor extends VoidVisitorAdapter<Void> {

        // This variable holds the total computed size.
        private int totalArraySize = 0;

        @Override
        public void visit(VariableDeclarator vd, Void arg) {
            super.visit(vd, arg);
            if (vd.getInitializer().isPresent()) {
                Expression initializer = vd.getInitializer().get();
                if (initializer instanceof ArrayCreationExpr) {
                    ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) initializer;
                    List<ArrayCreationLevel> levels = arrayCreationExpr.getLevels();
                    // For multidimensional arrays, compute the product of dimensions.
                    int product = 1;
                    boolean valid = true;
                    for (ArrayCreationLevel level : levels) {
                        if (level.getDimension().isPresent()) {
                            String dimensionString = level.getDimension().get().toString();
                            try {
                                int dim = Integer.parseInt(dimensionString);
                                product *= dim;
                            } catch (NumberFormatException e) {
                                // When the expression is not a literal integer,
                                // log an error note
                                System.err.println("Non-integer array size encountered processing "
                                        + vd.getName() + ": " + dimensionString);
                                valid = false;
                                break;
                            }
                        }
                    }
                    if (valid) {
                        totalArraySize += product;
                    }
                }
            }
        }

        // Returns the computed total array size.
        public int getTotalArraySize() {
            return totalArraySize;
        }
    }
}
