package com.example;

import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.StaticJavaParser;
import java.util.ArrayList;
import java.util.List;

public class IfStmtDummyVisitor extends VoidVisitorAdapter<Void> {

    private static final String DUMMY_ORAM_ACCESS
            = "oram.access(\"dummy\", Optional.<byte[]>empty(), false);";

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        super.visit(ifStmt, arg);

        // Collect all branches (if, else-if, else)
        List<Statement> allBranches = new ArrayList<Statement>();
        List<Integer> accessCounts = new ArrayList<Integer>();

        // Add the initial if branch
        allBranches.add(ifStmt.getThenStmt());
        accessCounts.add(countOramAccesses(ifStmt.getThenStmt()));

        // Handle else-if branches
        Statement current = ifStmt.getElseStmt().orElse(null);
        while (current instanceof IfStmt) {
            IfStmt elseIfStmt = (IfStmt) current;
            allBranches.add(elseIfStmt.getThenStmt());
            accessCounts.add(countOramAccesses(elseIfStmt.getThenStmt()));
            current = elseIfStmt.getElseStmt().orElse(null);
        }

        // Handle final else branch if it exists
        if (current != null) {
            allBranches.add(current);
            accessCounts.add(countOramAccesses(current));
        }

        // Find maximum number of accesses across all branches
        int maxAccesses = 0;
        for (Integer count : accessCounts) {
            if (count > maxAccesses) {
                maxAccesses = count;
            }
        }

        // If there's no else branch, create one with max accesses
        if (current == null) {
            BlockStmt newElseBlock = new BlockStmt();
            addDummyAccesses(newElseBlock, maxAccesses);

            // Find the last else-if and add the else block to it
            Statement lastStmt = ifStmt;
            while (lastStmt instanceof IfStmt
                    && ((IfStmt) lastStmt).getElseStmt().isPresent()
                    && ((IfStmt) lastStmt).getElseStmt().get() instanceof IfStmt) {
                lastStmt = ((IfStmt) lastStmt).getElseStmt().get();
            }
            ((IfStmt) lastStmt).setElseStmt(newElseBlock);
        }

        // Balance all branches to have the same number of accesses
        for (int i = 0; i < allBranches.size(); i++) {
            if (accessCounts.get(i) < maxAccesses) {
                addDummyAccesses(allBranches.get(i), maxAccesses - accessCounts.get(i));
            }
        }
    }

    private int countOramAccesses(Statement stmt) {
        OramAccessCounter counter = new OramAccessCounter();
        stmt.accept(counter, null);
        return counter.getCount();
    }

    private void addDummyAccesses(Statement stmt, int count) {
        if (!(stmt instanceof BlockStmt)) {
            BlockStmt block = new BlockStmt();
            block.addStatement(stmt);
            if (stmt.getParentNode().isPresent()) {
                stmt.replace(block);
            }
            stmt = block;
        }

        BlockStmt block = (BlockStmt) stmt;
        for (int i = 0; i < count; i++) {
            block.addStatement(StaticJavaParser.parseStatement(DUMMY_ORAM_ACCESS));
        }
    }
}

class OramAccessCounter extends VoidVisitorAdapter<Void> {

    private int count = 0;

    @Override
    public void visit(MethodCallExpr methodCall, Void arg) {
        super.visit(methodCall, arg);
        if (methodCall.getNameAsString().equals("access")
                && methodCall.getScope().isPresent()
                && methodCall.getScope().get().toString().equals("oram")) {
            count++;
        }
    }

    public int getCount() {
        return count;
    }
}
