package com.studyloop.seed;

import java.util.List;
import java.util.Map;

/** The 25 demo personas. Subject entries are (code, kind, level). */
public final class SeedCatalog {

    public record Entry(String code, String kind, int level) {}

    public record Persona(String name, int year, String branch, String bio, List<Entry> subjects) {

        public String email() {
            return name.toLowerCase().replace(' ', '.') + "@demo.studyloop.app";
        }
    }

    public static final String LEARNER = "Aanya Sharma";
    public static final String HELPER = "Rohan Mehta";

    /** Helpers who were active within the last 12 hours, per the demo script. */
    public static final Map<String, Integer> RECENT_HELPERS_HOURS = Map.of(
            "Rohan Mehta", 1, "Simran Kaur", 3, "Tanvi Rao", 6, "Meher Bajwa", 9, "Priya Nair", 11);

    public static final List<Persona> PERSONAS = List.of(
            new Persona("Aanya Sharma", 2, "COE", "Second year, trying to survive DBMS.",
                    List.of(e("DBMS", "NEEDS_HELP", 2), e("OS", "NEEDS_HELP", 2), e("MATH", "STRONG", 4))),
            new Persona("Rohan Mehta", 4, "COE", "Final year. Happiest explaining trees, graphs and normal forms.",
                    List.of(e("DSA", "STRONG", 5), e("DBMS", "STRONG", 4), e("OOPJ", "STRONG", 4))),
            new Persona("Ishaan Gupta", 3, "COE", "Third year, competitive programming on weekends.",
                    List.of(e("DSA", "STRONG", 4), e("CN", "NEEDS_HELP", 2))),
            new Persona("Simran Kaur", 4, "COBS", "Fourth year, teaching assistant for the DBMS lab.",
                    List.of(e("DBMS", "STRONG", 5), e("OS", "STRONG", 4))),
            new Persona("Arjun Malhotra", 2, "COE", "Second year, still fighting with Java generics.",
                    List.of(e("DSA", "NEEDS_HELP", 2), e("OOPJ", "NEEDS_HELP", 3))),
            new Persona("Priya Nair", 3, "ENC", "Third year ENC, networks are my thing.",
                    List.of(e("CN", "STRONG", 5), e("OS", "STRONG", 3))),
            new Persona("Kabir Sethi", 1, "COE", "First year, new to everything.",
                    List.of(e("MATH", "NEEDS_HELP", 2), e("DSA", "NEEDS_HELP", 1))),
            new Persona("Meher Bajwa", 4, "COE", "Fourth year, kernel hacking in free time.",
                    List.of(e("OS", "STRONG", 5), e("CN", "STRONG", 4))),
            new Persona("Dev Khanna", 2, "COBS", "Second year, SQL is starting to click.",
                    List.of(e("DBMS", "NEEDS_HELP", 2), e("DSA", "NEEDS_HELP", 3))),
            new Persona("Tanvi Rao", 3, "COE", "Third year, Java and a bit too much coffee.",
                    List.of(e("OOPJ", "STRONG", 5), e("DSA", "STRONG", 3))),
            new Persona("Harsh Vardhan", 2, "ECE", "Second year ECE, picking up networks.",
                    List.of(e("CN", "NEEDS_HELP", 2), e("MATH", "NEEDS_HELP", 3))),
            new Persona("Navya Iyer", 4, "COE", "Fourth year, discrete maths is secretly fun.",
                    List.of(e("MATH", "STRONG", 5), e("DSA", "STRONG", 4))),
            new Persona("Aditya Bansal", 1, "COE", "First year, learning Java the hard way.",
                    List.of(e("OOPJ", "NEEDS_HELP", 2), e("MATH", "NEEDS_HELP", 2))),
            new Persona("Riya Chopra", 3, "COBS", "Third year, queries and indexes.",
                    List.of(e("DBMS", "STRONG", 4), e("OOPJ", "STRONG", 3))),
            new Persona("Yash Arora", 2, "COE", "Second year, scheduling algorithms confuse me.",
                    List.of(e("OS", "NEEDS_HELP", 2), e("CN", "NEEDS_HELP", 2))),
            new Persona("Sneha Pillai", 4, "ENC", "Fourth year ENC, loves graph problems.",
                    List.of(e("DSA", "STRONG", 4), e("MATH", "STRONG", 4))),
            new Persona("Karan Gill", 2, "COE", "Second year, preparing for placements.",
                    List.of(e("DSA", "NEEDS_HELP", 2), e("DBMS", "NEEDS_HELP", 3))),
            new Persona("Ananya Verma", 3, "COE", "Third year, processes and memory.",
                    List.of(e("OS", "STRONG", 4), e("DBMS", "STRONG", 3))),
            new Persona("Manav Jindal", 1, "ECE", "First year ECE, getting used to proofs.",
                    List.of(e("MATH", "NEEDS_HELP", 1), e("OOPJ", "NEEDS_HELP", 2))),
            new Persona("Pooja Saini", 2, "COBS", "Second year, synchronisation is hard.",
                    List.of(e("OOPJ", "NEEDS_HELP", 2), e("OS", "NEEDS_HELP", 3))),
            new Persona("Vikram Sandhu", 4, "COE", "Fourth year, TCP and trees.",
                    List.of(e("CN", "STRONG", 4), e("DSA", "STRONG", 4))),
            new Persona("Ira Kapoor", 3, "COE", "Third year, normal forms and probability.",
                    List.of(e("DBMS", "STRONG", 4), e("MATH", "STRONG", 3))),
            new Persona("Siddharth Jain", 2, "COE", "Second year, grinding DSA.",
                    List.of(e("DSA", "NEEDS_HELP", 3), e("CN", "NEEDS_HELP", 2))),
            new Persona("Gurnoor Dhillon", 1, "COE", "First year, building basics.",
                    List.of(e("DSA", "NEEDS_HELP", 1), e("MATH", "NEEDS_HELP", 2))),
            new Persona("Zoya Khan", 3, "COBS", "Third year, Java collections and joins.",
                    List.of(e("OOPJ", "STRONG", 4), e("DBMS", "STRONG", 3))));

    private SeedCatalog() {}

    private static Entry e(String code, String kind, int level) {
        return new Entry(code, kind, level);
    }
}
