package org.ihtsdo.classifier;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import org.ihtsdo.classifier.model.Relationship;
import org.junit.Before;
import org.junit.Test;

public class ClassificaitonRunnerTest {
	private static final String RELEASE_DATE = "20160731";
	private ClassificationRunner runner;
	private static final String coreModuleSctid = "900000000000207008";
	private static final String modelModuleSctid = "900000000000012004";
	private File equivanceReortFile = null;
	private File classificationResult = null;
	
	@Before
	public void setUp() throws IOException {
		classificationResult = File.createTempFile("classification", ".txt");
		ClassLoader classLoader = ClassificaitonRunnerTest.class.getClassLoader();
		List<String> conceptFiles = Arrays.asList(classLoader.getResource("sct2_Concept_Snapshot_INT_20160731.txt").getPath().toString());
		List<String> statedRelationshipFiles = Arrays.asList(classLoader.getResource("sct2_StatedRelationship_Snapshot_20160731.txt").getPath().toString());
		List<String> previousInferredFiles = Arrays.asList(classLoader.getResource("sct2_Relationship_Snapshot_20160131.txt").getPath().toString());
		equivanceReortFile = File.createTempFile("equivalence", ".txt");
		runner = new ClassificationRunner(coreModuleSctid, RELEASE_DATE, conceptFiles, statedRelationshipFiles, previousInferredFiles,
				classificationResult.getAbsolutePath(), equivanceReortFile.getAbsolutePath());
		
		System.out.println("Classfication result:" + classificationResult.getAbsolutePath());
	}
	
	@Test
	public void testUpdateModuleId() throws IOException {
		runner.conRefList = new HashMap<Integer, String>();
		runner.conStrList = new HashMap<String, Integer>();
		runner.conRefList.put(1, "718291003");
		runner.conRefList.put(2, "900000000000445007");
		runner.conRefList.put(3, "116680003");
		runner.conStrList.put("718291003", 1);
		runner.conStrList.put("900000000000445007", 2);
		runner.conStrList.put("116680003", 3);
		List<Relationship> snorelA = Arrays.asList( new Relationship(1, 2, 3, 0,coreModuleSctid,"6580269026"));
		List<Relationship> snorelB = Arrays.asList(new Relationship(1, 2, 3, 0, modelModuleSctid));
		String result = runner.compareAndWriteBack(snorelA, snorelB);
		System.out.println(result);
		assertTrue(equivanceReortFile.length() == 0);
		runner.consolidateRels();
		List<String> relationships = getRelationshipsFromResult(classificationResult);
		assertTrue(relationships.size() == 1);
		String expected = "6580269026\t20160731\t1\t900000000000012004\t718291003\t900000000000445007\t0\t116680003\t900000000000011006\t900000000000451002";
		assertEquals(expected,relationships.get(0));
	}
	
	
	
	@Test
	public void testRoleGroupChange() throws IOException {
		runner.conRefList = new HashMap<Integer, String>();
		runner.conStrList = new HashMap<String, Integer>();
		runner.conRefList.put(1, "718291003");
		runner.conRefList.put(2, "900000000000445007");
		runner.conRefList.put(3, "116680003");
		runner.conRefList.put(4, "42685002");
		runner.conRefList.put(5, "47538007");
		runner.conStrList.put("718291003", 1);
		runner.conStrList.put("900000000000445007", 2);
		runner.conStrList.put("116680003", 3);
		runner.conStrList.put("42685002", 4);
		runner.conStrList.put("47538007", 5);
		List<Relationship> snorelA = Arrays.asList( new Relationship(1, 2, 3, 0, coreModuleSctid),new Relationship(1, 4, 3, 1, coreModuleSctid));
		List<Relationship> snorelB = Arrays.asList( new Relationship(1, 2, 3, 0, coreModuleSctid),new Relationship(1, 5, 3, 1, coreModuleSctid));
		String result = runner.compareAndWriteBack(snorelA, snorelB);
		System.out.println(result);
		assertTrue(equivanceReortFile.length() == 0);
		runner.consolidateRels();
		List<String> relationships = getRelationshipsFromResult(classificationResult);
		assertTrue(relationships.size() == 3);
		String rel1 = "null	20160731	0	900000000000207008	718291003	42685002	1	116680003	900000000000011006	900000000000451002";
		String rel2 = "null	20160731	1	900000000000207008	718291003	47538007	2	116680003	900000000000011006	900000000000451002";
		String rel3 = "6580269026	20160131	1	900000000000207008	718291003	900000000000445007	0	116680003	900000000000011006	900000000000451002";
		assertEquals(rel1,relationships.get(0));
		assertEquals(rel2,relationships.get(1));
		assertEquals(rel3,relationships.get(2));

	}

	private List<String> getRelationshipsFromResult(File classificationResult) throws FileNotFoundException, IOException {
		List<String> result = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(classificationResult))) {
			//header line
			String line = reader.readLine();
			while ((line=reader.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}
}
