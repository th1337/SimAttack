package fr.cnrs.liris.SimAttack.Util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by apetit on 22/10/14.
 */
public class CoreNLPTokenizer {

    private static volatile CoreNLPTokenizer instance = null;
    private static volatile StanfordCoreNLP pipeline = null;
    private static Set<String> stopwords;

    /*
    private CoreNLPTokenizer()  {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos");
        pipeline = new StanfordCoreNLP(props, false);
        stopwords = new HashSet<>();
        stopwords.addAll(Util.read(PropertiesManager.getProperty("STOPWORDS")));
    }*/

    private CoreNLPTokenizer() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos");
        pipeline = new StanfordCoreNLP(props, false);
        try {
            stopwords = Files.lines(Paths.get("stopwords.txt"), Charset.forName("UTF-8"))
                    .map(String::intern)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            System.err.println("Stopwords couldn't be loaded");
            stopwords = new HashSet<>();
        }
    }

    public static CoreNLPTokenizer getInstance() {
        CoreNLPTokenizer result = instance;
        if (result == null) {
            synchronized (CoreNLPTokenizer.class) {
                result = instance;
                if (result == null) {
                    result = instance = new CoreNLPTokenizer();
                }
            }
        }
        return result;
    }

    public List<Annotation> process(String request) {
        return process(request, null);
    }


    public Set<String> oldProcess(String request) {
        Set<String> keywords = new HashSet<>();
        edu.stanford.nlp.pipeline.Annotation document = pipeline.process(request);
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class).intern();
                if (stopwords.contains(word)) {
                    continue;
                }
                keywords.add(word);
            }
        }
        return keywords;
    }



    /** Crée une liste d'annotations pour la requête **/

    public List<Annotation> process(String request, Set<String> filter) {
        List<Annotation> keywords = new ArrayList<>();
        edu.stanford.nlp.pipeline.Annotation document = pipeline.process(request);
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class).intern();
                if (stopwords.contains(word)) {
                    continue;
                }
                /*
                if(filter != null) {
                    String NER = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    if (filter.contains(NER)) {
                        continue;
                    }
                }
                */

                String POS = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                keywords.add(new Annotation(word, POS));
            }
        }
        return keywords;
    }


}
