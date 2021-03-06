/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataPatterns;

import DataModel.Attribute;
import DataModel.EntityProfile;
import DataModel.IdDuplicates;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.ext.com.google.common.collect.Multisets;

/**
 *
 * @author vefthym
 */
public class SimilarityDistributions extends DatasetStatistics {
    
    public enum AGGREGATION {MAX, AVERAGE};

    public SimilarityDistributions(String data1Path, String data2Path, String groundTruthPath) {
        super(data1Path, data2Path, groundTruthPath);
    }
    
    public void getMatchValueSimDistribution() {
        System.out.println("\nValue similarity of matches distribution:");
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();
        double[] valueSims = new double[duplicates.size()];
        int i = 0; 
        for (IdDuplicates duplicate : duplicates) {            
            double valueSim = getValueSim(profiles1.get(duplicate.getEntityId1()),profiles2.get(duplicate.getEntityId2()));                        
            valueSims[i++] = valueSim;
        }
        Arrays.sort(valueSims); 
        printArray(valueSims);
        double median = getMedian(valueSims);
        System.out.println("Median:"+median);
    }
    
    
    public void getMatchNeighborSimDistribution() {
        System.out.println("\nNeighbor similarity of matches distribution:");
        Map<String,Set<String>> profilesURLs1 = getAllValuesFromProfileURLs(profiles1);
        Map<String,Set<String>> profilesURLs2 = getAllValuesFromProfileURLs(profiles2);
        
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();
        double[] neighborSims = new double[duplicates.size()];
        int i = 0; 
        for (IdDuplicates duplicate : duplicates) {            
            double neighborSim = getNeighborSimAvg(profiles1.get(duplicate.getEntityId1()),profiles2.get(duplicate.getEntityId2()), profilesURLs1, profilesURLs2);            
            neighborSims[i++] = neighborSim;
        }
        Arrays.sort(neighborSims);
        printArray(neighborSims);
        double median = getMedian(neighborSims);
        System.out.println("Median:"+median);
    }
    
    
    public void getNeighborMatchesOfMatches() {
        System.out.println("\nNumber of matches in the neighborhood of matches:");        
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();
        
        Map<String, Integer> urlToPosition = new HashMap<>(profiles1.size()+profiles2.size());
        for (int i = 0; i < profiles1.size(); i++) {
            urlToPosition.put(profiles1.get(i).getEntityUrl(), i);
        }
        for (int i = 0; i < profiles2.size(); i++) {
            urlToPosition.put(profiles2.get(i).getEntityUrl(), i+profiles1.size());
        }
        
        for (IdDuplicates duplicate : duplicates) {     
            EntityProfile e1 = profiles1.get(duplicate.getEntityId1());
            EntityProfile e2 = profiles2.get(duplicate.getEntityId2()); 
            System.out.println("Checking duplicate:"+e1.getEntityUrl()+"("+duplicate.getEntityId1()+"),"+e2.getEntityUrl()+"("+duplicate.getEntityId2()+")");
            
            Set<Integer> e1NeighborIds = getNeighborIds(e1, urlToPosition);
            Set<Integer> e2NeighborIds = getNeighborIds(e2, urlToPosition);
            
            for (Integer e1Neighbor : e1NeighborIds) {
                for (Integer e2Neighbor : e2NeighborIds) {
                    IdDuplicates neighborPair = new IdDuplicates(e1Neighbor, e2Neighbor);
                    System.out.println(neighborPair);
                    if (duplicates.contains(neighborPair)) {
                        //TODO: then add one to the number of neighbor matches per match
                        System.out.println("Found one pair of matching neighbors!");
                    }
                    
                    //added for debugging
                    for (IdDuplicates duplicate2 : duplicates) {
                        if (duplicate2.getEntityId1() == e1Neighbor) {
                            System.out.println(e1Neighbor+" exists in ground truth with "+duplicate2.getEntityId2());
                            return;
                        }
                        if (duplicate2.getEntityId2() == e2Neighbor) {
                            System.out.println(e2Neighbor+" exists in ground truth with "+duplicate2.getEntityId1());
                            return;
                        }
                    }                    
                }
            }
                        
        }
        
    }
    
    
    public void getNumberOfNeighborPairsPerMatch() {
        Map<String,Set<String>> profilesURLs1 = getAllValuesFromProfileURLs(profiles1);
        Map<String,Set<String>> profilesURLs2 = getAllValuesFromProfileURLs(profiles2);             
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();
        double[] neighborPairs = new double[duplicates.size()];        
        
        int i = 0;
        for (IdDuplicates duplicate : duplicates) {
            EntityProfile e1 = profiles1.get(duplicate.getEntityId1());
            EntityProfile e2 = profiles2.get(duplicate.getEntityId2());
            
            int e1Neighbors = 0;
            for (String neighbor: e1.getAllValues()) {
                Set<String> values = profilesURLs1.get(neighbor);
                if (values != null) { //then this value is an entity id
                    e1Neighbors++;
                }
            }
            
            if (e1Neighbors == 0) {
                neighborPairs[i++] = 0;
                continue;
            }
            
            int e2Neighbors = 0;
            for (String neighbor: e2.getAllValues()) {
                Set<String> values = profilesURLs2.get(neighbor);
                if (values != null) { //then this value is an entity id
                    e2Neighbors++;
                }
            }
            neighborPairs[i++] = e1Neighbors*e2Neighbors;            
        }
        Arrays.sort(neighborPairs);
        printArray(neighborPairs);
        double median = getMedian(neighborPairs);
        System.out.println("Median:"+median);
    }
    
    
    public void getRelationshipsBetweenMatchesAndNeighbors() {
        Map<String,Set<String>> profilesURLs1 = getAllValuesFromProfileURLs(profiles1);
        Map<String,Set<String>> profilesURLs2 = getAllValuesFromProfileURLs(profiles2);
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();
        
        Multiset<String> relationPairsCount = HashMultiset.create();
                
        int i = 0;
        for (IdDuplicates duplicate : duplicates) {
            EntityProfile e1 = profiles1.get(duplicate.getEntityId1());
            EntityProfile e2 = profiles2.get(duplicate.getEntityId2());
            
            Set<String> e1Relations = new HashSet<>();            
            for (Attribute att: e1.getAttributes()) {
                String value = att.getValue();
                Set<String> values = profilesURLs1.get(value);
                if (values != null) { //then this value is an entity id
                    e1Relations.add(att.getName());
                }
            }
            
            if (e1Relations.isEmpty()) {                
                continue;
            }
            
            Set<String> e2Relations = new HashSet<>();            
            for (Attribute att: e2.getAttributes()) {                
                String value = att.getValue();
                Set<String> values = profilesURLs2.get(value);
                if (values != null) { //then this value is an entity id
                    e2Relations.add(att.getName());
                }
            }
                        
            for (String att1 : e1Relations) {
                for (String att2 : e2Relations) {
                    relationPairsCount.add("("+att1+", "+att2+")");
                }
            }
        }
        
        //sort and print relationPairs
        for (String relationPair : Multisets.copyHighestCountFirst(relationPairsCount).elementSet()) {
            System.out.println(relationPair+":"+relationPairsCount.count(relationPair));
        }
        
    }
    
    
    public void getValueAndNeighborSimOfMatches(AGGREGATION aggregation) {
        System.out.println("\nValuesim:NeighborSim");
        Map<String,Set<String>> profilesURLs1 = getAllValuesFromProfileURLs(profiles1);
        Map<String,Set<String>> profilesURLs2 = getAllValuesFromProfileURLs(profiles2);
        
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();        
        for (IdDuplicates duplicate : duplicates) {       
            EntityProfile e1 = profiles1.get(duplicate.getEntityId1());
            EntityProfile e2 = profiles2.get(duplicate.getEntityId2());
            double neighborSim;
            switch (aggregation) {
                case MAX:
                    neighborSim = getNeighborSimMax(e1,e2, profilesURLs1, profilesURLs2);      
                    break;
                case AVERAGE:
                    neighborSim = getNeighborSimAvg(e1, e2, profilesURLs1, profilesURLs2);
                    break;
                default:
                    neighborSim = getNeighborSimMax(e1,e2, profilesURLs1, profilesURLs2);
            }            
            if (neighborSim > 1) { //could be slightly above 1 due to imprecision of doubles (slgihtly below 0 is not a problem, as it gets mapped to 0)
                neighborSim = 1; 
            }
            double valueSim = getValueSim(e1, e2);            
            System.out.println(valueSim+":"+neighborSim);
        }        
    }
    
    
    
