package fr.cnrs.liris.SimAttack.Util;

import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 * Created by apetit on 08/10/15.
 */
public class Profile {

    private final int userId;
    private final List<Pair<Set<String>,Double>> queries = new ArrayList<>();
    private final Set<String> words = new HashSet<>();
    private double alpha = Double.parseDouble("0.6");

    private Set<Query> realQueries;
    private Map<String,Double> domains;

    /**
     * Create a new Profile
     * @param entry A map entry containing the userId and a list of queries
     */

    public Profile(Map.Entry<Integer, List<Query>> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Create a new Profile
     * @param userId the user id
     * @param profile a list of queries
     */
    public Profile(int userId, List<Query> profile) {
        this.userId = userId;
        profile.stream().forEachOrdered(this::add);

        domains = new TreeMap<>();

        realQueries = new HashSet<>();
        realQueries.addAll(profile);
    }

    public int getUserId() {
        return userId;
    }

    private void add(Query query) {
        words.addAll(query.getKeywords());
        queries.add(new Pair<>(query.getKeywords(), Math.sqrt(query.getKeywords().size())));
    }

    /**
     * This method returns the distance between the Profile and a query
     * @param query
     * @return distance between the Profile and a query
     */

    public double distance(Query query) {

        for (String word : query.getKeywords()) {
            if(words.contains(word)){
                List<Double> distrib = computeDotProduct(query.getKeywords());
                Collections.sort(distrib);

                double mean = distrib.get(0);
                for (int i = 1; i < distrib.size(); i++) {
                    mean = distrib.get(i) * alpha + mean * (1-alpha);
                }
                return mean;
            }
        }
        return 0;
    }

    private List<Double> computeDotProduct(Set<String> vector2) {
        List<Double> distrib = new ArrayList<>(queries.size());
        double norm2 = Math.sqrt(vector2.size());
        for (Pair<Set<String>,Double> vector1 : queries) {
            Set<String> v1;
            Set<String> v2;
            if (vector1.getFirst().size() > vector2.size()) {
                v1 = vector1.getFirst();
                v2 = vector2;
            } else {
                v1 = vector2;
                v2 = vector1.getFirst();
            }
            double value = 0;
            for (String string : v2) {
                if (v1.contains(string)) {
                    value++;
                }
            }
            double normalizedValue =  value / (norm2 * vector1.getSecond());
            if (Double.isNaN(normalizedValue)) {
                distrib.add(0.);
            } else {
                distrib.add(normalizedValue);
            }
        }
        return distrib;
    }

    public void calculateDomains (){
        int size = this.realQueries.size();
        for (Query query : this.realQueries) {
            Map<String,Integer> dom = query.getDomains();
            for (String domain : dom.keySet()) {
                Double j;
                if(this.domains.get(domain) != null){
                    j = this.domains.get(domain);
                } else {
                    j = 0.0;
                }
                Double i = (double)dom.get(domain);
                this.domains.put(domain,j+(i/size));
            }
        }
    }

    public Map<String,Double> getDomains (){
        return this.domains;
    }

}
