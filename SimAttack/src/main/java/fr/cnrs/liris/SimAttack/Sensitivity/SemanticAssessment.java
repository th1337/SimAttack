package fr.cnrs.liris.SimAttack.Sensitivity;

import com.google.common.collect.Sets;
import fr.cnrs.liris.SimAttack.Util.*;
import fr.cnrs.liris.SimAttack.Util.Annotation;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.dictionary.MorphologicalProcessor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by apetit on 02/10/15.
 */
public class SemanticAssessment {

    private static final HashSet<String> SENSITIVE_DOMAIN = Sets.newHashSet(
            "psychology, psychoanalysis, religion, theology, roman_catholic, medicine, dentistry, pharmacy, psychiatry, radiology, surgery, anatomy, politics, sexuality, health".split(", "));

    private final MorphologicalProcessor morphologicalProcessor;
    private final Map<String, List<String>> synsetToDomains = new HashMap<>();

    public final Map<String, Set<String>> queryToDomains = new HashMap<>();
    public final Map<String, String> queryToDescription = new HashMap<>();

    private final int NB_CATEGORIES_PER_SYNSET = Integer.parseInt(PropertiesManager.getProperty("NB_CATEGORIES_PER_SYNSET"));
    private final int NB_SYNSETS_PER_KEYWORD = Integer.parseInt(PropertiesManager.getProperty("NB_SYNSETS_PER_KEYWORD"));
    private final String[] WORDNET = PropertiesManager.getProperty("WORDNET").split(",");
    private final Set<String> medicalWords;
    private final List<String> badWords;


    public SemanticAssessment() throws Exception {
        try {
            Dictionary dictionary = Dictionary.getFileBackedInstance(WORDNET[0]);
            this.morphologicalProcessor = dictionary.getMorphologicalProcessor();
            System.out.println("Read Wordnet from: "+WORDNET[0]);
        } catch (JWNLException e) {
            throw new Exception("Cannot read the Wordnet resource from: "+WORDNET[0]);
        }

        System.out.println("Read the WordNet Domains file: " + WORDNET[1]);
        if (WORDNET[1].contains("xwnd-30g") && !(new File(WORDNET[1]).exists())) {
            //TODO call to export
        }
        for (String line : Util.read(WORDNET[1])) {
            String[] tokens = line.split("[\t\\ ]");
            String synset = tokens[0].intern();
            List<String> domains = new ArrayList<>();
            //for (int i = 1; i < tokens.length && (!WORDNET[1].contains("xwnd-30g") || i < NB_CATEGORIES_PER_SYNSET+1) ; i++) {
            for (int i = 1; i < tokens.length; i++) {
                domains.add(tokens[i].intern());
            }
            synsetToDomains.put(synset, domains);
        }
        this.medicalWords = new HashSet<>();
        medicalWords.addAll(Util.read(PropertiesManager.getProperty("MEDWORDS")));
        badWords = Util.read(PropertiesManager.getProperty("BADWORDS"));

    }


    public boolean process(Query query) {
        return process(query, NB_SYNSETS_PER_KEYWORD, NB_CATEGORIES_PER_SYNSET);
    }

    public boolean process(Query query, int nbSynsetsPerKeyword) {
        if (nbSynsetsPerKeyword == -1) {
            return process(query);
        } else {
            return process(query, nbSynsetsPerKeyword, NB_CATEGORIES_PER_SYNSET);
        }
    }

    /** Calcule la sensibilité d'une requête **/

