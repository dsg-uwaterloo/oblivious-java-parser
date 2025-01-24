package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * A transformer that converts eligible if statements into branchless, oblivious
 * code using unique masks.
 */
public class IfStmtVisitor extends ModifierVisitor<Void> {

    private static int maskCounter = 1; // Counter for unique mask variable names

    @Override
    public Visitable visit(IfStmt ifStmt, Void arg) {
        super.visit(ifStmt, arg);

        // Check if the if statement has both 'then' and 'else' blocks
        boolean hasElse = ifStmt.hasElseBlock();

        Statement thenStmt = ifStmt.getThenStmt();
        Optional<Statement> elseStmtOpt = ifStmt.getElseStmt();

        // We are looking for if statements (with or without else) that assign values to a variable
        // TODO: Handle arbitrary cases
        if (!(thenStmt.isBlockStmt() || thenStmt.isExpressionStmt())) {
            return ifStmt;
        }

        if (hasElse && !(elseStmtOpt.get().isBlockStmt() || elseStmtOpt.get().isExpressionStmt())) {
            return ifStmt;
        }

        // Extract assignment expressions from then and else blocks
        Optional<AssignExpr> thenAssignOpt = extractAssignmentExpr(thenStmt);
        Optional<AssignExpr> elseAssignOpt = hasElse ? extractAssignmentExpr(elseStmtOpt.get()) : Optional.<AssignExpr>empty();

        if (!thenAssignOpt.isPresent()) {
            return ifStmt;
        }

        if (hasElse && !elseAssignOpt.isPresent()) {
            return ifStmt;
        }

        AssignExpr thenAssign = thenAssignOpt.get();
        AssignExpr elseAssign = elseAssignOpt.orElse(null);

        // Ensure that in case of else, both assignments assign to the same variable
        if (hasElse) {
            if (!thenAssign.getTarget().equals(elseAssign.getTarget())) {
                return ifStmt;
            }
        }

        // Extract variable name and assigned values
        Expression target = thenAssign.getTarget();
        Expression valueTrue = thenAssign.getValue();
        Expression valueFalse = hasElse ? elseAssign.getValue() : new NameExpr(target.toString());

        // Resolve type
        String targetType = resolveType(target);
        if (targetType == null) {
            System.err.println("Unable to resolve type for: " + target + ". Skipping transformation.");
            return ifStmt;
        }

        boolean isBoolean = targetType.equals("boolean");

        // Generate unique mask variable name
        String maskVarName = "mask" + maskCounter++;

        // Create mask declaration
        ConditionalExpr maskCondition = new ConditionalExpr(
                new EnclosedExpr(ifStmt.getCondition()),
                new LongLiteralExpr("-1L"),
                new LongLiteralExpr("0L")
        );
        VariableDeclarationExpr maskDecl = new VariableDeclarationExpr(
                new VariableDeclarator(
                        PrimitiveType.longType(),
                        maskVarName,
                        maskCondition
                ),
                Modifier.finalModifier()
        );
        Statement maskStatement = new ExpressionStmt(maskDecl);

        // Create branchless assignment
        Expression branchlessExpr;
        if (isBoolean) {
            // Convert boolean to long using ternary operators
            Expression trueLong = convertBooleanToLong(valueTrue);
            Expression falseLong = hasElse ? convertBooleanToLong(valueFalse) : new EnclosedExpr(
                    new ConditionalExpr(
                            new NameExpr(target.toString()),
                            new LongLiteralExpr("-1L"),
                            new LongLiteralExpr("0L")
                    )
            );

            // Perform bitwise operations
            EnclosedExpr maskAndTrue = new EnclosedExpr(
                    new BinaryExpr(
                            new NameExpr(maskVarName),
                            trueLong,
                            BinaryExpr.Operator.BINARY_AND
                    )
            );

            UnaryExpr notMask = new UnaryExpr(
                    new NameExpr(maskVarName),
                    UnaryExpr.Operator.BITWISE_COMPLEMENT
            );

            EnclosedExpr notMaskAndFalse = new EnclosedExpr(
                    new BinaryExpr(
                            notMask,
                            falseLong,
                            BinaryExpr.Operator.BINARY_AND
                    )
            );

            EnclosedExpr combined = new EnclosedExpr(
                    new BinaryExpr(
                            maskAndTrue,
                            notMaskAndFalse,
                            BinaryExpr.Operator.BINARY_OR
                    )
            );

            // Convert back to boolean by comparing with 0L
            branchlessExpr = new BinaryExpr(
                    combined,
                    new LongLiteralExpr("0L"),
                    BinaryExpr.Operator.NOT_EQUALS
            );
        } else {
            // For non-boolean types, proceed with standard bitwise operations
            BinaryExpr maskAndTrue = new BinaryExpr(
                    new NameExpr(maskVarName),
                    castToLongIfNecessary(valueTrue),
                    BinaryExpr.Operator.BINARY_AND
            );

            UnaryExpr notMask = new UnaryExpr(
                    new NameExpr(maskVarName),
                    UnaryExpr.Operator.BITWISE_COMPLEMENT
            );

            BinaryExpr notMaskAndFalse = new BinaryExpr(
                    notMask,
                    castToLongIfNecessary(valueFalse),
                    BinaryExpr.Operator.BINARY_AND
            );

            BinaryExpr combined = new BinaryExpr(
                    maskAndTrue,
                    notMaskAndFalse,
                    BinaryExpr.Operator.BINARY_OR
            );

            branchlessExpr = combined;
        }

        AssignExpr operateAssign = new AssignExpr(
                new NameExpr(target.toString()),
                branchlessExpr,
                AssignExpr.Operator.ASSIGN
        );
        Statement operateAssignment = new ExpressionStmt(operateAssign);

        List<Statement> transformedStmts = new ArrayList<>();
        transformedStmts.add(maskStatement);
        transformedStmts.add(operateAssignment);

        Optional<Node> parentNodeOpt = ifStmt.getParentNode();
        if (!parentNodeOpt.isPresent()) {
            System.out.println("The if statement does not have a parent node.");
            return ifStmt;
        }

        Node parentNode = parentNodeOpt.get();

        if (!(parentNode instanceof BlockStmt)) {
            System.out.println("Parent node is not a BlockStmt. Cannot replace the if statement.");
            return ifStmt;
        }

        BlockStmt parentBlock = (BlockStmt) parentNode;
        // Get the list of statements in the parent block
        List<Statement> statements = parentBlock.getStatements();

        // Find the index of the if statement in the parent block
        int index = statements.indexOf(ifStmt);
        if (index != -1) {
            // Remove the if statement
            statements.remove(index);
            // Insert the new statements at the same position
            statements.addAll(index, transformedStmts);
        }

        return null;
    }

