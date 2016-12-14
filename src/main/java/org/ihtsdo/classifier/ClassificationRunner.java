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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.model.Concept;
import org.ihtsdo.classifier.model.ConceptGroup;
import org.ihtsdo.classifier.model.EquivalentClasses;
import org.ihtsdo.classifier.model.Relationship;
import org.ihtsdo.classifier.model.RelationshipGroup;
import org.ihtsdo.classifier.model.RelationshipGroupList;
import org.ihtsdo.classifier.model.StringIDConcept;
import org.ihtsdo.classifier.utils.GetDescendants;
import org.ihtsdo.classifier.utils.I_Constants;

import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.snapi.I_Snorocket_123.I_Callback;
import au.csiro.snorocket.snapi.I_Snorocket_123.I_EquivalentCallback;
import au.csiro.snorocket.snapi.Snorocket_123;

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

	private static final String LINE_SEPARATOR = "\r\n";

	private static final String TAB = "\t";

	/** The prev inferred rels. */
	private List<String> previousInferredRelationships;

	/** The output tmp. */
	private File tempRelationshipStore;

	private File config;
	
	/** The edited snomed concepts. */
	private  ArrayList<StringIDConcept> cEditSnoCons;

	/** The edit snomed rels. */
	private  ArrayList<Relationship> cEditRelationships;

	/** The con ref list. */
	HashMap<Integer, String> conRefList;

	/** The con str list. */
	HashMap<String,Integer> conStrList;

	/** The logger. */
	private final  Logger logger;

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

	public ClassificationRunner() {
		logger = Logger.getLogger("org.ihtsdo.classifier.ClassificationRunner");
		isa = new Integer(GetDescendants.ISA_SCTID);
		cEditSnoCons = new ArrayList<StringIDConcept>();
		cEditRelationships = new ArrayList<Relationship>();
		conRefList=new HashMap<Integer,String>();
		conStrList=new HashMap<String,Integer>();
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
	public ClassificationRunner(final String module, final String releaseDate, final List<String> conceptFilePaths,
			final List<String> statedRelationshipFilePaths, final List<String> previousInferredRelationshipFilePaths,
			final String inferredRelationshipOutputFilePath, final String equivalencyReportOutputFilePath) {
		this();
		this.module = module;
		this.releaseDate = releaseDate;
		this.concepts = conceptFilePaths;
		this.statedRelationships = statedRelationshipFilePaths;
		this.newInferredRelationships = inferredRelationshipOutputFilePath;
		this.equivalencyReport = equivalencyReportOutputFilePath;
		this.previousInferredRelationships = previousInferredRelationshipFilePaths;

		final File outputFile=new File(newInferredRelationships);
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
	public ClassificationRunner(final String module, final String releaseDate, final String[] concepts,
			final String[] statedRelationships,final String[] previousInferredRelationships, final String newInferredRelationships, final String equivalencyReport) {
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

		final File outputFile=new File(newInferredRelationships);
		tempRelationshipStore=new File(outputFile.getParentFile(),"Tmp_" + outputFile.getName());
	}

	public ClassificationRunner(final File config) throws ConfigurationException {
		this();

		this.config=config;
		getParams();

		final File outputFile=new File(newInferredRelationships);
		tempRelationshipStore=new File(outputFile.getParentFile(),"Tmp_" + outputFile.getName());

	}

	/**
	 * Execute the classification.
	 */
	public void execute() throws IOException, ClassificationException {

			logger.info("\r\n::: [Test Snorocket] execute() -- begin");
			loadConceptFilesTomap(concepts,true);

			final HashSet<String>parentConcepts=new HashSet<String>();
			parentConcepts.add(I_Constants.ATTRIBUTE_ROOT_CONCEPT); //concept model attribute

			final int[] roles =getRoles(parentConcepts); 
			final int ridx = roles.length;
			if (roles.length > 100) {
				final String errStr = "Role types exceeds 100. This will cause a memory issue. "
						+ "Please check that role root is set to 'Concept mode attribute'";
				logger.error(errStr);
				throw new ClassificationException(errStr);
			}
			final int reserved = 2;
			int cidx=reserved;
			final int margin = cEditSnoCons.size() >> 2; // Add 50%
			final int[] intArray = new int[cEditSnoCons.size() + margin + reserved];
			intArray[IFactory_123.TOP_CONCEPT] = IFactory_123.TOP;
			intArray[IFactory_123.BOTTOM_CONCEPT] = IFactory_123.BOTTOM;

			Collections.sort(cEditSnoCons);
			if (cEditSnoCons.get(0).id <= Integer.MIN_VALUE + reserved) {
				throw new ClassificationException("::: SNOROCKET: TOP & BOTTOM nids NOT reserved");
			}
			for (final Concept sc : cEditSnoCons) {
				intArray[cidx++] = sc.id;
			}
			// Fill array to make binary search work correctly.
			Arrays.fill(intArray, cidx, intArray.length, Integer.MAX_VALUE);
			final int root=conStrList.get(I_Constants.SNOMED_ROOT_CONCEPT);
			Snorocket_123 rocket_123 = new Snorocket_123(intArray, cidx, roles, ridx,
					root);

			// SnomedMetadata :: ISA
			isa=conStrList.get(GetDescendants.ISA_SCTID);
			rocket_123.setIsaNid(isa);

			// SnomedMetadata :: ROLE_ROOTS
			rocket_123.setRoleRoot(isa, true); // @@@
			final int roleRoot=conStrList.get(I_Constants.ATTRIBUTE_ROOT_CONCEPT);
			rocket_123.setRoleRoot(roleRoot, false);

			// SET DEFINED CONCEPTS
			for (int i = 0; i < cEditSnoCons.size(); i++) {
				if (cEditSnoCons.get(i).isDefined) {
					rocket_123.setConceptIdxAsDefined(i + reserved);
				}
			}
			
			cEditSnoCons = null; // :MEMORY:

			loadRelationshipFilesToMap(statedRelationships);
			// ADD RELATIONSHIPS
			Collections.sort(cEditRelationships);
			for (final Relationship sr : cEditRelationships) {
				final int err = rocket_123.addRelationship(sr.getSourceId(), sr.getTypeId(), sr.getDestinationId(), sr.getGroup());
				if (err > 0) {
					final StringBuilder sb = new StringBuilder();
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
			final ProcessEquiv pe = new ProcessEquiv();
			rocket_123.getEquivalents(pe);
			logger.info("\r\n::: [SnorocketMojo] ProcessEquiv() count=" + pe.countConSet
					+ " time= " + toStringLapseSec(startTime));
			pe.getEquivalentClasses();
			EquivalentClasses.writeEquivConcept(pe.getEquivalentClasses(), equivalencyReport);

			// GET CLASSIFER RESULTS
			/* The c rocket sno rels. */
		ArrayList<Relationship> cRocketRelationships = new ArrayList<Relationship>();
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
				loadRelationshipFilesToMap(previousInferredRelationships);
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
			if (previousInferredRelationships == null || previousInferredRelationships.isEmpty()) {
				writeInferredRel(cRocketRelationships);
			} else {

				logger.info(compareAndWriteBack(cEditRelationships, cRocketRelationships));

				logger.info("\r\n::: *** WRITEBACK *** LAPSED TIME =\t" + toStringLapseSec(startTime) + "\t ***");

				consolidateRels();

			}

			logger.info("\r\n::: *** WROTE *** LAPSED TIME =\t" + toStringLapseSec(startTime) + "\t ***");
	}

	/**
	 * Consolidate rels.
	 *
	 * @throws Exception the exception
	 */
	 void consolidateRels() throws IOException {

		final FileOutputStream fos = new FileOutputStream( newInferredRelationships);
		final OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		final BufferedWriter bw = new BufferedWriter(osw);

		FileInputStream rfis = new FileInputStream(tempRelationshipStore);
		InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
		BufferedReader rbr = new BufferedReader(risr);

		String line;
		while((line=rbr.readLine())!=null){
			bw.append(line);
			bw.append(LINE_SEPARATOR);
		}
		rbr.close();
		rbr=null;
		rfis=null;
		risr=null;

		String[] spl;
		for (final String relFile:previousInferredRelationships){

			rfis = new FileInputStream(relFile);
			risr = new InputStreamReader(rfis,"UTF-8");
			rbr = new BufferedReader(risr);
			rbr.readLine();
			while((line=rbr.readLine())!=null){

				spl=line.split(TAB,-1);
				if (retiredSet.contains(spl[0])){
					continue;
				}
				bw.append(line);
				bw.append(LINE_SEPARATOR);
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
	private  String toStringLapseSec(final long startTime) {
		final StringBuilder s = new StringBuilder();
		final long stopTime = System.currentTimeMillis();
		final long lapseTime = stopTime - startTime;
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
	private  int[] getRoles(final HashSet<String> parentConcepts) throws IOException, ClassificationException {
		HashSet<String> roles=new HashSet<String>();
		for (final String statedRel:statedRelationships){
			final File relationshipFile=new File(statedRel);
			GetDescendants getDesc=new GetDescendants(parentConcepts, relationshipFile, null);
			getDesc.execute();
			roles.addAll(getDesc.getDescendants());
			getDesc=null;
		}
		roles.add(GetDescendants.ISA_SCTID);
		final int[] result=new int[roles.size()];
		int resIdx=0;
		for (final String role:roles){
			final Integer integer = conStrList.get(role);
			if (integer != null) {
				result[resIdx] = integer;
			} else {
				throw new ClassificationException("No entry for " + role + " in conStrList.");
			}
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
	public  void loadConceptFilesTomap(final List<String> concepts, final boolean mapToModule) throws IOException {

		if (mapToModule){
			conceptModule=new HashMap<Integer,String>();
		}
		int cont=Integer.MIN_VALUE + 3;

		String line;
		String[] spl;
		boolean definitionStatusId ;

		for (final String concept:concepts){
			final FileInputStream rfis = new FileInputStream(concept);
			final InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
			BufferedReader rbr = new BufferedReader(risr);
			rbr.readLine();
			while((line=rbr.readLine())!=null){

				spl=line.split(TAB,-1);
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
						final StringIDConcept conStr=new StringIDConcept(cont,spl[0],definitionStatusId);
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
	private void loadRelationshipFilesToMap(final List<String> relationshipFiles) throws IOException, ClassificationException {

		String line;
		String[] spl;
		for (final String relFile:relationshipFiles) {
			 FileInputStream rfis = null;
			 InputStreamReader risr = null;
			 BufferedReader rbr = null;
			try {
				rfis = new FileInputStream(relFile);
				risr = new InputStreamReader(rfis,"UTF-8");
				rbr = new BufferedReader(risr);
				
				rbr.readLine();
				
					while((line=rbr.readLine())!=null){

						spl=line.split(TAB,-1);
						if (spl[2].equals("1") && (spl[8].equals(I_Constants.INFERRED)
								|| spl[8].equals(I_Constants.STATED))) {
							final Integer c1 = conStrList.get(spl[4]);
							final Integer c2 = conStrList.get(spl[5]);
							final Integer rg = Integer.parseInt(spl[6]);
							final Integer ty = conStrList.get(spl[7]);
							if (c1 == null) {
								throw new ClassificationException("Relationship source concept missing:" + spl[4]);
							} else if (c2 == null) {
								throw new ClassificationException("Relationship destinationconcept missing:" + spl[5]);
							} else if (rg == null) {
								throw new ClassificationException("Relationship role type concept missing:" + rg);
							} else if (ty == null) {
								throw new ClassificationException("Relationship group concept missing:" + spl[7]);
							} else {
								final Relationship rel = new Relationship(c1, c2, ty, rg, spl[3], spl[0]);
								cEditRelationships.add(rel);
							}
						}
					}
			} finally {
				if ( rfis != null ) {
					rfis.close();
				}
				if ( risr != null ) {
					risr.close();
				}
				if (rbr != null ) {
					rbr.close();
				}
			}
		}
	}

	/**
	 * The Class ProcessResults.
	 */
	private class ProcessResults implements I_Callback {

		/** The snorels. */
		private final List<Relationship> snorels;

		/** The count rel. */
		private int countRel = 0; // STATISTICS COUNTER

		/**
		 * Instantiates a new process results.
		 *
		 * @param snorels the snorels
		 */
		public ProcessResults(final List<Relationship> snorels) {
			this.snorels = snorels;
			this.countRel = 0;
		}

		/* (non-Javadoc)
		 * @see au.csiro.snorocket.snapi.I_Snorocket_123.I_Callback#addRelationship(int, int, int, int)
		 */
		public void addRelationship(final int conceptId1, final int roleId, final int conceptId2, final int group) {
			countRel++;
			String moduleId=conceptModule.get(conceptId1);
			if (moduleId==null){
				moduleId = module;
			}
			final Relationship relationship = new Relationship(conceptId1, conceptId2, roleId, group, moduleId);
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
		private final EquivalentClasses equivalentClasses;

		/**
		 * Instantiates a new process equiv.
		 */
		public ProcessEquiv() {
			equivalentClasses =new EquivalentClasses();
		}

		/* (non-Javadoc)
		 * @see au.csiro.snorocket.snapi.I_Snorocket_123.I_EquivalentCallback#equivalent(java.util.ArrayList)
		 */
		public void equivalent(final ArrayList<Integer> equivalentConcepts) {
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
	private  void writeInferredRel( final List<Relationship> infRels)
			throws IOException {

		// STATISTICS COUNTERS
		int countConSeen = 0;

		FileOutputStream fos = new FileOutputStream( newInferredRelationships);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		bw.append("id");
		bw.append(TAB);
		bw.append("effectiveTime");
		bw.append(TAB);
		bw.append("active");
		bw.append(TAB);
		bw.append("moduleId");
		bw.append(TAB);
		bw.append("sourceId");
		bw.append(TAB);
		bw.append("destinationId");
		bw.append(TAB);
		bw.append("relationshipGroup");
		bw.append(TAB);
		bw.append("typeId");
		bw.append(TAB);
		bw.append("characteristicTypeId");
		bw.append(TAB);
		bw.append("modifierId");
		bw.append(LINE_SEPARATOR);

		Collections.sort(infRels);

		// Typically, B is the SnoRocket Results Set (for newly inferred)
		final Iterator<Relationship> itRel = infRels.iterator();

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
	
	private  void updateRel(final BufferedWriter bw,final Relationship prevRel, final Relationship currentRel)
			throws  IOException {
		
		writeRF2TypeLine(bw,prevRel.getRelId(),releaseDate,"1",currentRel.getModuleId(),conRefList.get(currentRel.getSourceId()),
				conRefList.get(currentRel.getDestinationId()),currentRel.getGroup(),conRefList.get(currentRel.getTypeId()),
				I_Constants.INFERRED, I_Constants.SOMEMODIFIER);
		//add to the retired set so that previous version is dropped.
		retiredSet.add(prevRel.getRelId());
	}

	/**
	 * Write rel.
	 *
	 * @param bw the bw
	 * @param infRel the inf rel
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private  void writeRel(final BufferedWriter bw,final Relationship infRel)
			throws  IOException {
		String moduleC1=infRel.getModuleId();
		if (moduleC1==null){
			moduleC1=module;
		}
		writeRF2TypeLine(bw,"null",releaseDate,"1",moduleC1,conRefList.get(infRel.getSourceId()),
				conRefList.get(infRel.getDestinationId()),infRel.getGroup(),conRefList.get(infRel.getTypeId()),
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
	public  void writeRF2TypeLine(final BufferedWriter bw, final String relationshipId, final String effectiveTime, final String active, final String moduleId, final String sourceId, final String destinationId, final int relationshipGroup, final String relTypeId,
			final String characteristicTypeId, final String modifierId) throws IOException {
		bw.append(relationshipId).append(TAB).append(effectiveTime).append(TAB).append(active).append(TAB).append(moduleId).append(TAB).append(sourceId).append(TAB).append(destinationId).append(TAB).append(String.valueOf(relationshipGroup)).append(TAB).append(relTypeId).append(TAB).append(characteristicTypeId).append(TAB).append(modifierId);
		bw.append( LINE_SEPARATOR);
	}

	/**
	 * Compare and write back.
	 *
	 * @param snorelA the snorel a
	 * @param snorelB the snorel b
	 * @return the string
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	protected  String compareAndWriteBack(final List<Relationship> snorelA, final List<Relationship> snorelB)
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
		bw.append(TAB);
		bw.append("effectiveTime");
		bw.append(TAB);
		bw.append("active");
		bw.append(TAB);
		bw.append("moduleId");
		bw.append(TAB);
		bw.append("sourceId");
		bw.append(TAB);
		bw.append("destinationId");
		bw.append(TAB);
		bw.append("relationshipGroup");
		bw.append(TAB);
		bw.append("typeId");
		bw.append(TAB);
		bw.append("characteristicTypeId");
		bw.append(TAB);
		bw.append("modifierId");
		bw.append(LINE_SEPARATOR);


		final long startTime = System.currentTimeMillis();
		Collections.sort(snorelA);
		Collections.sort(snorelB);

		// Typically, A is the Classifier Path (for previously inferred)
		// Typically, B is the SnoRocket Results Set (for newly inferred)
		final Iterator<Relationship> itA = snorelA.iterator();
		final Iterator<Relationship> itB = snorelB.iterator();
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

			if (rel_A.getSourceId() == rel_B.getSourceId()) {
				// COMPLETELY PROCESS ALL C1 FOR BOTH IN & OUT
				// PROCESS C1 WITH GROUP == 0
				final int thisC1 = rel_A.getSourceId();

				// PROCESS WHILE BOTH HAVE GROUP 0
				while (rel_A.getSourceId() == thisC1 && rel_B.getSourceId() == thisC1 && rel_A.getGroup() == 0
						&& rel_B.getGroup() == 0 && !done_A && !done_B) {

					// PROGESS GROUP ZERO
					switch (compareSnoRel(rel_A, rel_B)) {
					case 1: // SAME
						// GATHER STATISTICS
						countA_Total++;
						countB_Total++;
						countSame++;
						//check whether module id is changed
						if (!rel_A.getModuleId().equals(rel_B.getModuleId())) {
							updateRel(bw, rel_A, rel_B);
						} 
						if (rel_A.getTypeId() == isa) {
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
						if (rel_B.getTypeId() == isa) {
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
						if (rel_A.getTypeId() == isa) {
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
				while (rel_A.getSourceId() == thisC1 && rel_A.getGroup() == 0 && !done_A) {

					countA_Diff++;
					countA_Total++;
					if (rel_A.getTypeId() == isa) {
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
				while (rel_B.getSourceId() == thisC1 && rel_B.getGroup() == 0 && !done_B) {
					countB_Diff++;
					countB_Total++;
					if (rel_B.getTypeId() == isa) {
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
				final RelationshipGroupList groupList_A = new RelationshipGroupList();
				final RelationshipGroupList groupList_B = new RelationshipGroupList();
				RelationshipGroup groupA = null;
				RelationshipGroup groupB = null;

				// SEGMENT GROUPS IN LIST_A
				int prevGroup = Integer.MIN_VALUE;
				while (rel_A.getSourceId() == thisC1 && !done_A) {
					if (rel_A.getGroup() != prevGroup) {
						groupA = new RelationshipGroup();
						groupList_A.add(groupA);
					}

					groupA.add(rel_A);

					prevGroup = rel_A.getGroup();
					if (itA.hasNext()) {
						rel_A = itA.next();
					} else {
						done_A = true;
					}
				}
				// SEGMENT GROUPS IN LIST_B
				prevGroup = Integer.MIN_VALUE;
				while (rel_B.getSourceId() == thisC1 && !done_B) {
					if (rel_B.getGroup() != prevGroup) {
						groupB = new RelationshipGroup();
						groupList_B.add(groupB);
					}

					groupB.add(rel_B);

					prevGroup = rel_B.getGroup();
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
					for (final RelationshipGroup sg : groupList_NotEqual) {
						for (final Relationship sr_A : sg) {
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
					for (final RelationshipGroup sg : groupList_NotEqual) {
						if (sg.get(0).getGroup() != 0) {
							rgNum = nextRoleGroupNumber(groupList_A, rgNum);
							for (final Relationship sr_B : sg) {
								sr_B.setGroup(rgNum);
								writeRel(bw,sr_B);
							}
						} else {
							for (final Relationship sr_B : sg) {
								writeRel(bw,sr_B);
							}
						}
					}
					countB_Total += groupList_A.countRels();
					countB_Diff += groupList_NotEqual.countRels();
				}
			} else if (rel_A.getSourceId() > rel_B.getSourceId()) {
				// CASE 2: LIST_B HAS CONCEPT NOT IN LIST_A
				// COMPLETELY *ADD* ALL THIS C1 FOR REL_B AS NEW, CURRENT
				final int thisC1 = rel_B.getSourceId();
				while (rel_B.getSourceId() == thisC1) {
					countB_Diff++;
					countB_Total++;
					if (rel_B.getTypeId() == isa) {
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
				final int thisC1 = rel_A.getSourceId();
				while (rel_A.getSourceId() == thisC1) {
					countA_Diff++;
					countA_Total++;
					if (rel_A.getTypeId() == isa) {
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
			if (rel_A.getTypeId() == isa) {
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
			if (rel_B.getTypeId() == isa) {
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

		final StringBuilder s = new StringBuilder();
		s.append("\r\n::: [Snorocket] compareAndWriteBack()");
		final long lapseTime = System.currentTimeMillis() - startTime;
		s.append("\r\n::: [Time] Sort/Compare Input & Output: \t").append(lapseTime);
		s.append("\t(mS)\t").append(((float) lapseTime / 1000) / 60).append("\t(min)");
		s.append(LINE_SEPARATOR);
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
	private  void writeBackRetired(final BufferedWriter bw,final Relationship rel_A)
			throws IOException {
		retiredSet.add(rel_A.getRelId());
		String moduleC1= rel_A.getModuleId();
		if (moduleC1==null){
			moduleC1=conceptModule.get(rel_A.getSourceId());
		}
		writeRF2TypeLine(bw,rel_A.getRelId(),releaseDate,"0",moduleC1,conRefList.get(rel_A.getSourceId()),
				conRefList.get(rel_A.getDestinationId()),rel_A.getGroup(),conRefList.get(rel_A.getTypeId()),
				I_Constants.INFERRED, I_Constants.SOMEMODIFIER);
	}

	/**
	 * Compare sno rel.
	 *
	 * @param inR the in r
	 * @param outR the out r
	 * @return the int
	 */
	private static int compareSnoRel(final Relationship inR, final Relationship outR) {
		if ((inR.getSourceId() == outR.getSourceId()) && (inR.getGroup() == outR.getGroup()) && (inR.getTypeId() == outR.getTypeId())
				&& (inR.getDestinationId() == outR.getDestinationId())) {
			return 1; // SAME
		} else if (inR.getSourceId() > outR.getSourceId()) {
			return 2; // ADDED
		} else if ((inR.getSourceId() == outR.getSourceId()) && (inR.getGroup() > outR.getGroup())) {
			return 2; // ADDED
		} else if ((inR.getSourceId() == outR.getSourceId()) && (inR.getGroup() == outR.getGroup())
				&& (inR.getTypeId() > outR.getTypeId())) {
			return 2; // ADDED
		} else if ((inR.getSourceId() == outR.getSourceId()) && (inR.getGroup() == outR.getGroup())
				&& (inR.getTypeId() == outR.getTypeId()) && (inR.getDestinationId() > outR.getDestinationId())) {
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
	private static int nextRoleGroupNumber(final RelationshipGroupList sgl, final int gnum) {

		int testNum = gnum + 1;
		final int sglSize = sgl.size();
		int trial = 0;
		while (trial <= sglSize) {

			boolean exists = false;
			for (int i = 0; i < sglSize; i++) {
				if (sgl.get(i).get(0).getGroup() == testNum) {
					exists = true;
				}
			}

			if (!exists) {
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

		XMLConfiguration xmlConfig;
		try {
			xmlConfig =new XMLConfiguration(config);
		} catch (final ConfigurationException e) {
			logger.info("ClassificationRunner - Error happened getting params file." + e.getMessage());
			throw e;
		}

		this.module = xmlConfig.getString(I_Constants.MODULEID);
		this.releaseDate= xmlConfig.getString(I_Constants.RELEASEDATE);
		this.equivalencyReport= xmlConfig.getString(I_Constants.EQUIVALENT_CONCEPTS_OUTPUT_FILE);
		this.newInferredRelationships= xmlConfig.getString(I_Constants.INFERRED_RELATIONSHIPS_OUTPUT_FILE);
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
		for (final String concept:concepts){
			logger.info( concept);
		}
		logger.info("Stated Relationship files : " );
		for (final String relFile:statedRelationships){
			logger.info(relFile);
		}
		logger.info("Previous Relationship files : " );
		if (previousInferredRelationships!=null){
			for (final String relFile:previousInferredRelationships){
				logger.info(relFile);
			}
		}
	}
}