    public void getValueAndNeighborSimCorrelation(AGGREGATION aggregation) {
        System.out.println("\nValue similarity distribution per neighbor similarity group of matches:");
        Map<String,Set<String>> profilesURLs1 = getAllValuesFromProfileURLs(profiles1);
        Map<String,Set<String>> profilesURLs2 = getAllValuesFromProfileURLs(profiles2);
        
        List<Double>[] neighborSimBands = new List[10]; 
        //element 0 is the band of matches with neighbor sim [0-0.1), element 1 for neighbor sim [0.1-0.2), ..., element 9 for neighbor sim [0.9-1]
        //the values of each element correspond to the value similarities of the pairs belonging to this band 
        for (int i = 0; i < neighborSimBands.length; ++i) {
            neighborSimBands[i] = new ArrayList<>();
        }
        
        Set<IdDuplicates> duplicates = groundTruth.getDuplicates();        
        for (IdDuplicates duplicate : duplicates) {       
            EntityProfile e1 = profiles1.get(duplicate.getEntityId1());
            EntityProfile e2 = profiles2.get(duplicate.getEntityId2());
            double neighborSim;
            switch (aggregation) {
                case MAX:
                    neighborSim = getNeighborSimMax(e1,e2, profilesURLs1, profilesURLs2);      
                    break;
                case AVERAGE:
                    neighborSim = getNeighborSimAvg(e1, e2, profilesURLs1, profilesURLs2);
                    break;
                default:
                    neighborSim = getNeighborSimMax(e1,e2, profilesURLs1, profilesURLs2);
            }            
            if (neighborSim >= 1) { //could be slightly above 1 due to imprecision of doubles (slgihtly below 0 is not a problem, as it gets mapped to 0)
                neighborSim = 0.9999; //just to end in the last bucket 
            }
            neighborSimBands[(int)(neighborSim/0.1)].add(getValueSim(e1, e2));
        }
        
        //we now have 10 distributions, one for each neighbor sim band
        for (int i = 0 ; i < neighborSimBands.length; ++i) {
            System.out.println("Value sims for neighbor sim in ["+(i*0.1)+","+((i+1)*0.1)+"):");
            double[] valueSims = neighborSimBands[i].stream().mapToDouble(d->d).toArray();            
            Arrays.sort(valueSims);
            printArray(valueSims);
        }
    }
    
    
    /////////////////////
    //UTILITY FUNCTIONS//
    /////////////////////
    