    /**
     * Resolves the type of a given expression using the Symbol Solver.
     *
     * @param expr The expression whose type needs to be resolved.
     * @return The type name as a String, or null if unresolved.
     */
    private String resolveType(Expression expr) {
        try {
            ResolvedType resolvedType = expr.calculateResolvedType();
            return resolvedType.describe();
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
            // Handle unresolved symbols or unsupported operations
            System.err.println("Unable to resolve type for expression: " + expr + ". Skipping transformation.");
            return null;
        }
    }

    /**
     * Converts a boolean expression to a long using ternary operators.
     *
     * @param expr The boolean expression.
     * @return The equivalent long expression.
     */
    private Expression convertBooleanToLong(Expression expr) {
        if (expr.isBooleanLiteralExpr()) {
            // Directly convert boolean literals
            boolean value = expr.asBooleanLiteralExpr().getValue();
            return value ? new LongLiteralExpr("-1L") : new LongLiteralExpr("0L");
        } else if (expr.isNameExpr()) {
            // For variables, use ternary to convert
            return new ConditionalExpr(
                    expr.clone(),
                    new LongLiteralExpr("-1L"),
                    new LongLiteralExpr("0L")
            );
        } else {
            // For more complex expressions, encapsulate with ternary
            return new ConditionalExpr(
                    expr.clone(),
                    new LongLiteralExpr("-1L"),
                    new LongLiteralExpr("0L")
            );
        }
    }

    /**
     * Helper method to extract an assignment expression from a statement.
     *
     * @param stmt The statement to extract from.
     * @return An Optional containing the AssignExpr if present.
     */
    private Optional<AssignExpr> extractAssignmentExpr(Statement stmt) {
        if (stmt.isExpressionStmt()) {
            ExpressionStmt exprStmt = stmt.asExpressionStmt();
            if (exprStmt.getExpression().isAssignExpr()) {
                return Optional.of(exprStmt.getExpression().asAssignExpr());
            }
        } else if (stmt.isBlockStmt()) {
            BlockStmt block = stmt.asBlockStmt();
            if (block.getStatements().size() == 1 && block.getStatement(0).isExpressionStmt()) {
                ExpressionStmt exprStmt = block.getStatement(0).asExpressionStmt();
                if (exprStmt.getExpression().isAssignExpr()) {
                    return Optional.of(exprStmt.getExpression().asAssignExpr());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Casts an expression to long if it is not already of type long.
     *
     * @param expr The expression to cast.
     * @return The original expression or a casted expression.
     */
    private Expression castToLongIfNecessary(Expression expr) {
        // If the expression is already a long literal or a name expression, return as is
        if (expr.isLongLiteralExpr() || expr.isNameExpr() || expr.isFieldAccessExpr()) {
            return expr;
        }
        // Otherwise, cast it to long
        return new CastExpr(new PrimitiveType(PrimitiveType.Primitive.LONG), expr.clone());
    }
}
