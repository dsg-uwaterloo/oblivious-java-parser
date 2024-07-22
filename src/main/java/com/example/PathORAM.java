package com.example;

import java.util.ArrayList;
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

    // Initialize the position map for each block
    for (int i = 0; i < numBlocks; i++) {
      String blockId = String.valueOf(i);
      int leafIndex = random.nextInt(1 << treeHeight);
      positionMap.put(blockId, leafIndex);
    }
  }

  public Optional<byte[]> access(String blockId, Optional<byte[]> newData, boolean isWrite) {
    // Step 1: Remap block
    Integer prevBlockPos = positionMap.getOrDefault(blockId, random.nextInt(1 << (treeHeight - 1)));
    positionMap.put(blockId, random.nextInt(1 << (treeHeight - 1)));

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
      List<Block> blocksToAddToLevelBucket = new ArrayList<>();
      for (Map.Entry<String, Block> stashEntry : stash.entrySet()) {
        Block stashBlock = stashEntry.getValue();
        if (idxOfBucketOnThisLevelOnPathToLeaf == computeIdxOfBucketOnThisLevelOnPathToLeaf(level,
            positionMap.get(stashBlock.id))) {
          blocksToAddToLevelBucket.add(stashBlock);
        }
      }

      for (Block block : blocksToAddToLevelBucket) {
        stash.remove(block.id);
      }

      Bucket newBucket = new Bucket();
      for (int i = 0; i < BUCKET_SIZE; i++) {
        if (!blocksToAddToLevelBucket.isEmpty()) {
          newBucket.blocks.add(blocksToAddToLevelBucket.remove(0));
        } else {
          newBucket.blocks.add(new Block(null, Optional.<byte[]>empty()));
        }
      }
      tree.set(idxOfBucketOnThisLevelOnPathToLeaf - 1, newBucket);
    }
  }

  private int computeIdxOfBucketOnThisLevelOnPathToLeaf(int level, int leafPos) {
    return (leafPos + (1 << (treeHeight - 1))) >> (treeHeight - 1 - level);
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