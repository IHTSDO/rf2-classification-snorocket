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
package org.ihtsdo.classifier;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.utils.I_Constants;

import java.io.*;
import java.util.*;

/**
 * The Class CycleCheck.
 * This class is responsible to detect cyclic isa relationships from RF2 format.
 * Output results are conceptIds for detected cycled.
 * ConceptIds for detected cycled are saved in file which is a parameter of class constructor.
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
public class CycleCheck {

	/** The concepts. */
	private HashMap<Long,Boolean> concepts;

	/** The isa relationships map. */
	private HashMap<Long, List<Long>> isarelationships;

	/** The concept in loop. */
	private HashSet<Long> conceptInLoop;

	/** The isa relationship typeid. */
	private long ISARELATIONSHIPTYPEID=116680003l;

	/** The concept file. */
	private List<String> conceptFilePaths;

	/** The relationship file. */
	private List<String> relationshipFilePaths;

	/** The output file. */
	private String outputFile;

	/** The reviewed. */
	private int reviewed;

	private File config;

	private XMLConfiguration xmlConfig;

	/** The logger. */
	private  Logger logger;

	private CycleCheck() {
		logger = Logger.getLogger(getClass());
	}

	/**
	 * Instantiates a new cycle check.
	 *
	 * @param conceptFilePaths the concept files
	 * @param relationshipFilePaths the relationship files
	 * @param outputFile the output file
	 */
	public CycleCheck(List<String> conceptFilePaths, List<String> relationshipFilePaths,
			String outputFile) {
		this();
		this.conceptFilePaths = conceptFilePaths;
		this.relationshipFilePaths = relationshipFilePaths;
		this.outputFile = outputFile;
	}

	/**
	 * Instantiates a new cycle check.
	 *
	 * @param conceptFilePathArray the concept files
	 * @param relationshipFilePathArray the relationship files
	 * @param outputFile the output file
	 */
	public CycleCheck(String[] conceptFilePathArray, String[] relationshipFilePathArray,
			String outputFile) {
		this();

		conceptFilePaths = new ArrayList<String>();
		Collections.addAll(conceptFilePaths, conceptFilePathArray);

		relationshipFilePaths = new ArrayList<String>();
		Collections.addAll(relationshipFilePaths, relationshipFilePathArray);

		this.outputFile = outputFile;
	}

	public CycleCheck(File config) throws ConfigurationException {
		this();

		this.config=config;

		getParams();
	}

	@SuppressWarnings("unchecked")
	private void getParams() throws ConfigurationException {

		try {
			xmlConfig=new XMLConfiguration(config);
		} catch (ConfigurationException e) {
			logger.info("CycleCheck - Error happened getting params file." + e.getMessage());
			throw e;
		}
		conceptFilePaths = xmlConfig.getList(I_Constants.CONCEPT_SNAPSHOT_FILES);
		relationshipFilePaths = xmlConfig.getList(I_Constants.RELATIONSHIP_SNAPSHOT_FILES);
		outputFile=xmlConfig.getString(I_Constants.DETECTED_CYCLE_OUTPUT_FILE);

		logger.info("CheckCycle - Parameters:");
		logger.info("Concept files : ");
		for (String concept: conceptFilePaths){
			logger.info( concept);
		}
		logger.info("Relationship files : " );
		for (String relFile: relationshipFilePaths){
			logger.info(relFile);
		}
		logger.info("Detected cycle output file = " + outputFile);

	}

	/**
	 * Cycle detected.
	 *
	 * @return true, if successful
	 * @throws org.ihtsdo.classifier.ClassificationException
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public boolean cycleDetected() throws FileNotFoundException, IOException, ClassificationException {
		conceptInLoop=new HashSet<Long>();
		loadConceptsFile();
		loadIsaRelationshipsFile();
		for(Long con:concepts.keySet()){
			if (!concepts.get(con)){
				List<Long> desc=new ArrayList<Long>();
				findCycle(con, desc);
				desc.remove(con);
				reviewed++;
				concepts.put(con, true);
			}
		}
		if (conceptInLoop.size()>0){
			saveDetectedCyclesFile();
			logger.info("CYCLE DETECTED - Concepts reviewed: "  + reviewed );
			logger.info("Please get conceptId for detected cycles in file:" + outputFile);
			return true;
		}
		logger.info("*******NO CYCLE DETECTED***** - Concepts reviewed: "  + reviewed);
		return false;
	}

	/**
	 * Save conceptIds in detected cycles file.
	 *
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private void saveDetectedCyclesFile() throws IOException {
		FileOutputStream fos = new FileOutputStream( outputFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		bw.append("conceptId");
		bw.append("\r\n");

		for (Long concept:conceptInLoop){
			bw.append(concept.toString());
			bw.append("\r\n");
		}
		bw.close();
		bw=null;
		fos=null;
		osw=null;

	}

	/**
	 * Find cycle.
	 *
	 * @param con the con
	 * @param desc the desc
	 */
	private void findCycle(Long con, List<Long> desc) throws ClassificationException {
		List<Long> parents=isarelationships.get(con);
		if (parents!=null){
			desc.add(con);
			for (Long parent:parents){
				if (desc.contains(parent)){
					conceptInLoop.add(parent);
				}else{
					Boolean aBoolean = concepts.get(parent);
					if (aBoolean != null) {
						if (!aBoolean) {
							findCycle(parent, desc);
							desc.remove(parent);
							reviewed++;
							concepts.put(parent, true);
						}
					} else {
						throw new ClassificationException("SCTID " + parent + " is declared as a parent of " + con + " but is missing or inactive in the concept file.");
					}
				}
			}
		}
	}

	/**
	 * Load concepts file.
	 *
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public void loadConceptsFile() throws FileNotFoundException, IOException {

		concepts=new HashMap<Long, Boolean>();
		int count = 0;
		for (String concept: conceptFilePaths){
			logger.info("Starting Concepts: " + concept);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(concept), "UTF8"));
			try {
				String line = br.readLine();
				line=br.readLine();
				while (line != null) {
					if (line.isEmpty()) {
						continue;
					}
					String[] columns = line.split("\t",-1);
					if ( columns[2].equals("1") ){

						concepts.put(Long.parseLong(columns[0]), false);
						count++;
						if (count % 100000 == 0) {
							System.out.print(".");
						}
					}
					line = br.readLine();
				}

				logger.info(".");
				logger.info("Active concepts loaded = " + concepts.size());
			} finally {
				br.close();
			}
		}

	}




	/**
	 * Load isa relationships file.
	 *
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public void loadIsaRelationshipsFile() throws FileNotFoundException, IOException {

		isarelationships=new HashMap<Long,List<Long>>();
		int count = 0;
		for (String relFile: relationshipFilePaths){
			logger.info("Starting Isas Relationships from: " + relFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(relFile), "UTF8"));
			try {
				String line = br.readLine();
				line=br.readLine();
				while (line != null) {
					if (line.isEmpty()) {
						continue;
					}
					String[] columns = line.split("\t");
					if (Long.parseLong(columns[7])==ISARELATIONSHIPTYPEID 
							&& columns[2].equals("1")){
						Long sourceId = Long.parseLong(columns[4]);

						List<Long> relList = isarelationships.get(sourceId);
						if (relList == null) {
							relList = new ArrayList<Long>();
						}
						relList.add(Long.parseLong(columns[5]));
						isarelationships.put(sourceId, relList);

						count++;
						if (count % 100000 == 0) {
							System.out.print(".");
						}
					}
					line = br.readLine();
				}
				logger.info(".");
				logger.info("Active isas Relationships for " +  isarelationships.size() + "  concepts loaded.");
			} finally {
				br.close();
			}
		}
	}
}
