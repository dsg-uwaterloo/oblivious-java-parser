package com.example;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class UnaryOperatorModifier extends ModifierVisitor<Void> {

    @Override
    public Visitable visit(UnaryExpr n, Void arg) {
        // First process any child nodes
        super.visit(n, arg);

        // Check if the unary expression is an increment or decrement
        if (n.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                || n.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                || n.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                || n.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {

            // Get the expression being incremented/decremented
            Expression target = n.getExpression();

            // Create an integer literal with value 1
            IntegerLiteralExpr one = new IntegerLiteralExpr("1");

            // Create binary expression: i + 1 or i - 1
            BinaryExpr binaryExpr;
            if (n.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || n.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT) {
                binaryExpr = new BinaryExpr(target.clone(), one, BinaryExpr.Operator.PLUS);
            } else {
                binaryExpr = new BinaryExpr(target.clone(), one, BinaryExpr.Operator.MINUS);
            }

            // Create assignment expression: i = i + 1 or i = i - 1
            return new AssignExpr(target, binaryExpr, AssignExpr.Operator.ASSIGN);
        }

        return n;
    }
}
