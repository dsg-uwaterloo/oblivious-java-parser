package com.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

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

        System.out.println(cu.toString());
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

    private static class ArrayAccessModifier extends ModifierVisitor<Void> {
        // // Array initializers
        // @Override
        // public Visitable visit(VariableDeclarator n, Void arg) {
        // if (n.getInitializer().isPresent() && n.getInitializer().get() instanceof
        // ArrayCreationExpr) {
        // // Remove the array initializer because array reads and writes will be issued
        // // through PathORAM.access
        // n.removeInitializer();
        // }
        // return super.visit(n, arg);
        // }

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

                MethodCallExpr accessMethodCall = new MethodCallExpr(
                        new NameExpr(ORAM_FIELD_NAME),
                        "access",
                        NodeList.nodeList(
                                blockIdExpr,
                                optionalEmptyExpr,
                                new NameExpr("false"))); // isWrite

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

                String blockId = arrayName.toString() + "[" + index.toString() + "]";
                StringLiteralExpr blockIdExpr = new StringLiteralExpr(blockId);

                MethodCallExpr methodCall = new MethodCallExpr(
                        new NameExpr(ORAM_FIELD_NAME),
                        "access",
                        NodeList.nodeList(
                                blockIdExpr,
                                optionalValueByteArrayExpr,
                                new NameExpr("true"))); // isWrite

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

        private Expression createByteArrayDecodeExpr(Expression valueExpr, ResolvedType arrayResolvedType) {
            String arrayTypeDescription = arrayResolvedType.describe();
            System.out.println("createByteArrayDecodeExpr " + arrayTypeDescription);
            if (arrayTypeDescription.equals("int[]")) {
                return createByteArrayToIntExpr(valueExpr);
            } else if (arrayTypeDescription.equals("java.lang.String[]")) {
                return createByteArrayToStringExpr(valueExpr);
            }

            return valueExpr;
        }

        private Expression createByteArrayToIntExpr(Expression valueExpr) {
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

        private Expression createByteArrayToStringExpr(Expression valueExpr) {
            ObjectCreationExpr newStringExpr = new ObjectCreationExpr();
            newStringExpr.setType("String");
            newStringExpr.addArgument(valueExpr);

            return newStringExpr;
        }
    }

}
