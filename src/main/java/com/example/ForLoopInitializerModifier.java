package com.example;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;

public class ForLoopInitializerModifier extends ModifierVisitor<Void> {

    @Override
    public Visitable visit(ForStmt n, Void arg) {
        super.visit(n, arg);
        System.out.println("Modifying " + n.toString());
        List<Expression> newInitialization = new ArrayList<>();

        for (Expression expr : n.getInitialization()) {
            System.out.println("Modifying " + expr.toString());
            if (expr instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr varDeclExpr = (VariableDeclarationExpr) expr;
                for (VariableDeclarator varDecl : varDeclExpr.getVariables()) {
                    if (!varDecl.getInitializer().isPresent()) {
                        // if there is no initializer, replace w/ default value
                        // TODO
                        continue;
                    }
                    System.out.println("Modifying " + varDecl.toString());

                    String varName = varDecl.getNameAsString();
                    Expression value = varDecl.getInitializer().get();
                    ResolvedType resolvedType = varDecl.getType().resolve();

                    Expression valueByteArrayExpr = ORAMUtils.createByteArrayExpr(value, resolvedType);
                    Expression optionalValueByteArrayExpr = new MethodCallExpr(
                            new NameExpr("Optional"),
                            "ofNullable",
                            NodeList.nodeList(valueByteArrayExpr));

                    Expression keyExpr = new StringLiteralExpr(varName);
                    // Create ORAM access call for the initializer
                    MethodCallExpr oramCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalValueByteArrayExpr, true);
                    newInitialization.add(oramCall);
                }
            } else {
                newInitialization.add(expr);
            }
        }

        // Create modified ForStmt
        System.out.println("New initialization: " + newInitialization.get(0).toString());
        ForStmt modifiedForStmt = new ForStmt(
                NodeList.nodeList(newInitialization),
                n.getCompare().orElse(null),
                n.getUpdate(),
                n.getBody()
        );

        return modifiedForStmt;
    }
}
