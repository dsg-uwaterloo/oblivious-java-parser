package com.example;

import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.ast.visitor.Visitable;

public class ArrayInitializerModifier extends ModifierVisitor<Void> {

    // Only call this Visitor if you no longer need the array variables to exist
    // Visitors that depend on array variables existing will not work if this
    // Visitor is called before them
    @Override
    public Visitable visit(VariableDeclarator n, Void arg) {
        if (n.getType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) n.getType();
            Optional<Expression> initializer = n.getInitializer();

            if (initializer.isPresent()) {
                Expression expr = initializer.get();
                if (expr instanceof ArrayCreationExpr) {
                    // If initializing an array using 'new' keyword, remove the variable declaration
                    return null;
                } else if (expr instanceof MethodCallExpr) {
                    System.out.println("Variable initialized by a method call: " + n.getName());
                    Type componentType = arrayType.getComponentType();
                    ResolvedType resolvedElementType;
                    try {
                        resolvedElementType = componentType.resolve();
                    } catch (Exception e) {
                        System.err.println("Could not resolve component type for array: " + n.getName());
                        return super.visit(n, arg);
                    }

                    // Create the ORAM write loop with the resolved element type
                    ForStmt writeArrayToORAMStmt = ORAMUtils.createWriteArrayToORAM(
                            n.getName().toString(),
                            resolvedElementType
                    );

                    // Insert the loop after the variable declaration
                    Expression variableDeclExpr = (Expression) n.getParentNode().get();
                    ExpressionStmt variableDeclExprStmt = (ExpressionStmt) variableDeclExpr.getParentNode().get();
                    BlockStmt blockStmt = (BlockStmt) variableDeclExprStmt.getParentNode().get();
                    NodeList<Statement> stmts = blockStmt.getStatements();
                    int indexOfVariableDeclStmt = stmts.indexOf(variableDeclExprStmt);

                    stmts.add(indexOfVariableDeclStmt + 1, writeArrayToORAMStmt);
                }
            }
        }

        return super.visit(n, arg);
    }
}
