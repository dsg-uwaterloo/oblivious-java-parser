package com.example;

import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;

class LocalVariableWriteModifier extends ModifierVisitor<Void> {

    private final Set<String> localVars = new HashSet<>();

    /*
    * Find variable declarations. Add to set of known local variables.
     */
    @Override
    public Visitable visit(VariableDeclarator n, Void arg) {
        super.visit(n, arg);
        localVars.add(n.getNameAsString());
        return n;
    }

    /*
    * For variable DECLARATIONS
     */
    @Override
    public Visitable visit(BlockStmt n, Void arg) {
        // Most statements will get added as-is
        // Declarations will be replaced by oram.access calls
        BlockStmt newBlock = new BlockStmt();

        for (Statement stmt : n.getStatements()) {
            // Process each statement to collect variables and transform expressions
            // (by other visit methods)
            stmt.accept(this, arg);

            // if not variable declaration, then just add it is
            if (!stmt.isExpressionStmt() || !stmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
                newBlock.addStatement(stmt);
                continue;
            }

            // For each variable with an initializer, replace w/ an ORAM access stmt
            for (VariableDeclarator varDecl : stmt.asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariables()) {
                if (!varDecl.getInitializer().isPresent()) {
                    // if there is no initializer, replace w/ default value
                    // TODO
                    continue;
                }

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

                // Add as a new statement after the declaration
                newBlock.addStatement(new ExpressionStmt(oramCall));
            }
        }

        return newBlock;
    }

    @Override
    public Visitable visit(AssignExpr n, Void arg) {
        super.visit(n, arg);

        // Only handle simple assignments (=) for now
        // TODO: +=, -=, *=, /=, %=
        if (n.getOperator() != AssignExpr.Operator.ASSIGN) {
            return n;
        }

        // Check if the left side is a NameExpr (simple variable)
        if (!(n.getTarget() instanceof NameExpr)) {
            return n;
        }

        NameExpr target = (NameExpr) n.getTarget();
        String name = target.getNameAsString();

        // Check if it's a local variable
        if (!localVars.contains(name)) {
            return n;
        }

        // The RHS of the assignment
        Expression value = n.getValue();

        ResolvedType resolvedType = target.calculateResolvedType();
        // Create byte array
        Expression valueByteArrayExpr = ORAMUtils.createByteArrayExpr(value, resolvedType);

        // Wrap in Optional.ofNullable
        Expression optionalValueByteArrayExpr = new MethodCallExpr(
                new NameExpr("Optional"),
                "ofNullable",
                NodeList.nodeList(valueByteArrayExpr));

        // Create the key (just the variable name as a string)
        Expression keyExpr = new StringLiteralExpr(name);

        MethodCallExpr methodCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalValueByteArrayExpr, true);

        return methodCall;
    }

}
