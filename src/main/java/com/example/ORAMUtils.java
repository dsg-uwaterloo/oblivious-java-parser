package com.example;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
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
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;

public class ORAMUtils {

    static MethodCallExpr createORAMAccessMethodCall(Expression blockIdExpr, Expression valueExpr, boolean isWrite) {
        MethodCallExpr methodCall = new MethodCallExpr(new NameExpr(ORAMConstants.ORAM_FIELD_NAME), "access", NodeList.nodeList(blockIdExpr, valueExpr, new BooleanLiteralExpr(isWrite)));
        return methodCall;
    }

    static Expression createByteArrayExpr(Expression valueExpr, ResolvedType resolvedType) {
        String typeDescription = resolvedType.describe();
        System.out.println("createByteArrayExpr " + typeDescription);
        // Support both 1d and element types for 2d arrays.
        if (typeDescription.equals("int") || typeDescription.equals("int[]")) {
            return createIntToByteArrayExpr(valueExpr);
        } else if (typeDescription.equals("java.lang.String") || typeDescription.equals("java.lang.String[]")) {
            return createStringToByteArrayExpr(valueExpr);
        }
        return valueExpr;
    }

    static Expression createIntToByteArrayExpr(Expression valueExpr) {
        // Equivalent to: ByteBuffer.allocate(4).putInt(valueExpr).array()
        MethodCallExpr allocateCall = new MethodCallExpr(new NameExpr("ByteBuffer"), "allocate", NodeList.<Expression>nodeList(new IntegerLiteralExpr("4")));
        MethodCallExpr putIntCall = new MethodCallExpr(allocateCall, "putInt", NodeList.<Expression>nodeList(valueExpr));
        return new MethodCallExpr(putIntCall, "array");
    }

    static Expression createStringToByteArrayExpr(Expression valueExpr) {
        // Equivalent to: valueExpr.getBytes()
        return new MethodCallExpr(valueExpr, "getBytes");
    }

    static Expression createByteArrayDecodeExpr(Expression valueExpr, ResolvedType resolvedType) {
        String typeDescription = resolvedType.describe();
        System.out.println("createByteArrayDecodeExpr " + typeDescription);
        if (typeDescription.equals("int") || typeDescription.equals("int[]")) {
            return createByteArrayToIntExpr(valueExpr);
        } else if (typeDescription.equals("java.lang.String") || typeDescription.equals("java.lang.String[]")) {
            return createByteArrayToStringExpr(valueExpr);
        }
        return valueExpr;
    }

    static Expression createByteArrayToIntExpr(Expression valueExpr) {
        // Equivalent to: ByteBuffer.wrap(valueExpr).getInt()
        MethodCallExpr wrapCall = new MethodCallExpr(new NameExpr("ByteBuffer"), "wrap", NodeList.nodeList(valueExpr));
        return new MethodCallExpr(wrapCall, "getInt");
    }

    static Expression createByteArrayToStringExpr(Expression valueExpr) {
        // Equivalent to: new String(valueExpr)
        ObjectCreationExpr newStringExpr = new ObjectCreationExpr();
        newStringExpr.setType("String");
        newStringExpr.addArgument(valueExpr);
        return newStringExpr;
    }

    static ExpressionStmt getGrandParentStatement(VariableDeclarator declarator) {
        Optional<Node> parent = declarator.getParentNode();
        if (!parent.isPresent()) {
            return null;
        }
        Optional<Node> grandParent = parent.get().getParentNode();
        return (grandParent.isPresent() && grandParent.get() instanceof ExpressionStmt) ? (ExpressionStmt) grandParent.get() : null;
    }

    static ForStmt createWriteArrayToORAM(String arrayName, ResolvedType elementType) {
        ForStmt forStmt = new ForStmt();
        VariableDeclarator iEqualsZero = new VariableDeclarator(PrimitiveType.intType(), "i", new IntegerLiteralExpr("0"));
        NodeList<Expression> initializationExprs = NodeList.<Expression>nodeList(new VariableDeclarationExpr(iEqualsZero));
        forStmt.setInitialization(initializationExprs);
        BinaryExpr compareExpr = new BinaryExpr(new NameExpr("i"), new FieldAccessExpr(new NameExpr(arrayName), "length"), BinaryExpr.Operator.LESS);
        forStmt.setCompare(compareExpr);
        AssignExpr iIncrement = new AssignExpr(new NameExpr("i"), new BinaryExpr(new NameExpr("i"), new IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS), AssignExpr.Operator.ASSIGN);
        NodeList<Expression> updateExprs = NodeList.<Expression>nodeList(iIncrement);
        forStmt.setUpdate(updateExprs);
        Expression arrayAccessExpr = new ArrayAccessExpr(new NameExpr(arrayName), new NameExpr("i"));
        Expression valueByteArrayExpr = ORAMUtils.createByteArrayExpr(arrayAccessExpr, elementType);
        Expression optionalValueByteArrayExpr = new MethodCallExpr(new NameExpr("Optional"), "ofNullable", NodeList.nodeList(valueByteArrayExpr));
        MethodCallExpr stringValueOfCall = new MethodCallExpr(null, "String.valueOf", NodeList.<Expression>nodeList(new NameExpr("i")));
        StringLiteralExpr arrayNameAndOpenBracket = new StringLiteralExpr(arrayName + "[");
        BinaryExpr firstPart = new BinaryExpr(arrayNameAndOpenBracket, stringValueOfCall, BinaryExpr.Operator.PLUS);
        StringLiteralExpr closingBracketLiteral = new StringLiteralExpr("]");
        BinaryExpr blockIdExpr = new BinaryExpr(firstPart, closingBracketLiteral, BinaryExpr.Operator.PLUS);
        MethodCallExpr oramAccessMethodCall = ORAMUtils.createORAMAccessMethodCall(blockIdExpr, optionalValueByteArrayExpr, true);
        ExpressionStmt oramAccessStmt = new ExpressionStmt(oramAccessMethodCall);
        forStmt.setBody(oramAccessStmt);
        return forStmt;
    }
}