    public boolean process(Query query, int nbSynsetsPerKeyword, int nbDomainsPerSynset) {

        if (nbSynsetsPerKeyword == -1) {
            nbSynsetsPerKeyword = NB_SYNSETS_PER_KEYWORD;
        }
        if (nbDomainsPerSynset == -1) {
            nbDomainsPerSynset = NB_CATEGORIES_PER_SYNSET;
        }

        List<List<Synset>> synsets = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        Set<String> domains = new HashSet<>();
        try {
            /** Transforme la requête en annotations **/
            for (Annotation annotation : CoreNLPTokenizer.getInstance().process(query.getRequest())) {
                String token = annotation.getText();
                POS pos = annotation.getPOS();
                IndexWord indexWord = morphologicalProcessor.lookupBaseForm(pos, token);
                if (indexWord != null) {
                    indexWord.sortSenses();
                    synsets.add(indexWord.getSenses());
                    keywords.add(token);
                } else {
                    if (medicalWords.contains(token) && pos != null) {
                        domains.add("medicine");
                    }
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        //if (synsets.isEmpty()) {
        boolean containsBadword = false;
        for (String badword: badWords) {
            if (query.getRequest().contains(badword)) {
                domains.add("sexuality");
                containsBadword = true;
                break;
            }
        }
        //}

        if (!containsBadword) {
            if (nbSynsetsPerKeyword != -1) {
                synsets = WuAndPalmer.disambiguate(synsets, nbSynsetsPerKeyword);
            }

            StringBuilder sb = new StringBuilder();
            // iterate over all terms in the input string
            for (int i = 0; i < synsets.size(); i++) {
                List<Synset> synsetList = synsets.get(i);
                // iterate over all synsets
                sb.append("> ").append(keywords.get(i)).append(": ");
                for (Synset synset : synsetList) {
                    List<String> synsetToDomains = collectDomains(synset);
                    if (synsetToDomains != null) {
                        synsetToDomains = synsetToDomains.subList(0, nbDomainsPerSynset);
                        domains.addAll(synsetToDomains);

                        Iterator<String> it = synsetToDomains.iterator();
                        while (it.hasNext()) {
                            String domain = it.next();
                            sb.append(domain);
                            if (it.hasNext()) {
                                sb.append(", ");
                            } else {
                                sb.append("   |   ");

                            }
                        }

                    }
                }
                sb.append('\n');
            }
            System.out.println(query.getQueryId() + "..." + sb.toString() + "\n");
            queryToDomains.put(query.getRequest(), domains);
            queryToDescription.put(query.getRequest(), sb.toString());
        }

        return Sets.intersection(SENSITIVE_DOMAIN, domains).size() > 0;

    }

    public Set<String> getDomains(Query query) {



        int nbSynsetsPerKeyword = NB_SYNSETS_PER_KEYWORD;
        int nbDomainsPerSynset = NB_CATEGORIES_PER_SYNSET;

        List<List<Synset>> synsets = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        Set<String> domains = new HashSet<>();
        try {
            /** Transforme la requête en annotations **/
            for (Annotation annotation : CoreNLPTokenizer.getInstance().process(query.getRequest())) {
                String token = annotation.getText();
                POS pos = annotation.getPOS();
                IndexWord indexWord = morphologicalProcessor.lookupBaseForm(pos, token);
                if (indexWord != null) {
                    indexWord.sortSenses();
                    synsets.add(indexWord.getSenses());
                    keywords.add(token);
                } else {
                    if (medicalWords.contains(token) && pos != null) {
                        domains.add("medicine");
                    }
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        //if (synsets.isEmpty()) {
        boolean containsBadword = false;
        for (String badword: badWords) {
            if (query.getRequest().contains(badword)) {
                domains.add("sexuality");
                containsBadword = true;
                break;
            }
        }
        //}

        if (!containsBadword) {
            if (nbSynsetsPerKeyword != -1) {
                synsets = WuAndPalmer.disambiguate(synsets, nbSynsetsPerKeyword);
            }

            // iterate over all terms in the input string
            for (int i = 0; i < synsets.size(); i++) {
                List<Synset> synsetList = synsets.get(i);
                // iterate over all synsets
                for (Synset synset : synsetList) {
                    List<String> synsetToDomains = collectDomains(synset);
                    if (synsetToDomains != null) {
                        synsetToDomains = synsetToDomains.subList(0, nbDomainsPerSynset);
                        domains.addAll(synsetToDomains);
                    }
                }
            }
        }

        return domains;
    }

    private List<String> collectDomains(Synset synset) {
        String synsetId = String.format("%08d-%s", synset.getKey(), synset.getPOS().getKey());
        return synsetToDomains.get(synsetId);
    }

/*
    public static void main(String[] args) throws Exception {

        Set<String> porn = Files.lines(Paths.get("/Users/apetit/Downloads/badwords/badwords.txt"))
                .collect(Collectors.toSet());
        Files.lines(Paths.get("/Users/apetit/Downloads/badwords/badwords1.txt"))
                .forEach(l -> porn.add(l));
        Files.lines(Paths.get("/Users/apetit/Downloads/badwords/badwords2.txt"))
                .forEach(l -> porn.add(l));
        List<String> list = porn.stream().sorted().collect(Collectors.toList());
        Util.save(list, "/Users/apetit/Downloads/badwords/merge.txt");
    }
    */

/*
    public static void main(String[] args) throws Exception {

        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(1);

        String[] categories = {"PORN"};
        //String[] categories = {"POLITIC", "PORN", "HEALTH", "RELIGION"};
        SemanticAssessment sa = new SemanticAssessment();
        //List<Query> listOfSensitiveQueries = Files.lines(Paths.get(PropertiesManager.getProperty("SENSITIVE_QUERIES")))
        //        .map(Query::new)
        //        .collect(Collectors.toList());

        for (String category : categories) {
            List<Query> listOfSensitiveQueries = Files.lines(Paths.get("/Users/apetit/Dropbox/Thesis/Experiments/crowdflower/"+category+".txt"))
                    .map(Query::new)
                    .collect(Collectors.toList());

            List<Query> listFalseNegative = new ArrayList<>();
            int i = 0;
            for (Query sensitiveQuery : listOfSensitiveQueries) {
                if (sa.process(sensitiveQuery, 2)) {
                    // nothing
                    i++;
                } else {
                    listFalseNegative.add(sensitiveQuery);
                }
            }
            for (Query query : listFalseNegative) {
                if (query.getRequest().contains("ass")) {
                    // nothing
                }
                System.out.print(query.getRequest() + " -> " + sa.queryToDomains.get(query.getRequest()) + '\n'
                    + sa.queryToDescription.get(query.getRequest()));
            }
            System.out.println(i);


            System.out.println(category+": " +i+"/"+listOfSensitiveQueries.size()+"("+nf.format(i/(double)listOfSensitiveQueries.size()));

        }



        //for (Query query : listFalseNegative) {
            //if (sa.queryToDomains.get(query.getRequest()).isEmpty()) {
                //System.out.print(query.getRequest() + "\n -> " + sa.queryToDomains.get(query.getRequest()) + '\n');
            //}
        //}
        //System.out.println(listFalseNegative.stream().filter(l -> sa.queryToDomains.get(l.getRequest()).isEmpty()).count());


        /*
        List<Query> listOfNonSensitiveQueries = Files.lines(Paths.get(PropertiesManager.getProperty("NON_SENSITIVE_QUERIES")))
                .filter(l -> l.length() > 1)
                .map(Query::new)
                .collect(Collectors.toList());

        Set<String> medicalWords = Files.lines(Paths.get("/Users/apetit/Dropbox/Thesis/Experiments/crowdflower/medicalWordlist.txt"))
                .collect(Collectors.toSet());

        Set<Query> queries = new HashSet<>();
        for (Query query : listOfNonSensitiveQueries) {
                for (Annotation annotation : query.getKeywords()) {
                    if (medicalWords.contains(annotation.getText())) {
                        queries.add(query);
                    }
                }
        }
        System.out.println(queries.size());

    */
    //}



    public static void main(String[] args) throws Exception {

        int numThreads = Integer.parseInt(PropertiesManager.getProperty("NUMBER_OF_THREADS"));
        //int[] s = {1,2,3,4};
        //int[] c = {1,2,3,4};
        int[] s = {2};
        int[] c = {3};
        numThreads = (numThreads<2) ? 2 : numThreads;

        List<Query> listOfSensitiveQueries = Files.lines(Paths.get(PropertiesManager.getProperty("SENSITIVE_QUERIES")))
                .filter(l -> l.length() > 1)
                .map(Query::new)
                .collect(Collectors.toList());
        List<Query> listOfNonSensitiveQueries = Files.lines(Paths.get(PropertiesManager.getProperty("NON_SENSITIVE_QUERIES")))
                .map(Query::new)
                .collect(Collectors.toList());

        double[][] truePositiveRate = new double[s.length][c.length], falsePositiveRate = new double[s.length][c.length];

        for (int i = 0; i < c.length; i++) {
            final BlockingQueue<Query> sensitiveQueries = new LinkedBlockingDeque<>(listOfSensitiveQueries);
            final BlockingQueue<Query> nonSensitiveQueries = new LinkedBlockingDeque<>(listOfNonSensitiveQueries);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CompletionService<int[][]> completionService = new ExecutorCompletionService<>(executor);
            int size = sensitiveQueries.size() + nonSensitiveQueries.size();

            // Start each thread
            for (int j = 0; j < numThreads; j++) {
                if (j == 0) {
                    completionService.submit(new ThreadSensitiveQueries(sensitiveQueries, s, c[i]));
                } else {
                    completionService.submit(new ThreadNonSensitiveQueries(nonSensitiveQueries, s, c[i]));
                }
            }
            executor.shutdownNow();

            try {
                // Display progression
                while (!executor.awaitTermination(4, TimeUnit.SECONDS)) {
                    System.out.print((sensitiveQueries.size() + nonSensitiveQueries.size()) + "/" + size + "\r");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int[] truePositive = new int[s.length], falsePositive = new int[s.length], falseNegative = new int[s.length], trueNegative = new int[s.length];
            for (int j = 0; j < numThreads; j++) {
                try {
                    int[][] value = completionService.take().get();
                    for (int k = 0; k < s.length; k++) {
                        truePositive[k] += value[k][0];
                        falsePositive[k] += value[k][1];
                        falseNegative[k] += value[k][2];
                        trueNegative[k] += value[k][3];
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }


            NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMinimumFractionDigits(1);

            for (int synset = 0; synset < s.length; synset++) {
                truePositiveRate[synset][i] = truePositive[synset] / ((double) (truePositive[synset] + falseNegative[synset]));
                falsePositiveRate[synset][i] = 1 - (trueNegative[synset] / ((double) trueNegative[synset] + falsePositive[synset]));
                System.out.println("Synset: " + s[synset] + " - Category: " + c[i]);
                System.out.println("Nb queries protected:" + (truePositive[synset] + falsePositive[synset]));
                System.out.println("Nb queries unprotected:" + (trueNegative[synset] + falseNegative[synset]));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int aC : c) {
            sb.append("\t").append(aC);
        }
        sb.append("\n");
        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);
            for (int j = 0; j < c.length; j++ ) {
                sb.append("\t ").append(truePositiveRate[i][j]);
            }
            sb.append("\n");
        }


        sb.append("\n");
        sb.append("\n");


        for (int aC : c) {
            sb.append("\t").append(aC);
        }
        sb.append("\n");
        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);
            for (int j = 0; j < c.length; j++ ) {
                sb.append("\t ").append(falsePositiveRate[i][j]);
            }
            sb.append("\n");
        }
        System.out.println(sb);

    }


    public static class ThreadSensitiveQueries implements Callable {

        private int[] truePositive, falsePositive, falseNegative, trueNegative;
        private final BlockingQueue<Query> queue;
        private final SemanticAssessment sa;
        private final int[] s;
        private final int c;

        public ThreadSensitiveQueries(BlockingQueue<Query> queue, int[] s, int c) throws Exception {
            this.queue = queue;
            this.sa = new SemanticAssessment();
            this.s = s;
            this.c = c;
            this.truePositive = new int[s.length];
            this.falsePositive = new int[s.length];
            this.falseNegative  = new int[s.length];
            this.trueNegative  = new int[s.length];
        }

        public ThreadSensitiveQueries(BlockingQueue<Query> queue) throws Exception {
            this(queue, new int[]{-1}, -1);
        }

        @Override
        public int[][] call() throws Exception {
            while (!queue.isEmpty()) {
                Query query = queue.poll();
                for (int i = 0; i < s.length; i++) {
                    if (sa.process(query, s[i], c)) {
                        truePositive[i]++;
                    } else {
                        falseNegative[i]++;
                    }
                }
            }
            int[][] results = new int[s.length][4];
            for (int i = 0; i < s.length; i++) {
                results[i][0] = truePositive[i];
                results[i][1] = falsePositive[i];
                results[i][2] = falseNegative[i];
                results[i][3] = trueNegative[i];

            }
            return results;
        }
    }

    public static class ThreadNonSensitiveQueries implements Callable {

        private int[] truePositive, falsePositive, falseNegative, trueNegative;
        private final BlockingQueue<Query> queue;
        private final SemanticAssessment sa;
        private final int[] s;
        private final int c;

        public ThreadNonSensitiveQueries(BlockingQueue<Query> queue, int[] s, int c) throws Exception {
            this.queue = queue;
            this.sa = new SemanticAssessment();
            this.s = s;
            this.c = c;
            this.truePositive = new int[s.length];
            this.falsePositive = new int[s.length];
            this.falseNegative  = new int[s.length];
            this.trueNegative  = new int[s.length];
        }

        public ThreadNonSensitiveQueries(BlockingQueue<Query> queue) throws Exception {
            this(queue, new int[]{-1}, -1);
        }

        @Override
        public int[][] call() throws Exception {
            while (!queue.isEmpty()) {
                Query query = queue.poll();
                for (int i = 0; i < s.length; i++) {
                    if (!sa.process(query, s[i], c)) {
                        trueNegative[i]++;
                    } else {
                        falsePositive[i]++;
                    }
                }
            }
            int[][] results = new int[s.length][4];
            for (int i = 0; i < s.length; i++) {
                results[i][0] = truePositive[i];
                results[i][1] = falsePositive[i];
                results[i][2] = falseNegative[i];
                results[i][3] = trueNegative[i];

            }
            return results;
        }
    }

}
