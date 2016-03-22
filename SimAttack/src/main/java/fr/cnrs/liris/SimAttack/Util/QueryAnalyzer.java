package fr.cnrs.liris.SimAttack.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Thibault on 07/03/2016.
 */
public class QueryAnalyzer {

    public static final Integer CLUSTER_1TestSet_1TrainingSet = 1;
    public static final Integer CLUSTER_1TestSet_TrainingSet = 2;
    public static final Integer CLUSTER_TestSet_1TrainingSet = 3;
    public static final Integer CLUSTER_TestSet_TrainingSet = 4;
    public static final Integer CLUSTER_TestSet_0TrainingSet = 5;
    public static final Integer CLUSTER_0TestSet_TrainingSet = 6;


    /**
     * Analyzes the query set and classifies the queries into four clusters.
     */
    public static Map<Integer, Integer> clusterizeQueries(List<Query> trainingSet, List<Query> testSet, double treshold){

        List<Query> totalSet =  new ArrayList<>();
        totalSet.addAll(trainingSet);
        totalSet.addAll(testSet);

        Map<Integer, Integer> clusters = new ConcurrentHashMap<>();

        totalSet.parallelStream().forEach(query ->{


            Set<String> keywords = query.getKeywords();
            Set<Integer> testsSets = new HashSet<>();
            for(Query queryTest : testSet){

                if(query.distance(queryTest)>=treshold){

                   testsSets.add(queryTest.getUserId());
                }
            }
            Set<Integer> trainingSets = new HashSet<>();
            for(Query queryTraining : trainingSet){

                if(query.distance(queryTraining)>=treshold){

                    trainingSets.add(queryTraining.getUserId());
                }
            }
            int nbOccurInTrainingSet = trainingSets.size();
            int nbOccurInTestSet = testsSets.size();



            int cluster = 0;

            if(nbOccurInTestSet==0){
                cluster =  CLUSTER_0TestSet_TrainingSet;
            }else if(nbOccurInTrainingSet == 0){
                cluster =  CLUSTER_TestSet_0TrainingSet;
            }else if(nbOccurInTestSet == 1 && nbOccurInTrainingSet == 1){
                cluster = CLUSTER_1TestSet_1TrainingSet;
            }else if(nbOccurInTestSet > 1 && nbOccurInTrainingSet == 1){
                cluster = CLUSTER_TestSet_1TrainingSet;
            }else if(nbOccurInTrainingSet > 1 && nbOccurInTestSet == 1){
                cluster = CLUSTER_1TestSet_TrainingSet;
            }else if(nbOccurInTestSet >1 && nbOccurInTrainingSet > 1){
                cluster = CLUSTER_TestSet_TrainingSet;
            }

            clusters.put(query.getQueryId(), cluster);
        });



        return clusters;
    }


}
