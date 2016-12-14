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
		List<String> conceptFiles = Arrays.asList(ClassLoader.getSystemResource("sct2_Concept_Snapshot_INT_20160731.txt").getPath().toString());
		List<String> statedRelationshipFiles = Arrays.asList(ClassLoader.getSystemResource("sct2_StatedRelationship_Snapshot_20160731.txt").getPath().toString());
		List<String> previousInferredFiles = Arrays.asList(ClassLoader.getSystemResource("sct2_Relationship_Snapshot_20160131.txt").getPath().toString());
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
		assertTrue(relationships.size() > 1);
		String expected = "6580269026\t20160731\t1\t900000000000012004\t718291003\t900000000000445007\t0\t116680003\t900000000000011006\t900000000000451002";
		assertEquals(expected,relationships.get(1));
	}

	private List<String> getRelationshipsFromResult(File classificationResult) throws FileNotFoundException, IOException {
		List<String> result = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(classificationResult))) {
			String line = null;
			while ((line=reader.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}
}
