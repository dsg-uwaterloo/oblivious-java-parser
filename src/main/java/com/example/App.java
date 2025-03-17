package com.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
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

        // For loops
        ForLoopVisitor visitor = new ForLoopVisitor();
        visitor.transform(cu); // Two pass approach which is encapsulated within ForLoopVisitor::transform

        // If statements
        VoidVisitor<?> ifStmtVisitor = new IfStmtDummyVisitor();
        ifStmtVisitor.visit(cu, null);

        // Re-generate type info
        String modifiedCode = cu.toString();
        cu = StaticJavaParser.parse(modifiedCode);

        ModifierVisitor<?> localVariableReadModifier = new LocalVariableReadModifier();
        localVariableReadModifier.visit(cu, null);
        ModifierVisitor<?> localVariableWriteModifier = new LocalVariableWriteModifier();
        localVariableWriteModifier.visit(cu, null);
        ModifierVisitor<?> forLoopInitializerModifier = new ForLoopInitializerModifier();
        forLoopInitializerModifier.visit(cu, null);
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
                System.err.println(
                        "Could not resolve type for parameter " + paramName + " in method " + method.getName());
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

    private static void addImports(CompilationUnit cu) {
        cu.addImport(new ImportDeclaration("java.util.Optional", false, false));
        cu.addImport(new ImportDeclaration("java.nio.ByteBuffer", false, false));
    }

    private static String getClassNameFromPath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        return className;
    }
}
