/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.classifier.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Class SnoConGrp.
 * Represents a concept list
 */
public class SnoConGrp extends ArrayList<SnoCon> {
    
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sno con grp.
     *
     * @param conList the con list
     * @param needsToBeSorted the needs to be sorted
     */
    public SnoConGrp(List<SnoCon> conList, boolean needsToBeSorted) {
        super();
        // set doSort = true if list not pre-sorted to C1-Group-Type-C2 order
        if (needsToBeSorted)
            Collections.sort(conList);
        this.addAll(conList);
    }

    /**
     * Instantiates a new sno con grp.
     *
     * @param concepts the concepts
     */
    public SnoConGrp(Collection<String> concepts) {
        super();
        // :NYI: defined or not_defined is indeterminate coming from classifier
        // callback.
        for (String cStr : concepts)
            this.add(new SnoCon(unwrap(cStr), false));
        Collections.sort(this);
    }

    /**
     * Instantiates a new sno con grp.
     *
     * @param concepts the concepts
     */
    public SnoConGrp(ArrayList<Integer> concepts) {
        super();
        // :NYI: defined or not_defined is indeterminate coming from classifier
        // callback.
        for (Integer c : concepts)
            this.add(new SnoCon(c.intValue(), false));
        Collections.sort(this);
    }

    /**
     * Instantiates a new sno con grp.
     *
     * @param o the o
     */
    public SnoConGrp(SnoCon o) {
        super();
        this.add(o); // 
    }

    /**
     * Instantiates a new sno con grp.
     */
    public SnoConGrp() {
        super();
    }

    /**
     * Unwrap.
     *
     * @param id the id
     * @return the int
     */
    static private int unwrap(final String id) {
        return Integer.parseInt(String.valueOf(id));
    }

} // class SnoConGrp

