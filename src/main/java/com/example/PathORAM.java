package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class PathORAM {

    private final static int BUCKET_SIZE = 4;
    private final List<Bucket> tree = new ArrayList<>();
    private final HashMap<String, Integer> positionMap = new HashMap<>();
    private final HashMap<String, Block> stash = new HashMap<>();
    private final Random random = new Random();
    private int treeHeight;

    public PathORAM(int numBlocks) {
        // Compute tree height from the number of blocks (at least 1)
        this.treeHeight = Math.max(1, (int) Math.ceil(Math.log(numBlocks) / Math.log(2)));
        int numBuckets = (1 << (treeHeight + 1)) - 1;

        // Initialize the tree with empty buckets
        for (int i = 0; i < numBuckets; i++) {
            tree.add(new Bucket());
        }
    }

    public Optional<byte[]> access(String blockId, Optional<byte[]> newData, boolean isWrite) {
        // Step 1: Remap block
        // (If blockId is not yet in the positionMap, assign it a random leaf.)
        Integer prevBlockPos = positionMap.getOrDefault(blockId, random.nextInt(1 << treeHeight) + 1);
        positionMap.put(blockId, random.nextInt(1 << treeHeight) + 1);

        // Step 2: Read path
        readPath(prevBlockPos);

        // Step 3: Update block
        Block block = stash.getOrDefault(blockId, new Block(blockId, Optional.<byte[]>empty()));
        Optional<byte[]> response = block.data;

        if (isWrite) {
            block.data = newData;
            response = newData;
        }

        stash.put(blockId, block);

        // Write the path back to the tree.
        writePath(prevBlockPos);

        // After the standard write, check if the stash has grown beyond a threshold.
        if (stash.size() > 4 * BUCKET_SIZE * treeHeight) {
            resizeTree();
        }

        return response;
    }

    private List<Block> readPath(int leafPos) {
        List<Block> blocksOnPath = new ArrayList<>();
        for (int level = 0; level < treeHeight; level++) {
            int bucketIdx = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, leafPos);
            blocksOnPath.addAll(tree.get(bucketIdx - 1).popAllBlocks());
        }
        // Place any non-dummy block from the read path into the stash.
        for (Block block : blocksOnPath) {
            if (block.id != null) {
                stash.put(block.id, block);
            }
        }
        return blocksOnPath;
    }

    private void writePath(int leafPos) {
        // For each level on the path from the root down to the leaf...
        for (int level = treeHeight - 1; level >= 0; level--) {
            int idxOfBucketOnThisLevelOnPathToLeaf = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, leafPos);
            List<Block> blocksForBucket = new ArrayList<>();

            // Collect all stash blocks that belong to this bucket
            for (Map.Entry<String, Block> stashEntry : stash.entrySet()) {
                Block block = stashEntry.getValue();
                if (idxOfBucketOnThisLevelOnPathToLeaf == computeIdxOfBucketOnThisLevelOnPathToLeaf(level, positionMap.get(block.id))) {
                    blocksForBucket.add(block);
                }
            }

            Bucket newBucket = new Bucket();
            int blocksAdded = 0;

            // Add blocks to the bucket until it's full
            for (Block block : blocksForBucket) {
                if (blocksAdded >= BUCKET_SIZE) {
                    break; // Bucket is full
                }
                // Add the block to the bucket and remove it from the stash
                newBucket.blocks.add(block);
                stash.remove(block.id);
                blocksAdded++;
            }

            // Fill remaining slots with dummy blocks
            while (newBucket.blocks.size() < BUCKET_SIZE) {
                newBucket.blocks.add(new Block(null, Optional.<byte[]>empty()));
            }

            // Update the tree with the new bucket
            tree.set(idxOfBucketOnThisLevelOnPathToLeaf - 1, newBucket);
        }
    }

    /**
     * Computes the 1-based index of the bucket at a given level along the path
     * for a given leaf position. (The tree is stored in a 0-based list, so the
     * bucket index is adjusted later.)
     */
    private int computeIdxOfBucketOnThisLevelOnPathToLeaf(int level, int leafPos) {
        return (leafPos + (1 << (treeHeight - 1))) >> (treeHeight - 1 - level);
    }

    private void resizeTree() {
        // Step 1: Gather all blocks (from both the tree and the stash)
        Map<String, Block> allBlocks = new HashMap<>();
        for (Bucket bucket : tree) {
            for (Block block : bucket.blocks) {
                if (block.id != null) {
                    allBlocks.put(block.id, block);
                }
            }
        }
        for (Map.Entry<String, Block> entry : stash.entrySet()) {
            allBlocks.put(entry.getKey(), entry.getValue());
        }

        // Step 2: Increase tree height by 1.
        treeHeight++;

        // Step 3: Replace the tree with a bigger tree
        tree.clear();
        int newNumBuckets = (1 << (treeHeight + 1)) - 1;
        for (int i = 0; i < newNumBuckets; i++) {
            tree.add(new Bucket());
        }

        // Step 4: Clear the stash and assign each block a new random leaf (using the new tree height).
        stash.clear();
        for (Block block : allBlocks.values()) {
            int newLeaf = random.nextInt(1 << treeHeight);
            positionMap.put(block.id, newLeaf);
            stash.put(block.id, block);
        }

        // Step 5: Flush all blocks from the stash into the new tree.
        int numLeaves = 1 << treeHeight;
        for (int leaf = 0; leaf < numLeaves; leaf++) {
            readPath(leaf); // If the path is not read first, then previously inserted blocks will get lost (instead of being moved to the stack)
            writePath(leaf);
        }
    }

    public void prettyPrintTree() {
        System.out.println("Pretty-printing ORAM tree:");
        System.out.println("Tree height: " + treeHeight);
        System.out.println("Number of buckets: " + tree.size());

        if (tree.isEmpty()) {
            System.out.println("Tree is empty.");
            return;
        }

        // Create a map to group buckets by their level
        Map<Integer, List<Bucket>> levelMap = new HashMap<>();
        int maxLevel = 0;
        for (int i = 0; i < tree.size(); i++) {
            int bucketNumber = i + 1; // Convert to 1-based index
            int level = 31 - Integer.numberOfLeadingZeros(bucketNumber);
            if (!levelMap.containsKey(level)) {
                levelMap.put(level, new ArrayList<Bucket>());
            }
            levelMap.get(level).add(tree.get(i));
            if (level > maxLevel) {
                maxLevel = level;
            }
        }

        // Print each level's buckets
        for (int level = 0; level <= maxLevel; level++) {
            List<Bucket> buckets = levelMap.getOrDefault(level, Collections.<Bucket>emptyList());
            if (buckets.isEmpty()) {
                continue;
            }
            System.out.println("Level " + level + ":");
            for (int idx = 0; idx < buckets.size(); idx++) {
                int bucketNumber = (1 << level) + idx;
                Bucket bucket = buckets.get(idx);
                System.out.print("  Bucket " + (bucketNumber - 1) + ": [");
                for (int i = 0; i < bucket.blocks.size(); i++) {
                    Block block = bucket.blocks.get(i);
                    if (block.id != null) {
                        System.out.print(block.id);
                    } else {
                        System.out.print("dummy");
                    }
                    if (i < bucket.blocks.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");
            }
        }

        // Print stash contents
        System.out.println("\nStash contents:");
        if (stash.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            System.out.println("  " + String.join(", ", stash.keySet()));
        }
    }

    /**
     * Debugging method only: Prints all the keys and leaf positions in the
     * position map.
     */
    public void printPositionMap() {
        System.out.println("Position Map:");
        for (Map.Entry<String, Integer> mapEntry : positionMap.entrySet()) {
            System.out.print(mapEntry.getKey() + ": " + mapEntry.getValue() + ", ");
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
}
