package fr.cnrs.liris.SimAttack;

import com.google.common.collect.TreeMultimap;
import fr.cnrs.liris.SimAttack.Sensitivity.SemanticAssessment;
import fr.cnrs.liris.SimAttack.Util.Profile;
import fr.cnrs.liris.SimAttack.Util.PropertiesManager;
import fr.cnrs.liris.SimAttack.Util.Query;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by apetit on 17/02/16.
 */
public class App {

    public static void main(String[] args) throws Exception {

        String toto = "play,electronics,artisanship,radiology,roman_catholic,cycling,psychiatry,theatre,number,diving,music,folklore,grammar,rowing,social_science,geography,acoustics,drawing,philology,railway,cinema,animals,hunting,publishing,dance,astronautics,anthropology,biology,numismatics,physiology,body_care,economy,philosophy,mythology,quality,graphic_arts,diplomacy,computer_science,volleyball,baseball,plastic_arts,photography,card,archery,sub,linguistics,racing,anatomy,enterprise,telegraphy,tourism,industry,astrology,psychoanalysis,swimming,literature,golf,psychological_features,humanities,paleontology,plants,physics,ethnology,paranormal,architecture,chemistry,hockey,art,book_keeping,skiing,health,badminton,medicine,electricity,metrology,mechanics,veterinary,tennis,food,nautical,religion,table_tennis,cricket,electrotechnology,oceanography,applied_science,geometry,atomic_physic,animal_husbandry,town_planning,time_period,statistics,fashion,surgery,insurance,tv,sexuality,administration,pedagogy,aviation,bowling,telecommunication,radio,basketball,post,furniture,school,military,law,chess,meteorology,rugby,topography,gastronomy,tax,engineering,history,astronomy,person,telephony,entomology,buildings,pharmacy,boxing,exchange,politics,color,dentistry,mathematics,university,sociology,pure_science,vehicles,heraldry,optics,geology,commerce,mountaineering,banking,agriculture,factotum,genetics,hydraulics,psychology,philately,gas,archaeology,soccer,social,fishing,skating,free_time,sculpture,betting,transport,football,theology,occultism,home,environment,jewellery,money,painting,biochemistry,earth,wrestling,athletics,sport,finance,fencing";
        String totobis = "(";
        int bl = 0;
        for (String popo : toto.split(",")){
            totobis += "\"" + popo + "\" " + bl + ",";
            bl++;
        }
        System.out.println(totobis);


        // Load all queries
        List<Query> queries = Files.lines(Paths.get("A1.txt"))
                .parallel() // parallelise le calcul
                .filter(l -> l.length()>1)
                .map(Query::new)
                .collect(Collectors.toList());


        // Filter queries for the training set
        List<Query> trainingSet = queries.parallelStream()
                .filter(q -> q.getDataset() == 0)
                .collect(Collectors.toList());

        // Pour chaque requête, associer à ses mots clés des thêmes

        //PropertiesManager.test();

        SemanticAssessment sem = new SemanticAssessment();

        Set<String> tousDomaines = new HashSet<>();

        List<Profile> profiles = trainingSet.parallelStream()
                .collect(Collectors.groupingBy(Query::getUserId))
                .entrySet()
                .parallelStream()
                .map(Profile::new)
                .collect(Collectors.toList());
/*
        trainingSet.parallelStream()
                .filter(q -> q.getDataset() == 0)
                .forEach(q -> {
                    Set<String> toto = sem.getDomains(q);
                    q.addDomains(toto);
                });*/

        File file = new File("/home/marcus/Documents/SimAttack/graphes/data.txt");

        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (Query query : trainingSet){

            Set<String> domains = sem.getDomains(query);
            query.addDomains(domains);
        }

        for(Profile profile : profiles){
            profile.calculateDomains();
        }

        Map<Integer,Integer> nbDomaines = new TreeMap();


        /** Donne le nombre d'utilisateur en fonction du nombre de domaines utilisés **/
  /*      for (Profile profil : profiles) {
            int size = profil.getDomains().size();
            if(nbDomaines.get(size) != null){
                int i = nbDomaines.get(size);
                nbDomaines.put(size, i + 1);
            } else {
                nbDomaines.put(size, 1);
            }
        }

        for(int size : nbDomaines.keySet()) {
            System.out.println(size + " " + nbDomaines.get(size) + "\n");
            bw.write(size + " " + nbDomaines.get(size) + "\n");
        }*/


        Map<String,Integer> nbUtilisateurs = new TreeMap();

        /** Donne le nombre d'utilisateur utilisant chaque domaine **/
        for (String domaine : PropertiesManager.getProperty("DOMAINS").split(",")){
            nbUtilisateurs.put(domaine,0);
            for (Profile profil : profiles){
                if(profil.getDomains().get(domaine)!=null){
                    int i = nbUtilisateurs.get(domaine);
                    nbUtilisateurs.put(domaine,i+1);
                }
            }
        }

        TreeMultimap<Integer,String> yolo = TreeMultimap.create();
        for (Map.Entry<String, Integer> entree : nbUtilisateurs.entrySet()) {
            yolo.put(entree.getValue(), entree.getKey());
        }
        for (Map.Entry<Integer, String> entree : yolo.entries()) {
            System.out.println(entree.getValue()+" "+entree.getKey());
        }





        //System.out.println(toto);




        //System.out.println(CoreNLPTokenizer.getInstance().process(trainingSet.get(0).getRequest()));


        /** Fait par Thibault pour répartir les users suivant leur présence dans le training set et
         * dans le test set
         */

        /*
        System.out.println("Begin of clustering");
        List<Query> test = queries.parallelStream()
                .filter(q -> q.getDataset() == 1)
                .collect(Collectors.toList());
        Map<Integer, Integer> clusters = QueryAnalyzer.clusterizeQueries(trainingSet, test, 0.8);
        System.out.println("End of clustering");



        final int nbFakeQueries = queries.parallelStream()
                .filter(q -> q.getDataset() == 1)
                .map(Query::getFakeQueryId)
                .max(Integer::compare)
                .get();

        Collection<List<Query>> testSet;
        // Filter queries for the test set and if the test set contains fake queries, it merge the fake queries with the initial query
        if (nbFakeQueries != 0) {
            testSet = queries.parallelStream()
                    .filter(q -> q.getDataset() == 1)
                    .collect(Collectors.groupingBy(Query::getQueryId))
                    .values();
            testSet.forEach(list -> Collections.sort(list,
                    (q1,q2) -> Integer.compare(q1.getFakeQueryId(), q2.getFakeQueryId())));
        } else {
            testSet = queries.parallelStream()
                    .filter(q -> q.getDataset() == 1)
                    .map(Collections::singletonList)
                    .collect(Collectors.toList());
        }

        // Initialize the attack with the training set
        Attack attack = new Attack(trainingSet);

        // Inititialize all multisets
        Multiset<Integer> nbQueriesPerUsers = ConcurrentHashMultiset.create();
        Multiset<Integer>[] nbQueriesCorrectlyClassified = new Multiset[nbFakeQueries+1];
        Multiset<Integer>[] nbQueriesIncorrectlyClassified = new Multiset[nbFakeQueries+1];

        //confidence calculus
        List<Pair<Integer, Pair<Double,Boolean>>> confidences = new ArrayList<>();

        IntStream.range(0, nbFakeQueries+1)
                .forEach(i -> {
                    nbQueriesCorrectlyClassified[i] = ConcurrentHashMultiset.create();
                    nbQueriesIncorrectlyClassified[i] = ConcurrentHashMultiset.create();
                });

        // Run the attack for all queries
        testSet.parallelStream().forEach(query -> {
                    nbQueriesPerUsers.add(query.get(0).getUserId());
                    List<Pair<Pair<Integer, Double>, Boolean>> result = attack.process(query);

                    IntStream.range(0, result.size())
                            .forEach(i -> {
                                int predictedUser = result.get(i).getKey().getKey();

                                if (result.get(i).getValue()) {
                                    confidences.add(new Pair<>(query.get(0).getQueryId(), new Pair<>(result.get(i).getKey().getValue(), true)));
                                    nbQueriesCorrectlyClassified[i].add(predictedUser);
                                } else {
                                    confidences.add(new Pair<>(query.get(0).getQueryId(), new Pair<>(result.get(i).getKey().getValue(), false)));
                                    nbQueriesIncorrectlyClassified[i].add(predictedUser);
                                }
                            });

                }
        );

            System.out.println("Begin of sorting");
            Collections.sort(confidences, new Comparator<Pair<Integer, Pair<Double, Boolean>>>() {
                @Override
                public int compare(Pair<Integer, Pair<Double, Boolean>> o1, Pair<Integer, Pair<Double, Boolean>> o2) {
                    return o2.getValue().getKey().compareTo(o1.getValue().getKey());
                }
            });



        System.out.println("End of sorting");

        File file = new File("C:\\Users\\Thibault\\Documents\\confidence.txt");

        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        //
        int nbOfClasses = (int) (1 + Math.log(confidences.size()));
        int[][] counts = new int[nbOfClasses][7]; //1 + 6 different clusters
        int[] goodRequestsCounts = new int[nbOfClasses];
        List<Pair<Double, Double>> classes = new ArrayList<>();
        double step = 2./nbOfClasses;
        double begin = -1;
        for(int i=0; i<nbOfClasses; i++){
            Pair<Double, Double> curr = new Pair<>(begin, begin+step);
            begin = begin + step;
            classes.add(curr);

        }

        int currIndex = classes.size()-1;

        for(Pair<Integer, Pair<Double, Boolean>> confidence : confidences){

            Pair<Double, Double> currInterval = classes.get(currIndex);

            bw.write(confidence.getKey()+" "+confidence.getValue()+" "+clusters.get(confidence.getKey())+"\n");

            if(confidence.getValue().getKey()>=currInterval.getKey()) {
                counts[currIndex][0]++;
                counts[currIndex][clusters.get(confidence.getKey())]++;
                if(confidence.getValue().getValue()){
                    goodRequestsCounts[currIndex]++;
                }
            }else{
                while(currIndex>0 && confidence.getValue().getKey()<currInterval.getKey()){
                    currIndex--;
                    currInterval = classes.get(currIndex);
                }
                counts[currIndex][0]++;
                counts[currIndex][clusters.get(confidence.getKey())]++;
                if(confidence.getValue().getValue()){
                    goodRequestsCounts[currIndex]++;
                }
            }


        }

        bw.close();



        for(int i=0; i<nbOfClasses; i++){
            System.out.print(classes.get(i).getKey()+" "+classes.get(i).getValue()+" "+counts[i][0]+" "+(double)(goodRequestsCounts[i])/counts[i][0]+" ");
            for(int j=1; j<7; j++){ //clusters
                System.out.print((double)(counts[i][j])/counts[i][0]+ " ");
            }
            System.out.println();

        }

        //Here we prepare the computing of the graphs
        List<Pair<Integer, Double>> users = new ArrayList<>();


        // Compute the metrics: recall and precision
        double[] recall = new double[nbFakeQueries+1], precision = new double[nbFakeQueries+1];
        for (int i = 0; i < nbFakeQueries+1; i++) {
            int count = 0;
            for (Multiset.Entry<Integer> entry : nbQueriesPerUsers.entrySet()) {
                double correctlyClassified = nbQueriesCorrectlyClassified[i].count(entry.getElement());
                double incorrectlyClassified = nbQueriesIncorrectlyClassified[i].count(entry.getElement());
                count++;
                System.out.println("For user "+entry.getElement()+" nb "+count+" correct guesses "+correctlyClassified/entry.getCount());
                users.add(new Pair<>(entry.getElement(), correctlyClassified/entry.getCount()));
                recall[i] += correctlyClassified / entry.getCount();
                if (correctlyClassified + incorrectlyClassified == 0) {
                    precision[i] += 1;
                } else {
                    precision[i] += correctlyClassified / (correctlyClassified + incorrectlyClassified);
                }
            }
            recall[i] /= nbQueriesPerUsers.elementSet().size();
            precision[i] /= nbQueriesPerUsers.elementSet().size();
        }

        //we sort the list for outputing a graph
        Collections.sort(users, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });


        for(Pair<Integer, Double> userData : users){

            System.out.println(userData.getKey()+" "+userData.getValue());
        }

        // Use to format the recall and the precision
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(1);

        // Display values
        System.out.println("NbQueries: "+testSet.size());
        StringBuilder sb = new StringBuilder();
        sb.append("NbFQ\tRecall\tPrecision\n");
        for (int i = 0; i < nbFakeQueries+1; i++) {
            sb.append(i).append('\t')
                    .append(nf.format(recall[i])).append('\t')
                    .append(nf.format(precision[i])).append('\n');
        }
        System.out.println(sb.toString());

        */


    }



}
