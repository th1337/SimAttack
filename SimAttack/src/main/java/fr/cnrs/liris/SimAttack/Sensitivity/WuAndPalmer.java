package fr.cnrs.liris.SimAttack.Sensitivity;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.Pointer;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.Synset;

import java.util.*;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by apetit on 28/09/15.
 */
public class WuAndPalmer {

    public static List<List<Synset>> disambiguate(List<List<Synset>> synsets, int nbSynsets) {
        List<List<Synset>> disambiguatedSynsets = new ArrayList<>();

        // iterate over all keywords
        for (int i = 0; i < synsets.size(); i++) {
            if (synsets.get(i).size() > nbSynsets) {
                List<SynsetAssignement> synsetAssignements = new ArrayList<>(synsets.get(i).size());

                // iterate over all synsets of a keywords
                for (Synset synset : synsets.get(i)) {
                    SynsetAssignement sa = new SynsetAssignement(synset);
                    // iterate all other keywords
                    for (int j = 0; j < synsets.size(); j++) {
                        if (i == j) {
                            continue;
                        }
                        // iterate over all synsets of these other keywords
                        for (Synset synset1 : synsets.get(j)) {
                            sa.setWeight(Math.max(sa.getWeight(), getSimilarity(synset, synset1)));
                        }
                    }
                    synsetAssignements.add(sa);
                }

                Collections.sort(synsetAssignements);
                List<Synset> mostProbableSynsets = new ArrayList<>(nbSynsets);
                for (int k = 0; k < nbSynsets; k++) {
                    mostProbableSynsets.add(synsetAssignements.get(k).getSynset());
                }
                disambiguatedSynsets.add(mostProbableSynsets);
            } else {
                disambiguatedSynsets.add(synsets.get(i));
            }
        }
        return disambiguatedSynsets;
    }

    public static double getSimilarity(Synset c1, Synset c2) {
        if (c1 == null) {
            return 0;
        }
        if (c1.equals(c2)) {
            return 1;
        }

        Node root = new Node(c1);
        MinHashMap<Synset, Integer> mhm = new MinHashMap<>();
        mhm.put(c1, 0);
        compute(root, 1, new HashMap<>(), mhm);

        //System.out.println(root.toString(mhm));

        Set<Synset> hypernym1 = mhm.keySet();
        Set<Synset> synsets = new HashSet<>();
        synsets.add(c2);

        int N2 = 0;
        Set<Synset> alreadyUsed = new HashSet<>();
        while (!synsets.isEmpty() && N2 < 40) {
            alreadyUsed.addAll(synsets);
            Set<Synset> commonSuperconcepts = Sets.intersection(hypernym1, synsets);
            if (!commonSuperconcepts.isEmpty()) {
                Synset commonSuperconcept = commonSuperconcepts.iterator().next();
                int N1 = mhm.get(commonSuperconcept);
                int H = mhm.getDistanceToRoot(commonSuperconcept);
                /*
                System.out.println("Lowest Common Subsumer: "+commonSuperconcept.getWords().get(0).getLemma());
                System.out.println("N1: "+N1);
                System.out.println("N2: "+N2);
                System.out.println("H: "+H);
                */
                return (2 * H) / ((double) N1 + N2 + 2 * H);
            }
            Set<Synset> tmp = new HashSet<>();
            for (Synset synset : synsets) {
                tmp.addAll(getHypernyms(synset));
                tmp.removeAll(alreadyUsed);
            }
            /*
            for (Synset synset : tmp) {
                System.out.println(synset);
            }
            */
            N2++;
            synsets = tmp;
        }
        return 0;

    }

    private static void compute(Node node, int depth, HashMap<Synset, Node> nodes, MinHashMap<Synset, Integer> mhm) {
        Set<Synset> hypernyms = getHypernyms(node.data);
        for (Synset hypernym : hypernyms) {
            Node child = nodes.get(hypernym);
            if (child == null) {
                child = new Node(hypernym);
                nodes.put(hypernym, child);
            }
            node.children.put(hypernym, child);
            mhm.put(hypernym, depth);
            if (depth < 40) {
                compute(child, depth+1, nodes, mhm);
            }
        }
        if (hypernyms.isEmpty()) {
            mhm.put(null, depth);
        }
    }

    private static Set<Synset> getHypernyms(Synset synset) {
        Set<Synset> synsets = new HashSet<>();
        List<Pointer> pointers = synset.getPointers();
        for (Pointer pointer : pointers) {
            if (pointer.getType() !=  PointerType.HYPERNYM) {
                continue;
            }
            try {
                synsets.add(pointer.getTargetSynset());
            } catch (JWNLException e) {
                e.printStackTrace();
            }
        }
        return synsets;
    }

    private static class SynsetAssignement implements Comparable<SynsetAssignement> {

        private final Synset synset;
        private double weight;

        public SynsetAssignement(Synset synset) {
            this.synset = synset;
            this.weight = 0;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public Synset getSynset() {
            return synset;
        }

        @Override
        public int compareTo(SynsetAssignement o) {
            return Double.compare(o.weight, weight);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SynsetAssignement pair = (SynsetAssignement) o;
            return Objects.equals(weight, pair.weight) &&
                    Objects.equals(synset, pair.synset);
        }

        @Override
        public int hashCode() {
            return Objects.hash(synset, weight);
        }

        @Override
        public String toString() {
            return "[synset=" + synset + ", weight=" + weight + ']';
        }
    }


    public static class Node {
        private final Synset data;
        private final Map<Synset, Node> children;

        public Node(Synset data) {
            this.children = new HashMap<>();
            this.data = data;
        }

        public String toString(MinHashMap<Synset, Integer> mhm) {
            StringBuilder s = new StringBuilder();
            s.append(data).append("\n");
            for (Node node : children.values()) {
                node.toString(s, 1, mhm);
            }
            return s.toString();
        }

        private void toString(StringBuilder s, int offset, MinHashMap<Synset, Integer> mhm) {
            toString(s,offset, -1, mhm);
        }

        private void toString(StringBuilder s, int offset) {
            toString(s,offset, -1);
        }

        private void toString(StringBuilder s, int offset, int depth) {
            s.append(Strings.repeat("\t", offset)).append(data).append('\n');
            if (depth == -1 || offset <= depth) {
                for (Node node : children.values()) {
                    node.toString(s, offset + 1, depth);
                }
            }
        }

        private void toString(StringBuilder s, int offset, int depth, MinHashMap<Synset, Integer> mhm) {
            s.append(Strings.repeat("\t", offset)).append(data).append(' ').append('(')
                    .append(mhm.get(data)).append(')').append('\n');
            if (depth == -1 || offset <= depth) {
                for (Node node : children.values()) {
                    node.toString(s, offset + 1, depth, mhm);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(data).append("\n");
            for (Node node : children.values()) {
                node.toString(s, 1);
            }
            return s.toString();
        }

        @Override
        public boolean equals(Object obj) {
            boolean sameSame = false;
            if (obj != null && obj instanceof Node) {
                sameSame = this.data == ((Node) obj).data;
            }
            return sameSame;
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

    }
}

