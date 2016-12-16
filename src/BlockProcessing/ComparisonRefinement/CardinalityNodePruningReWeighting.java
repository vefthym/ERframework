/*
* Copyright [2016] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package BlockProcessing.ComparisonRefinement;

import DataModel.AbstractBlock;
import DataModel.Comparison;
import DataModel.SimilarityPairs;
import Utilities.Comparators.ComparisonWeightComparator;
import Utilities.Enumerations.WeightingScheme;
import Utilities.TextModels.AbstractModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author vefthym
 */

public class CardinalityNodePruningReWeighting extends CardinalityEdgePruning {
    
    protected int firstId;
    protected int lastId;
    protected Set<Comparison>[] nearestEntities;
    
    
    protected AbstractModel[] entityModelsD1;
    protected AbstractModel[] entityModelsD2;
    protected AbstractModel[] neighborModelsD1;
    protected AbstractModel[] neighborModelsD2;
    
    protected SimilarityPairs simPairs;
    
    
    public CardinalityNodePruningReWeighting(WeightingScheme scheme) {
        super(scheme);
        nodeCentric = true;
    }

    public CardinalityNodePruningReWeighting(WeightingScheme scheme, AbstractModel[] entityModelsD1, AbstractModel[] entityModelsD2, AbstractModel[] neighborModelsD1, AbstractModel[] neighborModelsD2) {
        super(scheme);
        this.entityModelsD1 = entityModelsD1;
        this.entityModelsD2 = entityModelsD2;
        this.neighborModelsD1 = neighborModelsD1;
        this.neighborModelsD2 = neighborModelsD2;        
    }

    @Override
    public String getMethodInfo() {
        return "Cardinality Node Pruning with matching: a Meta-blocking method that retains for every entity, "
                + "the comparisons that correspond to its top-1 weighted edges in the blocking graph.";
    }

    @Override
    public String getMethodParameters() {
        return "Cardinality Node Pruning with matching involves two parameters:\n"
                + "the weighting scheme that assigns weights to the edges of the blcoking graph, "
                + "and the similarity threshold above which edges are retained (a matching threshold for the top edge).";
    }
    
    protected boolean isValidComparison(int entityId, Comparison comparison) {
        int neighborId = comparison.getEntityId1()==entityId?comparison.getEntityId2():comparison.getEntityId1();
        if (cleanCleanER && entityId < datasetLimit) {
            neighborId += datasetLimit;
        }
        
        if (nearestEntities[neighborId] == null) {
            return true;
        }
                
        if (nearestEntities[neighborId].contains(comparison)) {
            return entityId < neighborId;
        }

        return true;
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        nearestEntities = new Set[noOfEntities];
        topKEdges = new PriorityQueue<>((int) (2 * threshold), new ComparisonWeightComparator());
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < noOfEntities; i++) {
                processArcsEntity(i);
                verifyValidEntities(i);
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                processEntity(i);
                verifyValidEntities(i);
            }
        }
        List<AbstractBlock> newBlocks = new ArrayList<>();
        retainValidComparisons(newBlocks);
        return newBlocks;
    }
    
    protected void retainValidComparisons(List<AbstractBlock> newBlocks) {        
        List<Comparison> retainedComparisons = new ArrayList<>();        
        for (int i = 0; i < noOfEntities; i++) {
            double similaritySum = 0;
            int validComps = 0;
            if (nearestEntities[i] != null) {
                retainedComparisons.clear();
                for (Comparison comparison : nearestEntities[i]) {
                    if (isValidComparison(i, comparison)) {
                        validComps++;
                        double similarity = getSimilarity(comparison);
                        comparison.setUtilityMeasure(similarity);
                        similaritySum += similarity;
                    }
                }
                if (similaritySum == 0) {
                    continue;
                }
                
                final double AVERAGE_SIM = similaritySum / validComps;
                
                System.out.println("AVERAGE_SIM for entity "+i+" = "+similaritySum+"/"+validComps+"="+AVERAGE_SIM);
                
                for (Comparison comparison : nearestEntities[i]) {
                    if (isValidComparison(i, comparison)) {
                        double original_sim = comparison.getUtilityMeasure();
                        if (original_sim > AVERAGE_SIM) {
                            comparison.setUtilityMeasure(original_sim/AVERAGE_SIM); //should be greater than 1... (FIXME:  THIS IS IGNORED AFTERWARDS!)
                            System.out.println("New similarity of "+comparison.getEntityId1()+", "+comparison.getEntityId2()+" is "+ comparison.getUtilityMeasure());
                            retainedComparisons.add(comparison); //keep only the comparisons with similarity above average (per entity)
                            simPairs.addComparison(comparison);
                        }
                    }
                }                
                
                if (!retainedComparisons.isEmpty())
                    addDecomposedBlock(retainedComparisons, newBlocks);
            }
        }        
    }
    
    public SimilarityPairs getSimPairs() {
        return simPairs;
    }

    protected void setLimits() {
        firstId = 0;
        lastId = noOfEntities;
    }
    
    @Override
    protected void setThreshold() {
        threshold = Math.max(1, blockAssingments / noOfEntities);
    }
    
    @Override
    protected void verifyValidEntities(int entityId) {
        if (validEntities.isEmpty()) {
            return;
        }

        topKEdges.clear();
        minimumWeight = Double.MIN_VALUE;
        validEntities.stream().forEach((neighborId) -> {
            double weight = getWeight(entityId, neighborId);
            if (!(weight < minimumWeight)) {
                Comparison comparison = getComparison(entityId, neighborId);
                comparison.setUtilityMeasure(weight);
                topKEdges.add(comparison);
                if (threshold < topKEdges.size()) {
                    Comparison lastComparison = topKEdges.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        });
        nearestEntities[entityId] = new HashSet<>(topKEdges);
    }
    
    /**
     * Returns a synthetic similarity for the pair, 
     * which is a weighted sum of profile similarity and neighbor similarity.
     * @param comparison
     * @return 
     */
    public double getSimilarity(Comparison comparison) {
        final double a = 0.66;
        double profile_similarity;        
        double neighbor_similarity;        
        
        if (entityModelsD1[comparison.getEntityId1()].getNoOfDocuments() == 0) {      
            return 0;
        }
        
        if (cleanCleanER) {
            if (entityModelsD2[comparison.getEntityId2()].getNoOfDocuments() == 0) { 
                return 0;
            }
            profile_similarity =  entityModelsD1[comparison.getEntityId1()].getSimilarity(entityModelsD2[comparison.getEntityId2()]);
            neighbor_similarity = neighborModelsD1[comparison.getEntityId1()].getSimilarity(neighborModelsD2[comparison.getEntityId2()]);
        } else {
            if (entityModelsD1[comparison.getEntityId2()].getNoOfDocuments() == 0) {            
                return 0;
            }            
            profile_similarity = entityModelsD1[comparison.getEntityId1()].getSimilarity(entityModelsD1[comparison.getEntityId2()]);
            neighbor_similarity = neighborModelsD1[comparison.getEntityId1()].getSimilarity(neighborModelsD1[comparison.getEntityId2()]);
        }
        return a * profile_similarity + (1-a) * neighbor_similarity;
    }
}