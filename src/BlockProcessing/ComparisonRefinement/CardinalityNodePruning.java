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
import Utilities.Comparators.ComparisonWeightComparator;
import Utilities.Enumerations.WeightingScheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author gap2
 */

public class CardinalityNodePruning extends CardinalityEdgePruning {
    
    protected int firstId;
    protected int lastId;
    protected Set<Comparison>[] nearestEntities;
    
    public CardinalityNodePruning(WeightingScheme scheme) {
        super(scheme);
        nodeCentric = true;
    }

    @Override
    public String getMethodInfo() {
        return "Cardinality Node Pruning: a Meta-blocking method that retains for every entity, "
                + "the comparisons that correspond to its top-k weighted edges in the blocking graph.";
    }

    @Override
    public String getMethodParameters() {
        return "Cardinality Node Pruning involves a single parameter:\n"
                + "the weighting scheme that assigns weights to the edges of the blcoking graph.";
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
        topKEdges = new PriorityQueue<Comparison>((int) (2 * threshold), new ComparisonWeightComparator());
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
        final List<Comparison> retainedComparisons = new ArrayList<>();
        for (int i = 0; i < noOfEntities; i++) {
            if (nearestEntities[i] != null) {
                retainedComparisons.clear();
                for (Comparison comparison : nearestEntities[i]) {
                    if (isValidComparison(i, comparison)) {
                        retainedComparisons.add(comparison);
                    }
                }
                addDecomposedBlock(retainedComparisons, newBlocks);
            }
        }
    }

    protected void setLimits() {
        firstId = 0;
        lastId = noOfEntities;
    }
    
    @Override
    protected void setThreshold() {
        threshold = Math.max(1, blockAssignments / noOfEntities);
    }
    
    @Override
    protected void verifyValidEntities(int entityId) {
        if (validEntities.isEmpty()) {
            return;
        }

        topKEdges.clear();
        minimumWeight = Double.MIN_VALUE;
        for (int neighborId : validEntities) {
            double weight = getWeight(entityId, neighborId);
            if (weight < minimumWeight) {
                continue;
            }

            Comparison comparison = getComparison(entityId, neighborId);
            comparison.setUtilityMeasure(weight);

            topKEdges.add(comparison);
            if (threshold < topKEdges.size()) {
                Comparison lastComparison = topKEdges.poll();
                minimumWeight = lastComparison.getUtilityMeasure();
            }
        }
        nearestEntities[entityId] = new HashSet<Comparison>(topKEdges);
    }
}