    /**
     * Return the set of neighbors' numeric ids for a given entity e. 
     */
    private Set<Integer> getNeighborIds(EntityProfile e, Map<String, Integer> urlToPosition) {        
        Set<Integer> neighborIds = new HashSet<>();
        for (String value : e.getAllValues()) {
            Integer neighborId = urlToPosition.get(value);            
            if (neighborId != null) {//then value is a neighbor of e
                System.out.println(value+"("+neighborId+") is a neighbor of "+e.getEntityUrl());
                neighborIds.add(neighborId);
            }
        }
        return neighborIds;
    }
    
    /**
     * Get the median value of a double array, which must be sorted.
     * @param values a *sorted* array of doubles
     * @return the median value of a double array
     */
    private double getMedian(double[] values) {
        int middle = values.length/2;
        return values.length%2 == 1 ? values[middle] : (values[middle-1] + values[middle]) / 2.0;
    }
    
    private void printArray(double[] array) {
        for (double element : array) {
            System.out.println(element);
        }
    }
    
    public static void main(String[] args) {        
        //Restaurants dataset
        final String basePath = "C:\\Users\\VASILIS\\Documents\\OAEI_Datasets\\OAEI2010\\restaurant\\";
        String dataset1 = basePath+"restaurant1Profiles";
        String dataset2 = basePath+"restaurant2Profiles";
        String datasetGroundtruth = basePath+"restaurantIdDuplicates";
        
        //Rexa-DBLP dataset
//        final String basePath = "C:\\Users\\VASILIS\\Documents\\OAEI_Datasets\\rexa-dblp\\";
//        String dataset1 = basePath+"rexaProfiles";
//        String dataset2 = basePath+"swetodblp_april_2008Profiles";
//        String datasetGroundtruth = basePath+"rexa_dblp_goldstandardIdDuplicates";
        
        //YAGO-IMDb dataset (cannot be loaded in laptop ~60GB RAM used)
//        final String basePath = "C:\\Users\\VASILIS\\Documents\\OAEI_Datasets\\imdb-yago\\";
//        String dataset1 = basePath+"yagoProfiles";
//        String dataset2 = basePath+"imdbProfiles";
//        String datasetGroundtruth = basePath+"imdbgoldFinalIdDuplicates";
        
        //BBCmusic-DBpedia dataset
//        final String basePath = "G:\\VASILIS\\bbcMusic\\";
//        String dataset1 = basePath+"bbc-musicNewProfiles";
//        String dataset2 = basePath+"dbpedia37NewProfiles";
//        String datasetGroundtruth = basePath+"bbc-music_groundTruthUTF8IdDuplicates";

        //        final String basePath = "C:\\Users\\VASILIS\\Documents\\OAEI_Datasets\\imdb-yago\\";
        
        if (args.length == 3) { //override default values with user input
            dataset1 = args[0];
            dataset2 = args[1];
            datasetGroundtruth = args[2];
        }       
        
        SimilarityDistributions dists = new SimilarityDistributions(dataset1, dataset2, datasetGroundtruth);
//        dists.getMatchValueSimDistribution();
//        dists.getMatchNeighborSimDistribution();        
//        dists.getNeighborMatchesOfMatches();
//        dists.getNumberOfNeighborPairsPerMatch();
//        dists.getValueAndNeighborSimCorrelation(AGGREGATION.MAX);
//        dists.getRelationshipsBetweenMatchesAndNeighbors();
        dists.getValueAndNeighborSimOfMatches(AGGREGATION.MAX);
    }
    
}
