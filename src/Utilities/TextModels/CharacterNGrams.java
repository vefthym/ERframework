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

package Utilities.TextModels;

import Utilities.Enumerations.RepresentationModel;
import Utilities.Enumerations.SimilarityMetric;

/**
 *
 * @author G.A.P. II
 */

public class CharacterNGrams extends BagModel {
    
    public CharacterNGrams(int n, RepresentationModel model, SimilarityMetric simMetric, String iName) {
        super(n, model, simMetric, iName);
    }
    
    @Override
    public void updateModel(String text) {
        noOfDocuments++;
        int currentPosition = 0;
        final int length = text.length() - (nSize-1);
        while (currentPosition < length) {
            noOfTotalTerms++;
            final String term = text.substring(currentPosition, currentPosition + nSize);
            Integer frequency = itemsFrequency.get(term);
            if (frequency == null) {
                frequency = new Integer(0);
            } 
            frequency++;
            itemsFrequency.put(term, frequency);
            currentPosition++;
        }
    }
}