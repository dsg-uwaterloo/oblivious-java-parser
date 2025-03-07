package com.example;

import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;

class LocalVariableReadModifier extends ModifierVisitor<Void> {

    private Set<String> localVars = new HashSet<>();

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
     * NameExprs
     * Can be read or assignment. Skip if assignment.
     */
    @Override
    public Visitable visit(NameExpr n, Void arg) {
        super.visit(n, arg);
        String name = n.getNameAsString();

        if (!localVars.contains(name) || isLeftSideOfAssignment(n)) {
            // This `name` is not a local variable
            return n;
        }

        // 1. Generate key for this variable in the ORAM tree
        Expression keyExpr = new StringLiteralExpr(name);

        // Create Optional.empty() to pass as "data" argument to ORAM access call 
        Expression optionalEmptyExpr = new MethodCallExpr(new NameExpr("Optional"), "empty")
                .setTypeArguments(new ClassOrInterfaceType().setName("byte[]"));

        // 3. Create ORAM access method call
        MethodCallExpr accessMethodCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalEmptyExpr, false);
        MethodCallExpr getMethodCall = new MethodCallExpr(accessMethodCall, "get");

        // 4. Get resolved type of the variable and decode the byte array
        System.out.println("Trying to resolve type of " + name);
        ResolvedType resolvedType = n.calculateResolvedType();
        Expression byteArrayDecodeExpr = ORAMUtils.createByteArrayDecodeExpr(getMethodCall, resolvedType);

        return byteArrayDecodeExpr;
    }

    private boolean isLeftSideOfAssignment(NameExpr n) {
        // Is this NameExpr being assigned to? (i.e. is a write, not a read)

        return n.getParentNode().isPresent()
                && n.getParentNode().get() instanceof AssignExpr
                && ((AssignExpr) n.getParentNode().get()).getTarget() == n;
    }
}
