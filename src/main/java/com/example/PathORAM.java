package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class PathORAM implements AutoCloseable {
    private final static int BUCKET_SIZE = 4;
    private final List<Bucket> tree = new ArrayList<>();
    private final List<PositionMapEntry> positionMap = new ArrayList<>();
    private final List<Block> stash = new ArrayList<>();
    private final Random random = new Random();
    private int treeHeight;
    private static PrintWriter csvLogger;

    public PathORAM(int numBlocks) {
        // Initialize CSV logger
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        try {
            csvLogger = new PrintWriter(new FileWriter("oram_performance_" + timestamp + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        csvLogger.println(
                "timestamp,operation,duration_ns,blockId,isWrite,treeHeight,stashSize,resized,leafPos,oldTreeHeight");

        // Initialize ORAM
        this.treeHeight = Math.max(1, (int) Math.ceil(Math.log(numBlocks) / Math.log(2)));
        int numBuckets = (1 << (treeHeight + 1)) - 1;
        for (int i = 0; i < numBuckets; i++) {
            tree.add(new Bucket());
        }
    }

    @Override
    public void close() {
        if (csvLogger != null) {
            csvLogger.close();
        }
    }

    public Optional<byte[]> access(String blockId, Optional<byte[]> newData, boolean isWrite) {
        long startAccess = System.nanoTime();

        // Step 1: Remap block
        int prevBlockPos = findPositionInPositionMap(blockId);
        if (prevBlockPos == -1) {
            prevBlockPos = random.nextInt(1 << treeHeight) + 1;
        }
        int newPosition = random.nextInt(1 << treeHeight) + 1;
        updatePositionMap(blockId, newPosition);

        // Step 2: Read path
        readPath(prevBlockPos);

        // Step 3: Update block
        Block block = findBlockInStash(blockId).orElse(new Block(blockId, Optional.empty()));
        Optional<byte[]> response = block.data;

        if (isWrite) {
            block.data = newData;
            response = newData;
        }

        // Update stash
        stash.removeIf(b -> blockId.equals(b.id));
        stash.add(block);

        // Write the path back to the tree.
        writePath(prevBlockPos);

        // Check if the stash has grown beyond a threshold.
        int oldTreeHeight = treeHeight;
        boolean resized = false;
        if (stash.size() > 4 * treeHeight) {
            resizeTree();
            resized = true;
        }

        long duration = System.nanoTime() - startAccess;
        csvLogger.printf("%d,access,%d,%s,%b,%d,%d,%b,,%n",
                System.currentTimeMillis(), duration, blockId, isWrite, treeHeight, stash.size(), resized);
        return response;
    }

    private Optional<Block> findBlockInStash(String blockId) {
        for (Block b : stash) {
            if (blockId.equals(b.id)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    private int findPositionInPositionMap(String blockId) {
        for (PositionMapEntry entry : positionMap) {
            if (blockId.equals(entry.blockId)) {
                return entry.position;
            }
        }
        return -1;
    }

    private void updatePositionMap(String blockId, int newPosition) {
        for (PositionMapEntry entry : positionMap) {
            if (blockId.equals(entry.blockId)) {
                entry.position = newPosition;
                return;
            }
        }
        positionMap.add(new PositionMapEntry(blockId, newPosition));
    }

    private List<Block> readPath(int leafPos) {
        long start = System.nanoTime();

        List<Block> blocksOnPath = new ArrayList<>();
        for (int level = 0; level < treeHeight; level++) {
            int bucketIdx = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, leafPos);
            blocksOnPath.addAll(tree.get(bucketIdx - 1).popAllBlocks());
        }
        for (Block block : blocksOnPath) {
            if (block.id != null) {
                stash.removeIf(b -> block.id.equals(b.id));
                stash.add(block);
            }
        }

        long duration = System.nanoTime() - start;
        csvLogger.printf("%d,readPath,%d,,,%d,,,%d,%n",
                System.currentTimeMillis(), duration, treeHeight, leafPos);
        return blocksOnPath;
    }

    private void writePath(int leafPos) {
        long start = System.nanoTime();

        for (int level = treeHeight - 1; level >= 0; level--) {
            int bucketIdx = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, leafPos);
            List<Block> blocksForBucket = new ArrayList<>();

            for (Block block : stash) {
                if (block.id == null)
                    continue;
                int pos = findPositionInPositionMap(block.id);
                if (pos == -1)
                    continue;
                int computedBucketIdx = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, pos);
                if (computedBucketIdx == bucketIdx) {
                    blocksForBucket.add(block);
                }
            }

            Bucket newBucket = new Bucket();
            int blocksAdded = 0;

            for (Block block : blocksForBucket) {
                if (blocksAdded >= BUCKET_SIZE)
                    break;
                newBucket.blocks.add(block);
                stash.removeIf(b -> block.id.equals(b.id));
                blocksAdded++;
            }

            while (newBucket.blocks.size() < BUCKET_SIZE) {
                newBucket.blocks.add(new Block(null, Optional.empty()));
            }

            tree.set(bucketIdx - 1, newBucket);
        }

        long duration = System.nanoTime() - start;
        csvLogger.printf("%d,writePath,%d,,,%d,,,%d,%n",
                System.currentTimeMillis(), duration, treeHeight, leafPos);
    }

    private int computeIdxOfBucketOnThisLevelOnPathToLeaf(int level, int leafPos) {
        return (leafPos + (1 << (treeHeight - 1))) >> (treeHeight - 1 - level);
    }

    private void resizeTree() {
        long start = System.nanoTime();
        int oldTreeHeight = treeHeight;

        Map<String, Block> allBlocks = new java.util.HashMap<>();
        for (Bucket bucket : tree) {
            for (Block block : bucket.blocks) {
                if (block.id != null) {
                    allBlocks.put(block.id, block);
                }
            }
        }
        for (Block block : stash) {
            if (block.id != null) {
                allBlocks.put(block.id, block);
            }
        }

        treeHeight++;
        tree.clear();
        int newNumBuckets = (1 << (treeHeight + 1)) - 1;
        for (int i = 0; i < newNumBuckets; i++) {
            tree.add(new Bucket());
        }

        stash.clear();
        positionMap.clear();
        for (Block block : allBlocks.values()) {
            int newLeaf = random.nextInt(1 << treeHeight) + 1;
            updatePositionMap(block.id, newLeaf);
            stash.add(block);
        }

        int numLeaves = 1 << treeHeight;
        for (int leaf = 1; leaf <= numLeaves; leaf++) {
            readPath(leaf);
            writePath(leaf);
        }

        long duration = System.nanoTime() - start;
        csvLogger.printf("%d,resizeTree,%d,,,%d,,,,%d%n",
                System.currentTimeMillis(), duration, treeHeight, oldTreeHeight);
    }

    // Remaining methods (prettyPrintTree, printPositionMap, etc.) remain unchanged

    private static class PositionMapEntry {
        String blockId;
        int position;

        PositionMapEntry(String blockId, int position) {
            this.blockId = blockId;
            this.position = position;
        }
    }

    private static class Block {
        private String id;
        private Optional<byte[]> data;

        Block(String id, Optional<byte[]> data) {
            this.id = id;
            this.data = data;
        }
    }

    private static class Bucket {
        List<Block> blocks;

        Bucket() {
            this.blocks = new ArrayList<>(BUCKET_SIZE);
        }

        public List<Block> popAllBlocks() {
            List<Block> blocks = new ArrayList<>(this.blocks);
            this.blocks.clear();
            return blocks;
        }
    }

    // The prettyPrintTree and printPositionMap methods need adjustments for stash
    // and positionMap:
    public void prettyPrintTree() {
        System.out.println("Pretty-printing ORAM tree:");
        System.out.println("Tree height: " + treeHeight);
        System.out.println("Number of buckets: " + tree.size());

        if (tree.isEmpty()) {
            System.out.println("Tree is empty.");
            return;
        }

        Map<Integer, List<Bucket>> levelMap = new java.util.HashMap<>();
        int maxLevel = 0;
        for (int i = 0; i < tree.size(); i++) {
            int bucketNumber = i + 1;
            int level = 31 - Integer.numberOfLeadingZeros(bucketNumber);
            levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(tree.get(i));
            maxLevel = Math.max(maxLevel, level);
        }

        for (int level = 0; level <= maxLevel; level++) {
            List<Bucket> buckets = levelMap.getOrDefault(level, Collections.emptyList());
            if (buckets.isEmpty())
                continue;
            System.out.println("Level " + level + ":");
            for (int idx = 0; idx < buckets.size(); idx++) {
                int bucketNumber = (1 << level) + idx;
                Bucket bucket = buckets.get(idx);
                System.out.print("  Bucket " + (bucketNumber - 1) + ": [");
                for (int i = 0; i < bucket.blocks.size(); i++) {
                    Block block = bucket.blocks.get(i);
                    System.out.print(block.id != null ? block.id : "dummy");
                    if (i < bucket.blocks.size() - 1)
                        System.out.print(", ");
                }
                System.out.println("]");
            }
        }

        System.out.println("\nStash contents:");
        if (stash.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            List<String> ids = new ArrayList<>();
            for (Block b : stash) {
                if (b.id != null)
                    ids.add(b.id);
            }
            System.out.println("  " + String.join(", ", ids));
        }
    }

    public void printPositionMap() {
        System.out.println("Position Map:");
        for (PositionMapEntry entry : positionMap) {
            System.out.print(entry.blockId + ": " + entry.position + ", ");
        }
        System.out.println();
    }
}