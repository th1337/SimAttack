/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.cnrs.liris.SimAttack.Util;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author apetit
 */
public class Query implements Comparable<Query> {

	private Set<String> keywords;
	private final int dataset;
	private final int queryId;
	private final int userId;
	private final String request;
    private final int fakeQueryId;


    /**
     * Create a new query from a specific pattern:
     * - "QueryId\tUserId\tRequest\tDataset" (if no fake query)
     * - "QueryId\tUserId\tRequest\tDataset\tFakeQueryId" (if fake query)
     * @param line
     */
    public Query(String line) {
        // Parse each element
		String[] splitLine = line.split("\t");
        try {
            this.queryId = new Integer(splitLine[0]);
            this.userId = new Integer(splitLine[1]);
            this.dataset = new Integer(splitLine[3]);
            if (splitLine.length == 5) {
                this.fakeQueryId = new Integer(splitLine[4]);
            } else {
                this.fakeQueryId = 0;
            }
        } catch (NumberFormatException ex) {
            throw new NumberFormatException(ex.getMessage()+ " in the query: "+line);
        }
		this.request = splitLine[2].intern();
        this.keywords = null;
    }


    /**
     * This method returns the User Id of the user who issued the query
     * @return the User Id that corresponding to the user who issued the query
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * This method returns the Query Id of the request. If a fake query was sent in the same time of an initial query
     * they have the same Query Id
     * @return the Query Id
     */
    public int getQueryId() {
        return queryId;
    }

    /**
     * This method returns the type of data in which the query belongs
     * It is either the training set (value 0) or the test set (value 1)
     * @return 0 or 1 indicating from which dataset the query belons
     */
    public int getDataset() {
        return dataset;
    }

    /**
     * This method returns the id corresponding the fake query. 0 means the query is the initial user query.
     * @return The id corresponding the fake query.
     */
    public int getFakeQueryId() {
        return fakeQueryId;
    }

    /**
     * This method returns the keywords
     * @return returns the keywords
     */
    public Set<String> getKeywords() {
        if (keywords == null) {
            keywords = CoreNLPTokenizer.getInstance().process(request);
        }
        return keywords;
    }


    /**+
     * Implement the method <code>compareTo</code> for the class Query
     * @param other the <code>Query</code> to test from compaison with this <code>Query</code>
     * @return
     */

    public int compareTo(Query other){
        int d = Integer.compare(userId, other.userId);
        return (d == 0) ? Integer.compare(queryId, other.queryId) : d;
	}


    /**
     * This method returns the distance between two queries
     * @param query
     * @return distance between the two queries
     */
    public double distance(Query query) {


                Double dist  = computeDotProduct(query.getKeywords());

                return dist;
    }

    private Double computeDotProduct(Set<String> vector2) {
        double norm2 = vector2.size();

            Set<String> v1;
            Set<String> v2;
            if (keywords.size() > vector2.size()) {
                v1 = keywords;
                v2 = vector2;
            } else {
                v1 = vector2;
                v2 = keywords;
            }
            double value = 0;
            for (String string : v2) {
                if (v1.contains(string)) {
                    value++;
                }
            }
            double normalizedValue =  value*value / (norm2 * keywords.size());
            if (Double.isNaN(normalizedValue)) {
                return 0.;
            } else {
                return normalizedValue;
            }
    }


    public String getRequest() {
        return request;
    }

    /**
     * Implement the method <code>toString</code> for the class Query
     * @return a String composed of the queryId, the userId, the request, and the dataset
     */
    @Override
    public String toString() {
        return queryId+"\t"+userId+"\t"+request+"\t"+dataset;
    }

}
