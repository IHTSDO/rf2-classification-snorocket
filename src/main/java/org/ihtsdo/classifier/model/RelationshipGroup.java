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

/**
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
import java.util.*;

/**
 * A self sorting relationships set.
 */
public class RelationshipGroup extends TreeSet<Relationship> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The Constant debug. */
    // SORT BY [ROLE-C2-GROUP-C2]
    private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR = new Comparator<Relationship>() {
		public int compare(Relationship o1, Relationship o2) {
			int thisMore = 1;
			int thisLess = -1;
			if (o1.getTypeId() > o2.getTypeId()) {
				return thisMore;
			} else if (o1.getTypeId() < o2.getTypeId()) {
				return thisLess;
			} else {
				if (o1.getDestinationId() > o2.getDestinationId()) {
					return thisMore;
				} else if (o1.getDestinationId() < o2.getDestinationId()) {
					return thisLess;
				} else {
					return 0; // EQUAL
				}
			}
		} // compare()
	};

    /**
     * Instantiates a self sorting relationship group.
     */
    public RelationshipGroup() {
        super(RELATIONSHIP_COMPARATOR);
    }

    /**
     * Does roleGroupA Role-Value match roleGroupB Role-Values?<br>
     * <br>
     * <font color=#990099> IMPLEMENTATION NOTE: roleGroups MUST be pre-sorted
     * in C1-Group-Type-C2 order for this routine. Pre-sorting is used to
     * provide overall computational efficiency.</font>
     *
     * @param roleGroupB the role group b
     * @return true iff RoleValues match
     */
    public boolean equals(RelationshipGroup roleGroupB) {
        int sizeA = this.size();
        if (sizeA != roleGroupB.size()) {
            return false; // trivial case, does not have same number of elements
        }

        if (sizeA == 0) {
            return true; // trivial case, both empty
        }

        Iterator<Relationship> iteratorA = this.iterator();
        Iterator<Relationship> iteratorB = roleGroupB.iterator();
        Relationship relationshipA;
        Relationship relationshipB;
        while (iteratorA.hasNext() && (relationshipA = iteratorA.next()) != null) {
            relationshipB = iteratorB.next();
            if (relationshipA.getTypeId() != relationshipB.getTypeId() || relationshipA.getDestinationId() != relationshipB.getDestinationId()) {
                return false;
            }
        }

        return true;
    }

} // class RelationshipGroup

