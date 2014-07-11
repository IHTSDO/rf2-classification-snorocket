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
import java.util.Iterator;
import java.util.List;

/**
 * The Class SnoGrpList.
 * Represents a relationship group list
 */
public class SnoGrpList extends ArrayList<SnoGrp> {
    
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sno grp list.
     */
    public SnoGrpList() {
        super();
    }

    /**
     * Construct a ROLE_GROUP_LIST from <code>List&lt;SnoRel&gt;</code><br>
     * <br>
     * <font color=#990099> IMPLEMENTATION NOTE: roleGroups MUST be pre-sorted
     * in C1-Group-Type-C2 order for this routine. Pre-sorting is used to
     * provide overall computational efficiency.</font>
     *
     * @param rels the rels
     */
    public SnoGrpList(List<SnoRel> rels) {
        super();
        // First Group
        SnoGrp group = new SnoGrp();
        this.add(group);

        // First SnoRel in First Group
        Iterator<SnoRel> it = rels.iterator();
        SnoRel snoRelA = it.next();
        group.add(snoRelA);

        while (it.hasNext()) {
            SnoRel snoRelB = it.next();
            if (snoRelB.group == snoRelA.group) {
                group.add(snoRelB); // ADD TO SAME GROUP
            } else {
                group = new SnoGrp(); // CREATE NEW GROUP
                this.add(group); // ADD GROUP TO GROUP LIST
                group.add(snoRelB);
            }
            snoRelA = snoRelB;
        }
    }

    /**
     * Count rels.
     *
     * @return the int
     */
    public int countRels() {
        int returnCount = 0;
        for (SnoGrp sg : this)
            returnCount += sg.size();
        return returnCount;
    }

   
    /**
     * Which groups in this do not have ANY equal group in groupListB?<br>
     * <br>
     * <font color=#990099> IMPLEMENTATION NOTE: roleGroups MUST be pre-sorted
     * in C1-Group-Type-C2 order for this routine. Pre-sorting is used to
     * provide overall computational efficiency.</font>
     *
     * @param groupListB the group list b
     * @return <code>SnoGrpList</code>
     */
    public SnoGrpList whichNotEqual(SnoGrpList groupListB) {
        SnoGrpList sg = new SnoGrpList();
        for (SnoGrp groupA : this) {
            boolean foundEqual = false;
            for (SnoGrp groupB : groupListB) {
                if (groupA.equals(groupB)) {
                    foundEqual = true;
                    break;
                }
            }
            if (!foundEqual) {
                sg.add(groupA);
            }
        }
        return sg;
    }

}
