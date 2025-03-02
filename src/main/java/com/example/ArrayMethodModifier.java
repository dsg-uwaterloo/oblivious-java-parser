package com.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.visitor.Visitable;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.resolution.types.ResolvedType;

public class ArrayMethodModifier extends ModifierVisitor<Void> {

    private final Map<String, Expression> arrayLengths = new HashMap<>();
    private final Set<String> arrayNamesToSkip = new HashSet<>(Arrays.asList("args"));

    // Array initializers
    // Do not remove the initializer yet because visit(FieldAccessExpr n, Void arg)
    // expects the array variable to exist
    @Override
    public Visitable visit(VariableDeclarator n, Void arg) {
        Optional<Expression> initializer = n.getInitializer();
        String arrayName = n.getName().asString();

        if (initializer.isPresent() && initializer.get() instanceof ArrayCreationExpr) {
            ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) initializer.get();
            NodeList<ArrayCreationLevel> dimensions = arrayCreationExpr.getLevels();

            if (!dimensions.isEmpty()) {
                ArrayCreationLevel firstDimension = dimensions.get(0);
                Optional<Expression> dimension = firstDimension.getDimension();
                Expression sizeExpression = dimension.get();
                arrayLengths.put(arrayName, sizeExpression);
            }
        }

        return super.visit(n, arg);
    }

    // Array length
    @Override
    public Visitable visit(FieldAccessExpr n, Void arg) {
        String fieldName = n.getName().asString();
        if (fieldName.equals("length")) {
            Expression scope = n.getScope();
            ResolvedType scopeType = scope.calculateResolvedType();
            if (scopeType.isArray()) {
                String arrayName = scope.toString();
                if (!arrayNamesToSkip.contains(arrayName) && arrayLengths.containsKey(arrayName)) {
                    return arrayLengths.get(arrayName);
                }
            }
        }
        return super.visit(n, arg);
    }
}
