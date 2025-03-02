package com.example;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;

public class ArrayAccessModifier extends ModifierVisitor<Void> {

    // Array reads (accessing an element like a[i] or a[i][j])
    @Override
    public Visitable visit(ArrayAccessExpr n, Void arg) {
        if (!isWrite(n)) {
            // Generate a composite key for multidimensional array accesses.
            // For example, a[i][j] will produce key: "a[i][j]"
            Expression keyExpr = generateKeyExpression(n);

            // If no value is provided (read), we use Optional.empty() in the call to oram.access
            Expression optionalEmptyExpr = new MethodCallExpr(
                    new NameExpr("Optional"),
                    "empty")
                    .setTypeArguments(new ClassOrInterfaceType().setName("byte[]"));

            MethodCallExpr accessMethodCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalEmptyExpr, false);
            MethodCallExpr getMethodCall = new MethodCallExpr(accessMethodCall, "get");

            // Use the resolved type of the array access itself.
            // For a[i] (with a as int[]) this returns int,
            // while for a[i][j] (with a as int[][]) it returns int.
            ResolvedType resolvedType = n.calculateResolvedType();
            Expression byteArrayParseExpr = ORAMUtils.createByteArrayDecodeExpr(getMethodCall, resolvedType);
            return byteArrayParseExpr;
        }
        return super.visit(n, arg);
    }

    // Array writes (assignments such as a[i] = ... or a[i][j] = ...)
    @Override
    public Visitable visit(AssignExpr n, Void arg) {
        Expression target = n.getTarget();
        if (target instanceof ArrayAccessExpr) {
            ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) target;
            Expression value = n.getValue();

            // Use the type of the full array access;
            // e.g. for a[i][j] with a as int[][], we want the int conversion.
            ResolvedType resolvedType = arrayAccessExpr.calculateResolvedType();
            Expression valueByteArrayExpr = ORAMUtils.createByteArrayExpr(value, resolvedType);
            Expression optionalValueByteArrayExpr = new MethodCallExpr(
                    new NameExpr("Optional"),
                    "ofNullable",
                    NodeList.nodeList(valueByteArrayExpr));

            Expression keyExpr = generateKeyExpression(arrayAccessExpr);
            MethodCallExpr methodCall = ORAMUtils.createORAMAccessMethodCall(keyExpr, optionalValueByteArrayExpr, true);
            return methodCall;
        }
        return super.visit(n, arg);
    }

    // Determines if an array access is on the left-hand side of an assignment.
    private static boolean isWrite(ArrayAccessExpr n) {
        Node parent = n.getParentNode().orElse(null);
        if (parent instanceof AssignExpr) {
            AssignExpr assignment = (AssignExpr) parent;
            return assignment.getTarget().equals(n);
        }
        return false;
    }

    // Helper method that recursively builds the key for an array access.
    // For a one-dimensional array access a[i] the key is:
    //    "a" + "[" + String.valueOf(i) + "]"
    // For a two-dimensional access a[i][j] it recursively creates:
    //    (generateKeyExpression(a[i])) + "[" + String.valueOf(j) + "]"
    private static Expression generateKeyExpression(ArrayAccessExpr expr) {
        Expression arrayPart = expr.getName();
        Expression index = expr.getIndex();
        // Build String.valueOf(index)
        MethodCallExpr stringValueOfCall = new MethodCallExpr(null, "String.valueOf", NodeList.nodeList(index));
        StringLiteralExpr openBracket = new StringLiteralExpr("[");
        StringLiteralExpr closeBracket = new StringLiteralExpr("]");

        if (arrayPart instanceof ArrayAccessExpr) {
            Expression innerKey = generateKeyExpression((ArrayAccessExpr) arrayPart);
            BinaryExpr keyWithOpen = new BinaryExpr(innerKey, openBracket, BinaryExpr.Operator.PLUS);
            BinaryExpr keyWithIndex = new BinaryExpr(keyWithOpen, stringValueOfCall, BinaryExpr.Operator.PLUS);
            return new BinaryExpr(keyWithIndex, closeBracket, BinaryExpr.Operator.PLUS);
        } else {
            // Base case: if the array part is not an ArrayAccessExpr,
            // assume it is the variable or field name.
            String baseName = arrayPart.toString();
            StringLiteralExpr baseNameLiteral = new StringLiteralExpr(baseName);
            BinaryExpr keyWithOpen = new BinaryExpr(baseNameLiteral, openBracket, BinaryExpr.Operator.PLUS);
            BinaryExpr keyWithIndex = new BinaryExpr(keyWithOpen, stringValueOfCall, BinaryExpr.Operator.PLUS);
            return new BinaryExpr(keyWithIndex, closeBracket, BinaryExpr.Operator.PLUS);
        }
    }
}
