package fr.cnrs.liris.SimAttack;

import fr.cnrs.liris.SimAttack.Util.Profile;
import fr.cnrs.liris.SimAttack.Util.Query;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by apetit on 23/10/15.
 */
public class Attack {

    private List<Profile> profiles;

    /**
     * Create a new attack
     * @param trainingSet A set of previous queries known by the attacker.
     */
    public Attack(List<Query> trainingSet) {
        // Split queries per user and create a user profile for each user.
        this.profiles = trainingSet.parallelStream()
                .collect(Collectors.groupingBy(Query::getUserId))
                .entrySet()
                .parallelStream()
                .map(Profile::new)
                .collect(Collectors.toList());
    }

    /***
     * Return the most probable user who issue the query.
     * @param query The protected query sent by the user.
     * @return The user id of the most probable user.
     */
    public List<Pair<Pair<Integer,Double>, Boolean>> process(List<Query> query) {


        int userId = query.get(0).getUserId();
        if (query.size() == 1) {
            double confidence = getConfidenceProbableUser(query.get(0));
            int tmp = getMostProbableUser(query.get(0));
            return Collections.singletonList(new Pair<>(new Pair<>(tmp, confidence), tmp == userId));
        } else {
            /**List<Rank> ranks = query.stream()
                    .map(this::getMostProbableRank)
                    .collect(Collectors.toList());

            return IntStream.range(0,query.size())
                    .mapToObj(i -> {
                        Rank rank = ranks.stream()
                                .limit(i+1)
                                .max(Rank::compareTo)
                                .get();
                        return new Pair<>(rank.getUserId(),
                                rank.getQuery() == query.get(0) && rank.getUserId() == userId);
                    })
                    .collect(Collectors.toList());*/
            return null;
        }

        //Now we compute the confidence
        //we sort the list for outputing a graph


    }


    /***
     * This method returns the User Id with the higher similarity
     * @param query
     * @return
     */
    private int getMostProbableUser(Query query) {
        return profiles.stream()
                .map(p -> new Pair<>(p, p.distance(query)))
                .max( (p1, p2) -> p1.getValue().compareTo(p2.getValue()))
                .get()
                .getKey()
                .getUserId();
    }

    /**
     *
     * @param query
     * @return
     */
    private double getConfidenceProbableUser(Query query) {
        List<Pair<Profile, Double>> res =profiles.stream()
                .map(p -> new Pair<>(p, p.distance(query)))
                .sorted((p1, p2) -> p2.getValue().compareTo(p1.getValue()))
                .limit(2)
                .collect(Collectors.<Pair<Profile, Double>>toList());

        if(res.get(0).getValue() > 0 && res.size()>1){//more than one user, and value not equal to zero
            return (res.get(0).getValue() - res.get(1).getValue()) / (res.get(0).getValue());
        }else if(res.get(0).getValue()==0) { //value equals 0
            return -1;
        }else{
            return 1;
        }
    }

    /***
     * This method returns the Rank with the higher similarity
     * @param query
     * @return
     */
    private Rank getMostProbableRank(Query query) {
        // Return the User Id of the profile with the higher similarity
        return profiles.stream()
                .map(p -> new Pair<>(p, p.distance(query)))
                .max( (p1, p2) -> p1.getValue().compareTo(p2.getValue()))
                .map( p -> new Rank(p.getValue(), p.getKey().getUserId(), query))
                .get();
    }


    private class Rank implements Comparable<Rank> {

        private final double probability;
        private final int userId;
        private final Query query;

        public Rank(double probability, int userId, Query query) {
            this.probability = probability;
            this.query = query;
            this.userId = userId;
        }

        @Override
        public int compareTo(Rank o) {
            return Double.compare(probability, o.probability);
        }

        public Query getQuery() {
            return query;
        }

        public int getUserId() {
            return userId;
        }

    }
}
