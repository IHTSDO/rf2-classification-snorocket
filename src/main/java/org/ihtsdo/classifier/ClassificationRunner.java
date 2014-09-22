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

import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.snapi.I_Snorocket_123.I_Callback;
import au.csiro.snorocket.snapi.I_Snorocket_123.I_EquivalentCallback;
import au.csiro.snorocket.snapi.Snorocket_123;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.model.*;
import org.ihtsdo.classifier.utils.GetDescendants;
import org.ihtsdo.classifier.utils.I_Constants;

import java.io.*;
import java.util.*;

/**
 * The Class ClassificationRunner.
 * This class is responsible to classify stated relationships from RF2 format using snorocket reasoner.
 * Output results are inferred relationships composed of taxonomy and attributes.
 * Inferred relationships are saved in file which is a parameter of class constructor.
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
public class ClassificationRunner {

	/** The prev inferred rels. */
	private List<String> previousInferredRelationships;

	/** The output tmp. */
	private File tempRelationshipStore;

	private File config;

	public ClassificationRunner() {
		logger = Logger.getLogger("org.ihtsdo.classifier.ClassificationRunner");
	}

	/**
	 * Instantiates a new classification runner.
	 *
	 * @param module the module
	 * @param releaseDate the release date
	 * @param conceptFilePaths the concepts
	 * @param statedRelationshipFilePaths the stated rels
	 * @param previousInferredRelationshipFilePaths the prev inferred rels
	 * @param inferredRelationshipOutputFilePath the output rels
	 * @param equivalencyReportOutputFilePath the equiv concept file
	 */
	public ClassificationRunner(String module, String releaseDate, List<String> conceptFilePaths,
			List<String> statedRelationshipFilePaths, List<String> previousInferredRelationshipFilePaths,
			String inferredRelationshipOutputFilePath, String equivalencyReportOutputFilePath) {
		this();
		this.module = module;
		this.releaseDate = releaseDate;
		this.concepts = conceptFilePaths;
		this.statedRelationships = statedRelationshipFilePaths;
		this.newInferredRelationships = inferredRelationshipOutputFilePath;
		this.equivalencyReport = equivalencyReportOutputFilePath;
		this.previousInferredRelationships = previousInferredRelationshipFilePaths;

		File outputFile=new File(newInferredRelationships);
		tempRelationshipStore=new File(outputFile.getParentFile(), "Tmp_" + outputFile.getName());
	}

	/**
	 * Instantiates a new classification runner.
	 *
	 * @param module the module
	 * @param releaseDate the release date
	 * @param concepts the concepts
	 * @param statedRelationships the stated rels
	 * @param previousInferredRelationships the prev inferred rels
	 * @param newInferredRelationships the output rels
	 * @param equivalencyReport the equiv concept file
	 */
	public ClassificationRunner(String module, String releaseDate, String[] concepts,
			String[] statedRelationships,String[] previousInferredRelationships, String newInferredRelationships, String equivalencyReport) {
		this();
		this.module = module;
		this.releaseDate = releaseDate;

		this.concepts = new ArrayList<String>();
		Collections.addAll(this.concepts, concepts);

		this.statedRelationships = new ArrayList<String>();
		Collections.addAll(this.statedRelationships, statedRelationships);

		this.newInferredRelationships = newInferredRelationships;
		this.equivalencyReport = equivalencyReport;

		this.previousInferredRelationships = new ArrayList<String>();
		Collections.addAll(this.previousInferredRelationships, previousInferredRelationships);

		File outputFile=new File(newInferredRelationships);
		tempRelationshipStore=new File(outputFile.getParentFile(),"Tmp_" + outputFile.getName());
	}

	public ClassificationRunner(File config) throws ConfigurationException {
		this();

		this.config=config;
		getParams();

		File outputFile=new File(newInferredRelationships);
		tempRelationshipStore=new File(outputFile.getParentFile(),"Tmp_" + outputFile.getName());

	}

	/** The edited snomed concepts. */
	private  ArrayList<StringIDConcept> cEditSnoCons;

	/** The edit snomed rels. */
	private  ArrayList<Relationship> cEditRelationships;

	/** The con ref list. */
	private  HashMap<Integer, String> conRefList;

	/** The con str list. */
	private  HashMap<String,Integer> conStrList;

	/** The logger. */
	private  Logger logger;

	/** The c rocket sno rels. */
	private  ArrayList<Relationship> cRocketRelationships;

	/** The isa. */
	private  Integer isa;

	/** The concept module. */
	private  HashMap<Integer,String> conceptModule;

	//params
	/** The module. */
	private  String module;

	/** The release date. */
	private  String releaseDate;

	/** The concepts. */
	private  List<String> concepts;

	/** The stated rels. */
	private  List<String> statedRelationships;

	/** The output rels. */
	private  String newInferredRelationships;

	/** The equiv concept file. */
	private  String equivalencyReport;

	/** The retired set. */
	private HashSet<String> retiredSet;

	private XMLConfiguration xmlConfig;

	/**
	 * Execute the classification.
	 */
	public void execute(){

		try {

			logger.info("\r\n::: [Test Snorocket] execute() -- begin");
			cEditSnoCons = new ArrayList<StringIDConcept>();
			cEditRelationships = new ArrayList<Relationship>();
			conRefList=new HashMap<Integer,String>();
			conStrList=new HashMap<String,Integer>();

			loadConceptFilesTomap(concepts,false);


			HashSet<String>parentConcepts=new HashSet<String>();
			parentConcepts.add(I_Constants.ATTRIBUTE_ROOT_CONCEPT); //concept model attribute

			int[] roles =getRoles(parentConcepts); 
			int ridx = roles.length;
			if (roles.length > 100) {
				String errStr = "Role types exceeds 100. This will cause a memory issue. "
						+ "Please check that role root is set to 'Concept mode attribute'";
				logger.error(errStr);
				throw new Exception(errStr);
			}
			final int reserved = 2;
			int cidx=reserved;
			int margin = cEditSnoCons.size() >> 2; // Add 50%
			int[] intArray = new int[cEditSnoCons.size() + margin + reserved];
			intArray[IFactory_123.TOP_CONCEPT] = IFactory_123.TOP;
			intArray[IFactory_123.BOTTOM_CONCEPT] = IFactory_123.BOTTOM;

			Collections.sort(cEditSnoCons);
			if (cEditSnoCons.get(0).id <= Integer.MIN_VALUE + reserved) {
				throw new Exception("::: SNOROCKET: TOP & BOTTOM nids NOT reserved");
			}
			for (Concept sc : cEditSnoCons) {
				intArray[cidx++] = sc.id;
			}
			// Fill array to make binary search work correctly.
			Arrays.fill(intArray, cidx, intArray.length, Integer.MAX_VALUE);
			int root=conStrList.get(I_Constants.SNOMED_ROOT_CONCEPT);
			Snorocket_123 rocket_123 = new Snorocket_123(intArray, cidx, roles, ridx,
					root);

			// SnomedMetadata :: ISA
			isa=conStrList.get(GetDescendants.ISA_SCTID);
			rocket_123.setIsaNid(isa);

			// SnomedMetadata :: ROLE_ROOTS
			rocket_123.setRoleRoot(isa, true); // @@@
			int roleRoot=conStrList.get(I_Constants.ATTRIBUTE_ROOT_CONCEPT);
			rocket_123.setRoleRoot(roleRoot, false);

			// SET DEFINED CONCEPTS
			for (int i = 0; i < cEditSnoCons.size(); i++) {
				if (cEditSnoCons.get(i).isDefined) {
					rocket_123.setConceptIdxAsDefined(i + reserved);
				}
			}
			cEditSnoCons = null; // :MEMORY:

			loadRelationshipFilesTomap(statedRelationships);
			// ADD RELATIONSHIPS
			Collections.sort(cEditRelationships);
			for (Relationship sr : cEditRelationships) {
				int err = rocket_123.addRelationship(sr.sourceId, sr.typeId, sr.destinationId, sr.group);
				if (err > 0) {
					StringBuilder sb = new StringBuilder();
					if ((err & 1) == 1) {
						sb.append(" --UNDEFINED_C1-- ");
					}
					if ((err & 2) == 2) {
						sb.append(" --UNDEFINED_ROLE-- ");
					}
					if ((err & 4) == 4) {
						sb.append(" --UNDEFINED_C2-- ");
					}
					logger.info("\r\n::: " + sb /* :!!!: + dumpSnoRelStr(sr) */);
				}
			}

			cEditRelationships = null; // :MEMORY:

			conStrList = null; // :MEMORY:
			System.gc();

			// RUN CLASSIFIER
			long startTime = System.currentTimeMillis();
			logger.info("::: Starting Classifier... ");
			rocket_123.classify();
			logger.info("::: Time to classify (ms): " + (System.currentTimeMillis() - startTime));

			// GET CLASSIFER EQUIVALENTS
			logger.info("::: GET EQUIVALENT CONCEPTS...");
			startTime = System.currentTimeMillis();
			ProcessEquiv pe = new ProcessEquiv();
			rocket_123.getEquivalents(pe);
			logger.info("\r\n::: [SnorocketMojo] ProcessEquiv() count=" + pe.countConSet
					+ " time= " + toStringLapseSec(startTime));
			pe.getEquivalentClasses();
			EquivalentClasses.writeEquivConcept(pe.getEquivalentClasses(), equivalencyReport);

			// GET CLASSIFER RESULTS
			cRocketRelationships = new ArrayList<Relationship>();
			logger.info("::: GET CLASSIFIER RESULTS...");
			startTime = System.currentTimeMillis();
			ProcessResults pr = new ProcessResults(cRocketRelationships);
			rocket_123.getDistributionFormRelationships(pr);
			logger.info("\r\n::: [SnorocketMojo] GET CLASSIFIER RESULTS count=" + pr.countRel
					+ " time= " + toStringLapseSec(startTime));

			pr = null; // :MEMORY:
			rocket_123 = null; // :MEMORY:
			System.gc();
			System.gc();

			// GET CLASSIFIER_PATH RELS
			startTime = System.currentTimeMillis();
			cEditRelationships = new ArrayList<Relationship>();

			cEditSnoCons = new ArrayList<StringIDConcept>();
			conRefList=new HashMap<Integer,String>();
			conStrList=new HashMap<String,Integer>();
			loadConceptFilesTomap(concepts,true);
			cEditSnoCons=null;
			if (previousInferredRelationships != null && !previousInferredRelationships.isEmpty()){
				loadRelationshipFilesTomap(previousInferredRelationships);
			}
			conStrList=null;
			// FILTER RELATIONSHIPS
			//			int last = cEditRelationships.size();
			//			for (int idx = last - 1; idx > -1; idx--) {
			//				if (Arrays.binarySearch(intArray, cEditRelationships.get(idx).destinationId) < 0) {
			//					cEditRelationships.remove(idx);
			//				}
			//			}


			// WRITEBACK RESULTS
			startTime = System.currentTimeMillis();
			if (previousInferredRelationships == null || !previousInferredRelationships.isEmpty()) {
				writeInferredRel(cRocketRelationships);
			} else {

				logger.info(compareAndWriteBack(cEditRelationships, cRocketRelationships));

				logger.info("\r\n::: *** WRITEBACK *** LAPSED TIME =\t" + toStringLapseSec(startTime) + "\t ***");

				consolidateRels();

			}

			logger.info("\r\n::: *** WROTE *** LAPSED TIME =\t" + toStringLapseSec(startTime) + "\t ***");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Consolidate rels.
	 *
	 * @throws Exception the exception
	 */
	private void consolidateRels() throws Exception {

		FileOutputStream fos = new FileOutputStream( newInferredRelationships);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		FileInputStream rfis = new FileInputStream(tempRelationshipStore);
		InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
		BufferedReader rbr = new BufferedReader(risr);

		String line;
		while((line=rbr.readLine())!=null){
			bw.append(line);
			bw.append("\r\n");
		}
		rbr.close();
		rbr=null;
		rfis=null;
		risr=null;

		String[] spl;
		for (String relFile:previousInferredRelationships){

			rfis = new FileInputStream(relFile);
			risr = new InputStreamReader(rfis,"UTF-8");
			rbr = new BufferedReader(risr);
			rbr.readLine();
			while((line=rbr.readLine())!=null){

				spl=line.split("\t",-1);
				if (retiredSet.contains(spl[0])){
					continue;
				}
				bw.append(line);
				bw.append("\r\n");
			}
			rbr.close();
			rbr=null;
			rfis=null;
			risr=null;
		}
		bw.close();
		tempRelationshipStore.delete();
	}

	/**
	 * To string lapse sec.
	 *
	 * @param startTime the start time
	 * @return the string
	 */
	private  String toStringLapseSec(long startTime) {
		StringBuilder s = new StringBuilder();
		long stopTime = System.currentTimeMillis();
		long lapseTime = stopTime - startTime;
		s.append((float) lapseTime / 1000).append(" (seconds)");
		return s.toString();
	}

	/**
	 * Gets the roles.
	 *
	 * @param parentConcepts the parent concepts
	 * @return the roles
	 * @throws Exception the exception
	 */
	private  int[] getRoles(HashSet<String> parentConcepts) throws Exception {
		HashSet<String> roles=new HashSet<String>();
		for (String statedRel:statedRelationships){
			File relationshipFile=new File(statedRel);
			GetDescendants getDesc=new GetDescendants(parentConcepts, relationshipFile, null);
			getDesc.execute();
			roles.addAll(getDesc.getDescendants());
			getDesc=null;
		}
		roles.add(GetDescendants.ISA_SCTID);
		int[] result=new int[roles.size()];
		int resIdx=0;
		for (String role:roles){
			result[resIdx]=conStrList.get(role);
			resIdx++;
		}
		roles=null;
		Arrays.sort(result);
		return result;
	}

	/**
	 * Load concept files to map.
	 *
	 * @param concepts the concept file
	 * @param mapToModule the map to module
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public  void loadConceptFilesTomap(List<String> concepts, boolean mapToModule) throws IOException {

		if (mapToModule){
			conceptModule=new HashMap<Integer,String>();
		}
		int cont=Integer.MIN_VALUE + 3;

		String line;
		String[] spl;
		boolean definitionStatusId ;

		for (String concept:concepts){
			FileInputStream rfis = new FileInputStream(concept);
			InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
			BufferedReader rbr = new BufferedReader(risr);
			rbr.readLine();
			while((line=rbr.readLine())!=null){

				spl=line.split("\t",-1);
				if (!conStrList.containsKey(spl[0]) ){
					cont++;
					conRefList.put(cont,spl[0]);
					conStrList.put(spl[0],cont);

					if (mapToModule){
						if (spl[0].equals(I_Constants.META_SCTID)){
							conceptModule.put(cont, module);
						}else{
							conceptModule.put(cont, spl[3]);
						}
					}
					if (spl[2].equals("1") ){
						definitionStatusId = (spl[4].equals(I_Constants.FULLY_DEFINED));
						StringIDConcept conStr=new StringIDConcept(cont,spl[0],definitionStatusId);
						cEditSnoCons.add(conStr);
					}
				}
			}
			rbr.close();
			rbr=null;
		}
	}

	/**
	 * Load relationship files tomap.
	 *
	 * @param relationshipFiles the relationship file
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public  void loadRelationshipFilesTomap(List<String> relationshipFiles)throws IOException {

		String line;
		String[] spl;
		for (String relFile:relationshipFiles){
			FileInputStream rfis = new FileInputStream(relFile);
			InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
			BufferedReader rbr = new BufferedReader(risr);
			rbr.readLine();

			while((line=rbr.readLine())!=null){

				spl=line.split("\t",-1);
				if (spl[2].equals("1") && (spl[8].equals(I_Constants.INFERRED)
						|| spl[8].equals(I_Constants.STATED))){
					Integer c1 = conStrList.get(spl[4]);
					Integer c2 = conStrList.get(spl[5]);
					Integer rg = Integer.parseInt(spl[6]);
					Integer ty = conStrList.get(spl[7]);

					if (c1 == null || c2 == null || rg == null || ty == null) {
						logger.error("Unexpected null value c1:" + c1 + ", c2:" + c2 + ", rg:" + rg + ", ty:" + ty + ", loading line:\"" + line + "\"");
					}

					Relationship rel = new Relationship(c1, c2, ty, rg, spl[0]);
					cEditRelationships.add(rel);
				}
			}
			rbr.close();
			rbr=null;
		}
	}

	/**
	 * The Class ProcessResults.
	 */
	private class ProcessResults implements I_Callback {

		/** The snorels. */
		private List<Relationship> snorels;

		/** The count rel. */
		private int countRel = 0; // STATISTICS COUNTER

		/**
		 * Instantiates a new process results.
		 *
		 * @param snorels the snorels
		 */
		public ProcessResults(List<Relationship> snorels) {
			this.snorels = snorels;
			this.countRel = 0;
		}

		/* (non-Javadoc)
		 * @see au.csiro.snorocket.snapi.I_Snorocket_123.I_Callback#addRelationship(int, int, int, int)
		 */
		public void addRelationship(int conceptId1, int roleId, int conceptId2, int group) {
			countRel++;
			Relationship relationship = new Relationship(conceptId1, conceptId2, roleId, group);
			snorels.add(relationship);
			if (countRel % 25000 == 0) {
				// ** GUI: ProcessResults
				logger.info("rels processed " + countRel);
			}
		}
	}

	/**
	 * The Class ProcessEquiv.
	 */
	private class ProcessEquiv implements I_EquivalentCallback {

		/** The count con set. */
		private int countConSet = 0; // STATISTICS COUNTER

		/** The equiv concept. */
		private EquivalentClasses equivalentClasses;

		/**
		 * Instantiates a new process equiv.
		 */
		public ProcessEquiv() {
			equivalentClasses =new EquivalentClasses();
		}

		/* (non-Javadoc)
		 * @see au.csiro.snorocket.snapi.I_Snorocket_123.I_EquivalentCallback#equivalent(java.util.ArrayList)
		 */
		public void equivalent(ArrayList<Integer> equivalentConcepts) {
			equivalentClasses.add(new ConceptGroup(equivalentConcepts));
			countConSet += 1;
		}

		/**
		 * Gets the equiv concept.
		 *
		 * @return the equiv concept
		 */
		public EquivalentClasses getEquivalentClasses() {
			return equivalentClasses;
		}
	}

	/**
	 * Write inferred rel.
	 *
	 * @param infRels the inf rels
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private  void writeInferredRel( List<Relationship> infRels)
			throws IOException {

		// STATISTICS COUNTERS
		int countConSeen = 0;

		FileOutputStream fos = new FileOutputStream( newInferredRelationships);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		bw.append("id");
		bw.append("\t");
		bw.append("effectiveTime");
		bw.append("\t");
		bw.append("active");
		bw.append("\t");
		bw.append("moduleId");
		bw.append("\t");
		bw.append("sourceId");
		bw.append("\t");
		bw.append("destinationId");
		bw.append("\t");
		bw.append("relationshipGroup");
		bw.append("\t");
		bw.append("typeId");
		bw.append("\t");
		bw.append("characteristicTypeId");
		bw.append("\t");
		bw.append("modifierId");
		bw.append("\r\n");

		Collections.sort(infRels);

		// Typically, B is the SnoRocket Results Set (for newly inferred)
		Iterator<Relationship> itRel = infRels.iterator();

		Relationship infRel = null;
		boolean done = false;
		if (itRel.hasNext()) {
			infRel = itRel.next();
		} else {
			done = true;
		}

		// BY SORT ORDER, LOWER NUMBER ADVANCES FIRST

		while ( !done) {
			if (++countConSeen % 25000 == 0) {
				logger.info("::: [Snorocket] write inferred rels @ #\t" + countConSeen);
			}
			writeRel(bw,infRel);

			if (itRel.hasNext()) {
				infRel = itRel.next();
			} else {
				done = true;
			}

		}
		bw.close();
		bw=null;
		osw=null;
		fos=null;
	}

	/**
	 * Write rel.
	 *
	 * @param bw the bw
	 * @param infRel the inf rel
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private  void writeRel(BufferedWriter bw,Relationship infRel)
			throws  IOException {
		String moduleC1=conceptModule.get(infRel.sourceId);
		if (moduleC1==null){
			moduleC1=module;
		}
		writeRF2TypeLine(bw,"null",releaseDate,"1",moduleC1,conRefList.get(infRel.sourceId),
				conRefList.get(infRel.destinationId),infRel.group,conRefList.get(infRel.typeId),
				I_Constants.INFERRED, I_Constants.SOMEMODIFIER);

	}

	/**
	 * Write r f2 type line.
	 *
	 * @param bw the bw
	 * @param relationshipId the relationship id
	 * @param effectiveTime the effective time
	 * @param active the active
	 * @param moduleId the module id
	 * @param sourceId the source id
	 * @param destinationId the destination id
	 * @param relationshipGroup the relationship group
	 * @param relTypeId the rel type id
	 * @param characteristicTypeId the characteristic type id
	 * @param modifierId the modifier id
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public  void writeRF2TypeLine(BufferedWriter bw, String relationshipId, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, int relationshipGroup, String relTypeId,
			String characteristicTypeId, String modifierId) throws IOException {
		bw.append( relationshipId + "\t" + effectiveTime + "\t" + active + "\t" + moduleId + "\t" + sourceId + "\t" + destinationId + "\t" + relationshipGroup + "\t" + relTypeId
				+ "\t" + characteristicTypeId + "\t" + modifierId);
		bw.append( "\r\n");
	}

	/**
	 * Compare and write back.
	 *
	 * @param snorelA the snorel a
	 * @param snorelB the snorel b
	 * @return the string
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private  String compareAndWriteBack(List<Relationship> snorelA, List<Relationship> snorelB)
			throws  IOException {

		retiredSet=new HashSet<String>();
		// STATISTICS COUNTERS
		int countConSeen = 0;
		int countSame = 0;
		int countSameISA = 0;
		int countA_Diff = 0;
		int countA_DiffISA = 0;
		int countA_Total = 0;
		int countB_Diff = 0;
		int countB_DiffISA = 0;
		int countB_Total = 0;
		FileOutputStream fos = new FileOutputStream( tempRelationshipStore);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		bw.append("id");
		bw.append("\t");
		bw.append("effectiveTime");
		bw.append("\t");
		bw.append("active");
		bw.append("\t");
		bw.append("moduleId");
		bw.append("\t");
		bw.append("sourceId");
		bw.append("\t");
		bw.append("destinationId");
		bw.append("\t");
		bw.append("relationshipGroup");
		bw.append("\t");
		bw.append("typeId");
		bw.append("\t");
		bw.append("characteristicTypeId");
		bw.append("\t");
		bw.append("modifierId");
		bw.append("\r\n");


		long startTime = System.currentTimeMillis();
		Collections.sort(snorelA);
		Collections.sort(snorelB);

		// Typically, A is the Classifier Path (for previously inferred)
		// Typically, B is the SnoRocket Results Set (for newly inferred)
		Iterator<Relationship> itA = snorelA.iterator();
		Iterator<Relationship> itB = snorelB.iterator();
		Relationship rel_A = null;
		boolean done_A = false;
		if (itA.hasNext()) {
			rel_A = itA.next();
		} else {
			done_A = true;
		}
		Relationship rel_B = null;
		boolean done_B = false;
		if (itB.hasNext()) {
			rel_B = itB.next();
		} else {
			done_B = true;
		}

		logger.info("\r\n::: [SnorocketMojo]"
				+ "\r\n::: snorelA.size() = \t" + snorelA.size()
				+ "\r\n::: snorelB.size() = \t" + snorelB.size());

		// BY SORT ORDER, LOWER NUMBER ADVANCES FIRST
		while (!done_A && !done_B) {
			if (++countConSeen % 25000 == 0) {
				logger.info("::: [SnorocketMojo] compareAndWriteBack @ #\t" + countConSeen);
			}

			if (rel_A.sourceId == rel_B.sourceId) {
				// COMPLETELY PROCESS ALL C1 FOR BOTH IN & OUT
				// PROCESS C1 WITH GROUP == 0
				int thisC1 = rel_A.sourceId;

				// PROCESS WHILE BOTH HAVE GROUP 0
				while (rel_A.sourceId == thisC1 && rel_B.sourceId == thisC1 && rel_A.group == 0
						&& rel_B.group == 0 && !done_A && !done_B) {

					// PROGESS GROUP ZERO
					switch (compareSnoRel(rel_A, rel_B)) {
					case 1: // SAME
						// GATHER STATISTICS
						countA_Total++;
						countB_Total++;
						countSame++;
						// NOTHING TO WRITE IN THIS CASE
						if (rel_A.typeId == isa) {
							countSameISA++;
						}
						if (itA.hasNext()) {
							rel_A = itA.next();
						} else {
							done_A = true;
						}
						if (itB.hasNext()) {
							rel_B = itB.next();
						} else {
							done_B = true;
						}
						break;

					case 2: // REL_A > REL_B -- B has extra stuff
						// WRITEBACK REL_B (Classifier Results) AS CURRENT
						countB_Diff++;
						countB_Total++;
						if (rel_B.typeId == isa) {
							countB_DiffISA++;
						}
						writeRel(bw,rel_B);

						if (itB.hasNext()) {
							rel_B = itB.next();
						} else {
							done_B = true;
						}
						break;

					case 3: // REL_A < REL_B -- A has extra stuff
						// WRITEBACK REL_A (Classifier Input) AS RETIRED
						// GATHER STATISTICS
						countA_Diff++;
						countA_Total++;
						if (rel_A.typeId == isa) {
							countA_DiffISA++;
						}
						writeBackRetired(bw,rel_A);

						if (itA.hasNext()) {
							rel_A = itA.next();
						} else {
							done_A = true;
						}
						break;
					} // switch
				}

				// REMAINDER LIST_A GROUP 0 FOR C1
				while (rel_A.sourceId == thisC1 && rel_A.group == 0 && !done_A) {

					countA_Diff++;
					countA_Total++;
					if (rel_A.typeId == isa) {
						countA_DiffISA++;
					}
					writeBackRetired(bw,rel_A);
					if (itA.hasNext()) {
						rel_A = itA.next();
					} else {
						done_A = true;
						break;
					}
				}

				// REMAINDER LIST_B GROUP 0 FOR C1
				while (rel_B.sourceId == thisC1 && rel_B.group == 0 && !done_B) {
					countB_Diff++;
					countB_Total++;
					if (rel_B.typeId == isa) {
						countB_DiffISA++;
					}
					writeRel(bw,rel_B);
					if (itB.hasNext()) {
						rel_B = itB.next();
					} else {
						done_B = true;
						break;
					}
				}

				// ** SEGMENT GROUPS **
				RelationshipGroupList groupList_A = new RelationshipGroupList();
				RelationshipGroupList groupList_B = new RelationshipGroupList();
				RelationshipGroup groupA = null;
				RelationshipGroup groupB = null;

				// SEGMENT GROUPS IN LIST_A
				int prevGroup = Integer.MIN_VALUE;
				while (rel_A.sourceId == thisC1 && !done_A) {
					if (rel_A.group != prevGroup) {
						groupA = new RelationshipGroup();
						groupList_A.add(groupA);
					}

					groupA.add(rel_A);

					prevGroup = rel_A.group;
					if (itA.hasNext()) {
						rel_A = itA.next();
					} else {
						done_A = true;
					}
				}
				// SEGMENT GROUPS IN LIST_B
				prevGroup = Integer.MIN_VALUE;
				while (rel_B.sourceId == thisC1 && !done_B) {
					if (rel_B.group != prevGroup) {
						groupB = new RelationshipGroup();
						groupList_B.add(groupB);
					}

					groupB.add(rel_B);

					prevGroup = rel_B.group;
					if (itB.hasNext()) {
						rel_B = itB.next();
					} else {
						done_B = true;
					}
				}

				// FIND GROUPS IN GROUPLIST_A WITHOUT AN EQUAL IN GROUPLIST_B
				// WRITE THESE GROUPED RELS AS "RETIRED"
				RelationshipGroupList groupList_NotEqual;
				if (groupList_A.size() > 0) {
					groupList_NotEqual = groupList_A.whichNotEqual(groupList_B);
					for (RelationshipGroup sg : groupList_NotEqual) {
						for (Relationship sr_A : sg) {
							writeBackRetired(bw,sr_A);
						}
					}
					countA_Total += groupList_A.countRels();
					countA_Diff += groupList_NotEqual.countRels();
				}

				// FIND GROUPS IN GROUPLIST_B WITHOUT AN EQUAL IN GROUPLIST_A
				// WRITE THESE GROUPED RELS AS "NEW, CURRENT"
				int rgNum = 0; // USED TO DETERMINE "AVAILABLE" ROLE GROUP NUMBERS
				if (groupList_B.size() > 0) {
					groupList_NotEqual = groupList_B.whichNotEqual(groupList_A);
					for (RelationshipGroup sg : groupList_NotEqual) {
						if (sg.get(0).group != 0) {
							rgNum = nextRoleGroupNumber(groupList_A, rgNum);
							for (Relationship sr_B : sg) {
								sr_B.group = rgNum;
								writeRel(bw,sr_B);
							}
						} else {
							for (Relationship sr_B : sg) {
								writeRel(bw,sr_B);
							}
						}
					}
					countB_Total += groupList_A.countRels();
					countB_Diff += groupList_NotEqual.countRels();
				}
			} else if (rel_A.sourceId > rel_B.sourceId) {
				// CASE 2: LIST_B HAS CONCEPT NOT IN LIST_A
				// COMPLETELY *ADD* ALL THIS C1 FOR REL_B AS NEW, CURRENT
				int thisC1 = rel_B.sourceId;
				while (rel_B.sourceId == thisC1) {
					countB_Diff++;
					countB_Total++;
					if (rel_B.typeId == isa) {
						countB_DiffISA++;
					}
					writeRel(bw,rel_B);
					if (itB.hasNext()) {
						rel_B = itB.next();
					} else {
						done_B = true;
						break;
					}
				}

			} else {
				// CASE 3: LIST_A HAS CONCEPT NOT IN LIST_B
				// COMPLETELY *RETIRE* ALL THIS C1 FOR REL_A
				int thisC1 = rel_A.sourceId;
				while (rel_A.sourceId == thisC1) {
					countA_Diff++;
					countA_Total++;
					if (rel_A.typeId == isa) {
						countA_DiffISA++;
					}
					writeBackRetired(bw,rel_A);
					if (itA.hasNext()) {
						rel_A = itA.next();
					} else {
						done_A = true;
						break;
					}
				}
			}
		}

		// AT THIS POINT, THE PREVIOUS C1 HAS BE PROCESSED COMPLETELY
		// AND, EITHER REL_A OR REL_B HAS BEEN COMPLETELY PROCESSED
		// AND, ANY REMAINDER IS ONLY ON REL_LIST_A OR ONLY ON REL_LIST_B
		// AND, THAT REMAINDER HAS A "STANDALONE" C1 VALUE
		// THEREFORE THAT REMAINDER WRITEBACK COMPLETELY
		// AS "NEW CURRENT" OR "OLD RETIRED"
		//
		// LASTLY, IF .NOT.DONE_A THEN THE NEXT REL_A IN ALREADY IN PLACE
		while (!done_A) {
			countA_Diff++;
			countA_Total++;
			if (rel_A.typeId == isa) {
				countA_DiffISA++;
			}
			// COMPLETELY UPDATE ALL REMAINING REL_A AS RETIRED
			writeBackRetired(bw,rel_A);
			if (itA.hasNext()) {
				rel_A = itA.next();
			} else {
				done_A = true;
				break;
			}
		}

		while (!done_B) {
			countB_Diff++;
			countB_Total++;
			if (rel_B.typeId == isa) {
				countB_DiffISA++;
			}
			// COMPLETELY UPDATE ALL REMAINING REL_B AS NEW, CURRENT
			writeRel(bw,rel_B);
			if (itB.hasNext()) {
				rel_B = itB.next();
			} else {
				done_B = true;
				break;
			}
		}

		bw.close();
		bw=null;
		osw=null;
		fos=null;
		// CHECKPOINT DATABASE

		StringBuilder s = new StringBuilder();
		s.append("\r\n::: [Snorocket] compareAndWriteBack()");
		long lapseTime = System.currentTimeMillis() - startTime;
		s.append("\r\n::: [Time] Sort/Compare Input & Output: \t").append(lapseTime);
		s.append("\t(mS)\t").append(((float) lapseTime / 1000) / 60).append("\t(min)");
		s.append("\r\n");
		s.append("\r\n::: ");
		s.append("\r\n::: countSame:     \t").append(countSame);
		s.append("\r\n::: countSameISA:  \t").append(countSameISA);
		s.append("\r\n::: A == Classifier Output Path");
		s.append("\r\n::: countA_Diff:   \t").append(countA_Diff);
		s.append("\r\n::: countA_DiffISA:\t").append(countA_DiffISA);
		s.append("\r\n::: countA_Total:  \t").append(countA_Total);
		s.append("\r\n::: B == Classifier Solution Set");
		s.append("\r\n::: countB_Diff:   \t").append(countB_Diff);
		s.append("\r\n::: countB_DiffISA:\t").append(countB_DiffISA);
		s.append("\r\n::: countB_Total:  \t").append(countB_Total);
		s.append("\r\n::: ");

		return s.toString();
	}

	/**
	 * Write back retired.
	 *
	 * @param bw the bw
	 * @param rel_A the rel_ a
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private  void writeBackRetired(BufferedWriter bw,Relationship rel_A)
			throws IOException {

		retiredSet.add(rel_A.getRelId());

		String moduleC1=conceptModule.get(rel_A.sourceId);
		if (moduleC1==null){
			moduleC1=module;
		}
		writeRF2TypeLine(bw,rel_A.getRelId(),releaseDate,"0",moduleC1,conRefList.get(rel_A.sourceId),
				conRefList.get(rel_A.destinationId),rel_A.group,conRefList.get(rel_A.typeId),
				I_Constants.INFERRED, I_Constants.SOMEMODIFIER);


	}

	/**
	 * Compare sno rel.
	 *
	 * @param inR the in r
	 * @param outR the out r
	 * @return the int
	 */
	private static int compareSnoRel(Relationship inR, Relationship outR) {
		if ((inR.sourceId == outR.sourceId) && (inR.group == outR.group) && (inR.typeId == outR.typeId)
				&& (inR.destinationId == outR.destinationId)) {
			return 1; // SAME
		} else if (inR.sourceId > outR.sourceId) {
			return 2; // ADDED
		} else if ((inR.sourceId == outR.sourceId) && (inR.group > outR.group)) {
			return 2; // ADDED
		} else if ((inR.sourceId == outR.sourceId) && (inR.group == outR.group)
				&& (inR.typeId > outR.typeId)) {
			return 2; // ADDED
		} else if ((inR.sourceId == outR.sourceId) && (inR.group == outR.group)
				&& (inR.typeId == outR.typeId) && (inR.destinationId > outR.destinationId)) {
			return 2; // ADDED
		} else {
			return 3; // DROPPED
		}
	} // compareSnoRel

	/**
	 * Next role group number.
	 *
	 * @param sgl the sgl
	 * @param gnum the gnum
	 * @return the int
	 */
	private static int nextRoleGroupNumber(RelationshipGroupList sgl, int gnum) {

		int testNum = gnum + 1;
		int sglSize = sgl.size();
		int trial = 0;
		while (trial <= sglSize) {

			boolean exists = false;
			for (int i = 0; i < sglSize; i++) {
				if (sgl.get(i).get(0).group == testNum) {
					exists = true;
				}
			}

			if (exists == false) {
				return testNum;
			} else {
				testNum++;
				trial++;
			}
		}

		return testNum;
	}

	@SuppressWarnings("unchecked")
	private void getParams() throws ConfigurationException  {

		try {
			xmlConfig=new XMLConfiguration(config);
		} catch (ConfigurationException e) {
			logger.info("ClassificationRunner - Error happened getting params file." + e.getMessage());
			throw e;
		}

		this.module = xmlConfig.getString(I_Constants.MODULEID);
		this.releaseDate=xmlConfig.getString(I_Constants.RELEASEDATE);
		this.equivalencyReport=xmlConfig.getString(I_Constants.EQUIVALENT_CONCEPTS_OUTPUT_FILE);
		this.newInferredRelationships=xmlConfig.getString(I_Constants.INFERRED_RELATIONSHIPS_OUTPUT_FILE);
		concepts= xmlConfig.getList(I_Constants.CONCEPT_SNAPSHOT_FILES);

		statedRelationships = xmlConfig.getList(I_Constants.RELATIONSHIP_SNAPSHOT_FILES);

		previousInferredRelationships = xmlConfig.getList(I_Constants.PREVIOUS_INFERRED_RELATIONSHIP_FILES);
		logger.info("Classification - Parameters:");
		logger.info("Module = " + module);
		logger.info("Release date = " + releaseDate);
		logger.info("Equivalent Concept Output file = " + equivalencyReport);
		logger.info("Previous Inferred Relationship file = " + previousInferredRelationships);
		logger.info("Inferred Relationship Output file = " + newInferredRelationships);
		logger.info("Concept files : ");
		for (String concept:concepts){
			logger.info( concept);
		}
		logger.info("Stated Relationship files : " );
		for (String relFile:statedRelationships){
			logger.info(relFile);
		}
		logger.info("Previous Relationship files : " );
		if (previousInferredRelationships!=null){
			for (String relFile:previousInferredRelationships){
				logger.info(relFile);
			}
		}
	}
}
