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
        this.treeHeight = Math.max(1, (int) Math.ceil(Math.log(numBlocks) / Math.log(2)));
        int numBuckets = (1 << (treeHeight + 1)) - 1;

        // Initialize the tree with empty buckets
        for (int i = 0; i < numBuckets; i++) {
            tree.add(new Bucket());
        }

        // // Initialize the position map for each block
        // for (int i = 0; i < numBlocks; i++) {
        //     String blockId = String.valueOf(i);
        //     int leafIndex = random.nextInt(1 << treeHeight);
        //     positionMap.put(blockId, leafIndex);
        // }
    }

    public Optional<byte[]> access(String blockId, Optional<byte[]> newData, boolean isWrite) {
        // Step 1: Remap block
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

        writePath(prevBlockPos);

        return response;
    }

    private List<Block> readPath(int leafPos) {
        List<Block> blocksOnPath = new ArrayList<>();
        for (int level = 0; level < treeHeight; level++) {
            int bucketIdx = computeIdxOfBucketOnThisLevelOnPathToLeaf(level, leafPos);
            blocksOnPath.addAll(tree.get(bucketIdx - 1).popAllBlocks());
        }
        for (Block block : blocksOnPath) {
            if (block.id != null) {
                stash.put(block.id, block);
            }
        }
        return blocksOnPath;
    }

    private void writePath(int leafPos) {
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
                newBucket.blocks.add(new Block(null, Optional.empty()));
            }

            // Update the tree with the new bucket
            tree.set(idxOfBucketOnThisLevelOnPathToLeaf - 1, newBucket);
        }
    }

    private int computeIdxOfBucketOnThisLevelOnPathToLeaf(int level, int leafPos) {
        return (leafPos + (1 << (treeHeight - 1))) >> (treeHeight - 1 - level);
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
            levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(tree.get(i));
            if (level > maxLevel) {
                maxLevel = level;
            }
        }

        // Print each level's buckets
        for (int level = 0; level <= maxLevel; level++) {
            List<Bucket> buckets = levelMap.getOrDefault(level, Collections.emptyList());
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
