/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    Copyright (C) 2015 George Antony Papadakis (gpapadis@yahoo.gr)
 */

package BlockProcessing.ComparisonRefinement;

import DataModel.AbstractBlock;
import DataModel.BilateralBlock;
import DataModel.Comparison;
import DataModel.UnilateralBlock;
import Utilities.Enumerations.WeightingScheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractMetablocking extends AbstractComparisonRefinementMethod {

    protected boolean nodeCentric;

    protected int[] flags;

    protected double threshold;
    protected double blockAssignments;
    protected double distinctComparisons;
    protected double[] comparisonsPerEntity;
    protected double[] counters;
    protected double[] totalWeights; //used for WJS

    protected final List<Integer> neighbors;
    protected final List<Integer> retainedNeighbors;
    protected WeightingScheme weightingScheme;

    public AbstractMetablocking(WeightingScheme wScheme) {
        super();
        neighbors = new ArrayList<>();
        retainedNeighbors = new ArrayList<>();
        weightingScheme = wScheme;
    }

    protected abstract List<AbstractBlock> pruneEdges();

    protected abstract void setThreshold();

    @Override
    protected List<AbstractBlock> applyMainProcessing() {
        counters = new double[noOfEntities];
        flags = new int[noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            flags[i] = -1;
        }

        blockAssignments = 0;
        if (cleanCleanER) {
            for (BilateralBlock bBlock : bBlocks) {
                blockAssignments += bBlock.getTotalBlockAssignments();
            }
        } else {
            for (UnilateralBlock uBlock : uBlocks) {
                blockAssignments += uBlock.getTotalBlockAssignments();
            }
        }

        if (weightingScheme != null) {            
            if (weightingScheme.equals(WeightingScheme.EJS)) {
                setStatistics();
            } else if (weightingScheme.equals(WeightingScheme.WJS)) {
                setWjsStatistics();
            }
        }

        setThreshold();
        return pruneEdges();
    }

    protected void freeMemory() {
        bBlocks = null;
        flags = null;
        counters = null;
        uBlocks = null;
    }
    
    protected Comparison getComparison(int entityId, int neighborId) {
        if (!cleanCleanER) {
            if (entityId < neighborId) {
                return new Comparison(cleanCleanER, entityId, neighborId);
            } else {
                return new Comparison(cleanCleanER, neighborId, entityId);
            }
        } else {
            if (entityId < datasetLimit) {
                return new Comparison(cleanCleanER, entityId, neighborId - datasetLimit);
            } else {
                return new Comparison(cleanCleanER, neighborId, entityId - datasetLimit);
            }
        }
    }

    protected int[] getNeighborEntities(int blockIndex, int entityId) {
        if (cleanCleanER) {
            if (entityId < datasetLimit) {
                return bBlocks[blockIndex].getIndex2Entities();
            } else {
                return bBlocks[blockIndex].getIndex1Entities();
            }
        }
        return uBlocks[blockIndex].getEntities();
    }

    protected double getWeight(int entityId, int neighborId) {
        switch (weightingScheme) {
            case ARCS:
                return counters[neighborId];
            case CBS:
                return counters[neighborId];
            case ECBS:
                return counters[neighborId] * Math.log10(noOfBlocks / entityIndex.getNoOfEntityBlocks(entityId, 0)) * Math.log10(noOfBlocks / entityIndex.getNoOfEntityBlocks(neighborId, 0));
            case JS:
                return counters[neighborId] / (entityIndex.getNoOfEntityBlocks(entityId, 0) + entityIndex.getNoOfEntityBlocks(neighborId, 0) - counters[neighborId]);
            case EJS:
                double probability = counters[neighborId] / (entityIndex.getNoOfEntityBlocks(entityId, 0) + entityIndex.getNoOfEntityBlocks(neighborId, 0) - counters[neighborId]);
                return probability * Math.log10(distinctComparisons / comparisonsPerEntity[entityId]) * Math.log10(distinctComparisons / comparisonsPerEntity[neighborId]);
            case WJS:
                return counters[neighborId] / (Double.MIN_NORMAL + totalWeights[entityId] + totalWeights[neighborId]);
        }
        return -1;
    }

    protected void setNormalizedNeighborEntities(int blockIndex, int entityId) {
        neighbors.clear();
        if (cleanCleanER) {
            if (entityId < datasetLimit) {
                for (int originalId : bBlocks[blockIndex].getIndex2Entities()) {
                    neighbors.add(originalId + datasetLimit);
                }
            } else {
                for (int originalId : bBlocks[blockIndex].getIndex1Entities()) {
                    neighbors.add(originalId);
                }
            }
        } else {
            if (!nodeCentric) {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId < entityId) {
                        neighbors.add(neighborId);
                    }
                }
            } else {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId != entityId) {
                        neighbors.add(neighborId);
                    }
                }
            }
        }
    }
    
    
    protected List<Integer> getNormalizedNeighborEntities(int blockIndex, int entityId) {
        List<Integer> neighbors = new ArrayList<>(); //on-purpose shadowing
        if (cleanCleanER) {
            if (entityId < datasetLimit) {
                for (int originalId : bBlocks[blockIndex].getIndex2Entities()) {
                    neighbors.add(originalId + datasetLimit);
                }
            } else {
                for (int originalId : bBlocks[blockIndex].getIndex1Entities()) {
                    neighbors.add(originalId);
                }
            }
        } else {
            if (!nodeCentric) {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId < entityId) {
                        neighbors.add(neighborId);
                    }
                }
            } else {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId != entityId) {
                        neighbors.add(neighborId);
                    }
                }
            }
        }
        return neighbors;
    }
    
    protected void setWjsStatistics() {
        totalWeights = new double[noOfEntities];
        for (int entityId = 0; entityId < noOfEntities; ++entityId) {            
            totalWeights[entityId] = 0;
            final int[] associatedBlocks = entityIndex.getEntityBlocks(entityId, 0);                
            if (entityId < datasetLimit) {                                
                for (int blockId : associatedBlocks) {
                    int df1t = bBlocks[blockId].getIndex1Entities().length; //"document frequency" of token t, defining this block, for D1
                    double weight1 = Math.log10((double)datasetLimit/df1t); //weight_1(t) = IDF_1(t) = log(|D1|/ |df_1(t)|)
                    totalWeights[entityId] += weight1;
                }
            } else {                                
                for (int blockId : associatedBlocks) {
                    int df2t = bBlocks[blockId].getIndex2Entities().length;    //"document frequency" of token t, defining this block, for D2
                    double weight2 = Math.log10((double)(noOfEntities-datasetLimit)/df2t); //weight_2(t) = IDF_2(t) = log(|D2|/ |df_2(t)|)
                    totalWeights[entityId] += weight2;
                }
            }
        }
    }
    
    protected void setStatistics() {
        distinctComparisons = 0;
        comparisonsPerEntity = new double[noOfEntities];
        final Set<Integer> distinctNeighbors = new HashSet<Integer>();
        for (int i = 0; i < noOfEntities; i++) {
            final int[] associatedBlocks = entityIndex.getEntityBlocks(i, 0);
            if (associatedBlocks.length != 0) {
                distinctNeighbors.clear();
                for (int blockIndex : associatedBlocks) {
                    for (int neighborId : getNeighborEntities(blockIndex, i)) {
                        distinctNeighbors.add(neighborId);
                    }
                }
                comparisonsPerEntity[i] = distinctNeighbors.size();
                if (!cleanCleanER) {
                    comparisonsPerEntity[i]--;
                }
                distinctComparisons += comparisonsPerEntity[i];
            }
        }
        distinctComparisons /= 2;
    }
}
