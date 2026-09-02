package com.pulseops.correlation.grouping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Disjoint-set (union-find) with path compression and union by rank.
 *
 * <p>Correlation grouping is a connected-components problem: if A links to B and
 * B links to C, then A, B and C are one incident even if A and C did not score
 * above the threshold directly. Union-find gives near-constant-time merges and a
 * single pass to extract the components.
 */
public class UnionFind {

    private final int[] parent;
    private final int[] rank;

    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    public int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]; // path halving
            x = parent[x];
        }
        return x;
    }

    public void union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra == rb) {
            return;
        }
        if (rank[ra] < rank[rb]) {
            int tmp = ra;
            ra = rb;
            rb = tmp;
        }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) {
            rank[ra]++;
        }
    }

    /** Components as lists of member indices, preserving first-seen order. */
    public List<List<Integer>> components() {
        Map<Integer, List<Integer>> byRoot = new LinkedHashMap<>();
        for (int i = 0; i < parent.length; i++) {
            byRoot.computeIfAbsent(find(i), k -> new ArrayList<>()).add(i);
        }
        return new ArrayList<>(byRoot.values());
    }
}
