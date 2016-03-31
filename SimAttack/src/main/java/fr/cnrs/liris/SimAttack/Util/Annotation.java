package fr.cnrs.liris.SimAttack.Util;

import net.sf.extjwnl.data.POS;


/**
 * Created by apetit on 07/10/15.
 */
public class Annotation {

    private final String text;
    private final POS pos;

    public Annotation(String text) {
        this.text = text;
        this.pos = null;
    }

    public Annotation(String text, String pos) {
        this.text = text;
        switch (pos) {
            case "JJ":
            case "JJR":
            case "JJS":
                this.pos = POS.ADJECTIVE;
                break;
            case "NN":
            case "NNS":
            case "NNP":
            case "NNPS":
                this.pos = POS.NOUN;
                break;
            case "RB":
            case "RBR":
            case "RBS":
                this.pos = POS.ADVERB;
                break;
            case "VB":
            case "VBD":
            case "VBG":
            case "VBN":
            case "VBP":
            case "VBZ":
                this.pos = POS.VERB;
                break;
            default:
                this.pos = null;
                break;
        }
    }

    public String getText() {
        return text;
    }

    public POS getPOS() {
        return pos;
    }

}
