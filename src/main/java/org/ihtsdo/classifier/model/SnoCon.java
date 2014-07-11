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
 * The Class SnoCon.
 * Represents a concept.
 */
public class SnoCon implements Comparable<Object> {
    
    /** The id. */
    public int id;
    
    /** The is defined. */
    public boolean isDefined;

    /**
     * Instantiates a new sno con.
     */
    public SnoCon() {
        id = Integer.MIN_VALUE;
        isDefined = false;
    }

    /**
     * Instantiates a new sno con.
     *
     * @param id the id
     * @param isDefined the is defined
     */
    public SnoCon(int id, boolean isDefined) {
        this.id = id;
        this.isDefined = isDefined;
    }

    /**
     * Sets the id.
     *
     * @param i the i
     */
    public void SetId(int i) {
        this.id = i;
    }

    /**
     * Sets the is defined.
     *
     * @param b the b
     */
    public void SetIsDefined(boolean b) {
        this.isDefined = b;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        SnoCon other = (SnoCon) o;
        if (this.id > other.id) {
            return 1; // this is greater than received
        } else if (this.id < other.id) {
            return -1; // this is less than received
        } else {
            return 0; // this == received
        }
    }

}
