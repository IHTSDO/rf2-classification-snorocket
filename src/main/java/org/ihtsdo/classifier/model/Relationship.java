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

    /** The rel nid. */
    public String relNid;
    
    /** The c1 id. */
    public int c1Id; // from I_RelVersioned
    
    /** The c2 id. */
    public int c2Id; // from I_RelVersioned
    
    /** The type id. */
    public int typeId; // from I_RelPart
    
    /** The group. */
    public int group; // from I_RelPart

    // Relationship form a versioned "new" database perspective
    /**
     * Instantiates a new sno rel.
     *
     * @param c1Id the c1 id
     * @param c2Id the c2 id
     * @param roleTypeId the role type id
     * @param group the group
     * @param relNid the rel nid
     */
    public Relationship(int c1Id, int c2Id, int roleTypeId, int group, String relNid) {
        this.c1Id = c1Id;
        this.c2Id = c2Id;
        this.typeId = roleTypeId;
        this.group = group;
        this.relNid = relNid;
    }

    // Relationship from a SnoRocket perspective
    /**
     * Instantiates a new sno rel.
     *
     * @param c1Id the c1 id
     * @param c2Id the c2 id
     * @param roleTypeId the role type id
     * @param group the group
     */
    public Relationship(int c1Id, int c2Id, int roleTypeId, int group) {
        this.c1Id = c1Id;
        this.c2Id = c2Id;
        this.typeId = roleTypeId;
        this.group = group;
        this.relNid = "null";
    }

    /**
     * Gets the rel id.
     *
     * @return the rel id
     */
    public String getRelId() {
        return relNid;
    }

    /**
     * Sets the nid.
     *
     * @param nid the new nid
     */
    public void setNid(String nid) {
        this.relNid = nid;
    }

    // default sort order [c1-group-type-c2]
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Relationship other = (Relationship) o;
        int thisMore = 1;
        int thisLess = -1;
        if (this.c1Id > other.c1Id) {
            return thisMore;
        } else if (this.c1Id < other.c1Id) {
            return thisLess;
        } else {
            if (this.group > other.group) {
                return thisMore;
            } else if (this.group < other.group) {
                return thisLess;
            } else {
                if (this.typeId > other.typeId) {
                    return thisMore;
                } else if (this.typeId < other.typeId) {
                    return thisLess;
                } else {
                    if (this.c2Id > other.c2Id) {
                        return thisMore;
                    } else if (this.c2Id < other.c2Id) {
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
        sb.append(c1Id);
        sb.append(": ");
        sb.append(typeId);
        sb.append(": ");
        sb.append(c2Id);
        return sb.toString();
    }

    /**
     * To string c1.
     *
     * @return the string
     */
    public String toStringC1() {
       
        return Integer.toString(c1Id);
    }

    /**
     * To string type.
     *
     * @return the string
     */
    public String toStringType() {
       
        return Integer.toString(typeId);
    }

    /**
     * To string c2.
     *
     * @return the string
     */
    public String toStringC2() {
       
        return Integer.toString(c2Id);
    }

    /**
     * To string group.
     *
     * @return the string
     */
    public String toStringGroup() {
        return Integer.toString(group);
    }


    /**
     * To string4 file.
     *
     * @return the string
     */
    public String toString4File() {
        return "relId     \t" + "c1Id      \t" + "c2Id      \t" + "typeId    \t" + "group";
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
                    bw.write(sr.c1Id + "\t" + sr.typeId + "\t" + sr.c2Id + "\t" + sr.group + "\r\n");
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
                    bw.write("#" + index + "\t" + sr.c1Id + "\t" + sr.typeId + "\t" + sr.c2Id
                            + "\t");
                    // CHARACTERISTICTYPE + REFINABILITY + RELATIONSHIPGROUP
                    bw.write("NA\t" + "NA\t" + sr.group + "\r\n");
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
} // class Relationship


