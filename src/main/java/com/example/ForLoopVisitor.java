package com.example;

import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ForLoopVisitor {

    private static final String POWER_OF_2_FUNCTION_NAME = "_obliviousNextPowerOf2";
    private final Set<String> classesToModify = new HashSet<>();

    private class ForLoopCollector extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ForStmt forStmt, Void arg) {
            System.out.println("Visiting for loop: " + forStmt);
            super.visit(forStmt, arg);

            if (!forStmt.getCompare().isPresent()) {
                System.out.println("No compare expression found in for loop");
                return;
            }

            Expression compare = forStmt.getCompare().get();
            if (!(compare instanceof BinaryExpr)) {
                System.out.println("Compare expression is not binary: " + compare);
                return;
            }

            // Find parent class and add to set
            ClassOrInterfaceDeclaration classDecl = getParentClass(forStmt);
            if (classDecl != null) {
                String className = classDecl.getNameAsString();
                System.out.println("Found parent class: " + className);
                classesToModify.add(className);
                System.out.println("Classes to modify size: " + classesToModify.size());
            } else {
                System.out.println("No parent class found for for loop");
            }

            BinaryExpr binaryExpr = (BinaryExpr) compare;
            Expression originalRight = binaryExpr.getRight();
            Expression loopVar = binaryExpr.getLeft();

            // Create the power of 2 call for the loop bound
            MethodCallExpr powerOf2Call = new MethodCallExpr(
                    null,
                    POWER_OF_2_FUNCTION_NAME,
                    new NodeList<>(originalRight)
            );

            // Replace the loop bound with the power of 2 call
            binaryExpr.setRight(powerOf2Call);

            // Create if statement: if (i < originalBound)
            BinaryExpr ifCondition = new BinaryExpr(
                    loopVar.clone(),
                    originalRight.clone(),
                    BinaryExpr.Operator.LESS
            );

            // Get the original loop body
            Statement originalBody = forStmt.getBody();

            // Create new if statement with original body
            IfStmt ifStmt = new IfStmt(
                    ifCondition,
                    originalBody,
                    null
            );

            NodeList<Statement> statements = new NodeList<>();
            statements.add(ifStmt);

            // Replace original loop body with block containing if statement
            forStmt.setBody(new BlockStmt(statements));

            System.out.println("Transformed loop condition to use " + POWER_OF_2_FUNCTION_NAME);
        }

        private ClassOrInterfaceDeclaration getParentClass(ForStmt forStmt) {
            com.github.javaparser.ast.Node current = forStmt;
            while (current != null && !(current instanceof ClassOrInterfaceDeclaration)) {
                current = current.getParentNode().isPresent() ? current.getParentNode().get() : null;
            }
            return (ClassOrInterfaceDeclaration) current;
        }
    }

    private class MethodAdder extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
            String className = classDecl.getNameAsString();
            System.out.println("MethodAdder visiting class: " + className);
            super.visit(classDecl, arg);

            if (classesToModify.contains(className)) {
                System.out.println("Class " + className + " needs method");
                if (!hasMethod(classDecl, POWER_OF_2_FUNCTION_NAME)) {
                    System.out.println("Adding method to class " + className);
                    addPowerOf2Method(classDecl);
                } else {
                    System.out.println("Method already exists in class " + className);
                }
            } else {
                System.out.println("Class " + className + " does not need method");
            }
        }

        private boolean hasMethod(ClassOrInterfaceDeclaration classDecl, String methodName) {
            boolean hasMethod = classDecl.getMethodsByName(methodName).size() > 0;
            System.out.println("Checking for method " + methodName + ": " + hasMethod);
            return hasMethod;
        }
    }

    public void transform(com.github.javaparser.ast.CompilationUnit cu) {
        System.out.println("\n=== Starting First Pass ===");
        ForLoopCollector collector = new ForLoopCollector();
        collector.visit(cu, null);

        System.out.println("\n=== Starting Second Pass ===");
        System.out.println("Number of classes to modify: " + classesToModify.size());
        System.out.println("Classes to modify: " + classesToModify);
        MethodAdder adder = new MethodAdder();
        adder.visit(cu, null);
    }

    private void addPowerOf2Method(ClassOrInterfaceDeclaration classDecl) {
        System.out.println("Creating method " + POWER_OF_2_FUNCTION_NAME);
        MethodDeclaration method = new MethodDeclaration();
        method.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
        method.setType(PrimitiveType.intType());
        method.setName(POWER_OF_2_FUNCTION_NAME);

        Parameter param = new Parameter(PrimitiveType.intType(), "n");
        method.addParameter(param);

        String body
                = "{\n"
                + "    if (n < 1) return 1;\n"
                + "    n--;\n"
                + "    n |= n >> 1;\n"
                + "    n |= n >> 2;\n"
                + "    n |= n >> 4;\n"
                + "    n |= n >> 8;\n"
                + "    n |= n >> 16;\n"
                + "    return n + 1;\n"
                + "}";

        method.setBody(StaticJavaParser.parseBlock(body));
        classDecl.addMember(method);
        System.out.println("Added method " + POWER_OF_2_FUNCTION_NAME + " to class " + classDecl.getNameAsString());
    }
}
