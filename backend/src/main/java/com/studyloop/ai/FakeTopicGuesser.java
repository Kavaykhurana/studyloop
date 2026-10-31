package com.studyloop.ai;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keyword heuristic that lets the offline {@link FakeLlmClient} pick a plausible subject and topic, so the demo campus
 * still routes and groups doubts sensibly when no Gemini key is configured. Never used when Gemini is enabled.
 */
final class FakeTopicGuesser {

    record Guess(String subjectCode, String topic) {}

    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("Arrays & Strings", List.of("array", "string", "substring", "two pointer", "sliding window",
                    "anagram", "palindrome", "kadane")),
            Map.entry("Linked Lists", List.of("linked list", "singly", "doubly", "node next", "cycle in")),
            Map.entry("Stacks & Queues", List.of("stack", "queue", "infix", "postfix", "deque", "parenthes")),
            Map.entry("Trees & BST", List.of("bst", "binary search tree", "binary tree", "avl", "red-black",
                    "inorder", "preorder", "postorder", "tree height")),
            Map.entry("Graphs", List.of("bfs", "dfs", "dijkstra", "topological", "spanning tree", "kruskal", "prim",
                    "bellman", "adjacency")),
            Map.entry("Dynamic Programming", List.of("dynamic programming", " dp ", "memoiz", "knapsack",
                    "tabulation", "lcs", "climbing stairs", "overlapping subproblem")),
            Map.entry("Sorting & Searching", List.of("sort", "binary search", "quicksort", "merge sort", "heapsort",
                    "pivot")),
            Map.entry("Complexity Analysis", List.of("time complexity", "big o", "big-o", "o(n", "o(log", "asymptotic",
                    "master theorem")),
            Map.entry("ER Modelling", List.of("er diagram", "entity", "weak entity", "cardinality", "er model")),
            Map.entry("Relational Algebra", List.of("relational algebra", "projection", "selection operator",
                    "natural join", "division operator")),
            Map.entry("SQL Queries", List.of("sql", "select ", "group by", "having", "join", "subquery", "query")),
            Map.entry("Normalization", List.of("bcnf", "3nf", "2nf", "1nf", "normal form", "normaliz",
                    "functional dependenc", "candidate key", "superkey", "decompos", "closure")),
            Map.entry("Transactions & Concurrency", List.of("transaction", "serializab", "two-phase locking", "2pl",
                    "wait-die", "wound-wait", "acid", "concurrency control", "conflict serial")),
            Map.entry("Indexing & B+ Trees", List.of("b+ tree", "b+tree", "index", "b-tree", "hashing")),
            Map.entry("Query Processing", List.of("query plan", "query optimi", "query processing", "cost estimat")),
            Map.entry("Recovery", List.of("recovery", "undo", "redo", "checkpoint", "write-ahead", "wal log")),
            Map.entry("Processes & Threads", List.of("process", "fork", "pcb", "context switch", "zombie")),
            Map.entry("CPU Scheduling", List.of("round robin", "scheduling", "fcfs", "sjf", "waiting time",
                    "turnaround", "quantum", "gantt")),
            Map.entry("Synchronization", List.of("semaphore", "mutex", "producer", "consumer", "critical section",
                    "race condition", "monitor", "peterson")),
            Map.entry("Deadlocks", List.of("deadlock", "banker", "safe state", "resource allocation graph")),
            Map.entry("Memory Management", List.of("fragmentation", "first fit", "best fit", "segmentation",
                    "memory allocation")),
            Map.entry("Paging & Virtual Memory", List.of("paging", "page table", "tlb", "page fault",
                    "virtual memory", "lru", "fifo page", "thrashing", "effective access time")),
            Map.entry("File Systems", List.of("file system", "inode", "fat ", "directory structure")),
            Map.entry("Disk Scheduling", List.of("disk scheduling", "sstf", "scan algorithm", "c-scan", "seek time",
                    "cylinder")),
            Map.entry("OSI & TCP/IP Models", List.of("osi", "tcp/ip model", "layer", "encapsulation")),
            Map.entry("Error Control & CRC", List.of("crc", "checksum", "hamming", "parity", "generator polynomial",
                    "error detection")),
            Map.entry("MAC & Ethernet", List.of("csma", "ethernet", "mac address", "aloha", "collision")),
            Map.entry("IP Addressing & Subnetting", List.of("subnet", "cidr", "ip address", "ipv4", "netmask",
                    "/24", "/26")),
            Map.entry("Routing Algorithms", List.of("routing", "distance vector", "link state", "ospf", "rip ",
                    "count to infinity")),
            Map.entry("TCP & Congestion Control", List.of("tcp", "congestion", "cwnd", "slow start",
                    "three-way handshake", "sliding window protocol", "go-back-n", "selective repeat")),
            Map.entry("DNS & HTTP", List.of("dns", "http", "url", "cookie", "domain name")),
            Map.entry("Network Security Basics", List.of("rsa", "encryption", "firewall", "ssl", "tls",
                    "symmetric key", "digital signature")),
            Map.entry("Classes & Objects", List.of("class", "object", "constructor", "static", "this keyword")),
            Map.entry("Inheritance & Polymorphism", List.of("inheritance", "polymorphism", "override", "overload",
                    "super(", "extends")),
            Map.entry("Interfaces & Abstract Classes", List.of("interface", "abstract class", "implements",
                    "default method")),
            Map.entry("Exception Handling", List.of("exception", "try catch", "try-catch", "finally", "throws",
                    "checked exception")),
            Map.entry("Collections Framework", List.of("arraylist", "hashmap", "hashset", "iterator", "collection",
                    "concurrentmodification", "treemap", "linkedlist")),
            Map.entry("Generics", List.of("generic", "wildcard", "type parameter", "<t>", "type erasure")),
            Map.entry("Multithreading", List.of("thread", "synchronized", "runnable", "executor", "volatile")),
            Map.entry("Streams & Lambdas", List.of("stream", "lambda", "map(", "filter(", "collect(", "optional")),
            Map.entry("Sets & Relations", List.of("set ", "relation is", "reflexive", "symmetric", "transitive",
                    "equivalence relation", "power set")),
            Map.entry("Logic & Proofs", List.of("proof", "induction", "contrapositive", "tautology", "predicate",
                    "propositional")),
            Map.entry("Combinatorics", List.of("permutation", "combination", "pigeonhole", "ncr", "arrangements",
                    "inclusion-exclusion")),
            Map.entry("Graph Theory", List.of("euler", "hamilton", "chromatic", "planar", "degree sequence",
                    "graph theory")),
            Map.entry("Probability Basics", List.of("probability", "dice", " die", "coin", "bayes",
                    "conditional probability")),
            Map.entry("Random Variables & Distributions", List.of("random variable", "expectation", "variance",
                    "binomial", "poisson", "distribution", "pmf", "pdf")),
            Map.entry("Recurrence Relations", List.of("recurrence", "characteristic equation", "a_n",
                    "generating function")),
            Map.entry("Linear Algebra Basics", List.of("matrix", "eigen", "determinant", "rank of", "vector space",
                    "linear transformation")));

    /** Subject-level vocabulary for doubts that match no specific topic keyword. */
    private static final Map<String, List<String>> SUBJECT_WORDS = Map.of(
            "DSA", List.of("algorithm", "data structure", "leetcode", "complexity", "recursion"),
            "DBMS", List.of("dbms", "database", "relation", "table", "tuple", "attribute", "schema"),
            "OS", List.of("operating system", "kernel", "cpu", "memory", "os "),
            "CN", List.of("network", "packet", "frame", "protocol", "router", "bandwidth"),
            "OOPJ", List.of("java", "oop", "jvm", "method"),
            "MATH", List.of("discrete", "maths", "math", "theorem", "formula"));

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private FakeTopicGuesser() {}

    /** @param subjects subject code to topic list, in catalog order, as sent in the prompt */
    static Guess guess(String text, Map<String, List<String>> subjects) {
        String hay = " " + SPACES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ") + " ";
        String bestSubject = null;
        String bestTopic = null;
        int bestScore = 0;
        for (Map.Entry<String, List<String>> e : subjects.entrySet()) {
            for (String topic : e.getValue()) {
                int score = hits(hay, KEYWORDS.getOrDefault(topic, List.of())) * 3;
                if (score > bestScore) {
                    bestScore = score;
                    bestSubject = e.getKey();
                    bestTopic = topic;
                }
            }
        }
        for (Map.Entry<String, List<String>> e : subjects.entrySet()) {
            int score = hits(hay, SUBJECT_WORDS.getOrDefault(e.getKey(), List.of()));
            if (score > bestScore) {
                bestScore = score;
                bestSubject = e.getKey();
                bestTopic = "General";
            }
        }
        if (bestSubject == null) {
            String first = subjects.isEmpty() ? "DSA" : subjects.keySet().iterator().next();
            return new Guess(first, "General");
        }
        return new Guess(bestSubject, bestTopic);
    }

    private static int hits(String hay, List<String> words) {
        int n = 0;
        for (String w : words) {
            if (hay.contains(w)) {
                n++;
            }
        }
        return n;
    }
}
