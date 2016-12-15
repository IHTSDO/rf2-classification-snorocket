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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Class Relationship.
 * Represents a relationship.
 */
public class Relationship implements Comparable<Object> {

    /** The relationship Id. */
    private String relationshipId;
    
    /** The source Id. */
    private int sourceId; 
    
    /** The destination Id. */
    private int destinationId;
    
    /** The type id. */
    private int typeId;
    
    /** The group. */
    private int group;
    
    private String moduleId;
    
    // Relationship form a versioned "new" database perspective
    /**
     * Instantiates a new sno rel.
     *
     * @param sourceId the c1 id
     * @param destinationId the c2 id
     * @param roleTypeId the role type id
     * @param group the group
     * @param relationshipId the rel nid
     */
    public Relationship(int sourceId, int destinationId, int roleTypeId, int group, String moduleId, String relationshipId) {
        this.sourceId = sourceId;
        this.destinationId=destinationId;
        this.typeId=roleTypeId;
        this.group = group;
        this.moduleId = moduleId;
        this.relationshipId = relationshipId;
    }

    // Relationship from a SnoRocket perspective
    /**
     * Instantiates a new sno rel.
     *
     * @param sourceId the c1 id
     * @param destinationId the c2 id
     * @param roleTypeId the role type id
     * @param group the group
     */
    public Relationship(int sourceId, int destinationId, int roleTypeId, int group, String moduleId) {
    	  this.sourceId = sourceId;
          this.destinationId=destinationId;
          this.typeId=roleTypeId;
          this.group = group;
          this.moduleId = moduleId;
          this.relationshipId = "null";
    }

    /**
     * Gets the rel id.
     *
     * @return the rel id
     */
    public String getRelId() {
        return relationshipId;
    }

    // default sort order [c1-group-type-c2]
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Relationship other = (Relationship) o;
        int thisMore = 1;
        int thisLess = -1;
        if (this.getSourceId() > other.getSourceId()) {
            return thisMore;
        } else if (this.getSourceId() < other.getSourceId()) {
            return thisLess;
        } else {
            if (this.getGroup() > other.getGroup()) {
                return thisMore;
            } else if (this.getGroup() < other.getGroup()) {
                return thisLess;
            } else {
                if (this.getTypeId() > other.getTypeId()) {
                    return thisMore;
                } else if (this.getTypeId() < other.getTypeId()) {
                    return thisLess;
                } else {
                    if (this.getDestinationId() > other.getDestinationId()) {
                        return thisMore;
                    } else if (this.getDestinationId() < other.getDestinationId()) {
                        return thisLess;
                    } else {
                        return 0; // this == received
                    }
                }
            }
        }
    } // Relationship.compareTo()

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSourceId());
        sb.append(": ");
        sb.append(getTypeId());
        sb.append(": ");
        sb.append(getDestinationId());
        return sb.toString();
    }

    /**
     * To string c1.
     *
     * @return the string
     */
    public String toStringC1() {
       
        return Integer.toString(getSourceId());
    }

    /**
     * To string type.
     *
     * @return the string
     */
    public String toStringType() {
       
        return Integer.toString(getTypeId());
    }

    /**
     * To string c2.
     *
     * @return the string
     */
    public String toStringC2() {
       
        return Integer.toString(getDestinationId());
    }

    /**
     * To string group.
     *
     * @return the string
     */
    public String toStringGroup() {
        return Integer.toString(getGroup());
    }


    /**
     * To string4 file.
     *
     * @return the string
     */
    public String toString4File() {
        return "relId     \t" + "sourceId      \t" + "destinationId      \t" + "typeId    \t" + "group";
    }

    /**
     * Dump to file.
     *
     * @param srl the srl
     * @param fName the f name
     * @param format the format
     */
    public static void dumpToFile(List<Relationship> srl, String fName, int format) {

        try {

            BufferedWriter bw = new BufferedWriter(new FileWriter(fName));
            if (format == 1) { // RAW NIDs
                for (Relationship sr : srl) {
                    bw.write(sr.getSourceId() + "\t" + sr.getTypeId() + "\t" + sr.getDestinationId() + "\t" + sr.getGroup() + "\r\n");
                }
            }
            
            if (format == 6) { // Distribution Form
                int index = 0;
                bw.write("RELATIONSHIPID\t" + "CONCEPTID1\t" + "RELATIONSHIPTYPE\t"
                        + "CONCEPTID2\t" + "CHARACTERISTICTYPE\t" + "REFINABILITY\t"
                        + "RELATIONSHIPGROUP\r\n");
                for (Relationship sr : srl) {
                    // RELATIONSHIPID + CONCEPTID1 + RELATIONSHIPTYPE +
                    // CONCEPTID2
                    bw.write("#" + index + "\t" + sr.getSourceId() + "\t" + sr.getTypeId() + "\t" + sr.getDestinationId()
                            + "\t");
                    // CHARACTERISTICTYPE + REFINABILITY + RELATIONSHIPGROUP
                    bw.write("NA\t" + "NA\t" + sr.getGroup() + "\r\n");
                    index += 1;
                }
            }
            bw.flush();
            bw.close();
        
        } catch (IOException e) {
            // can be caused by new FileWriter
            Logger.getLogger(Relationship.class.getName()).log(Level.SEVERE, null, e);
        }
    }

	/**
	 * @return the sourceId
	 */
	public int getSourceId() {
		return sourceId;
	}

	/**
	 * @return the destinationId
	 */
	public int getDestinationId() {
		return destinationId;
	}

	/**
	 * @return the typeId
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * @return the group
	 */
	public int getGroup() {
		return group;
	}

	/**
	 * @return the moduleId
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * @param role group number
	 */
	public void setGroup(int rgNum) {
		this.group = rgNum;
	}

	/**
	 * @param moduleId
	 */
	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
} 


