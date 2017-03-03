package org.ihtsdo.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.classifier.ClassificationRunner;
import org.junit.Before;
import org.junit.Test;


public class ClassifierRunnerIntegrationTest {

	private static final String CORE_MODULE_ID = "900000000000207008";
	
	private String effectiveTimeSnomedFormat;
	private List<String> localConceptFilePaths;
	private List<String> localStatedRelationshipFilePaths;
	private List<String> previousInferredRelationshipFilePaths;
	private File classifierInferredRelationshipResultOutputFile;
	private File equivalencyReportOutputFile;
	
	@Before
	public void setUp() {
		effectiveTimeSnomedFormat ="20170131";
		String rootDir = "/Users/mchu/Downloads/classifierTest/";
		localConceptFilePaths = new ArrayList<String>();
		localConceptFilePaths.add(rootDir + "xsct2_Concept_Snapshot_INT_20170131_modified.txt");
		localStatedRelationshipFilePaths = new ArrayList<String>();
		localStatedRelationshipFilePaths.add(rootDir + "xsct2_StatedRelationship_Snapshot_INT_20170131.txt");
		previousInferredRelationshipFilePaths = new ArrayList<String>();
		previousInferredRelationshipFilePaths.add(rootDir+"sct2_Relationship_Snapshot_INT_20160731.txt");
		classifierInferredRelationshipResultOutputFile = new File(rootDir,"classificationResult.txt");
		equivalencyReportOutputFile = new File(rootDir,"equivalency.txt");
	}

	@Test
	public void testClassification() throws Exception {
		ClassificationRunner classificationRunner = new ClassificationRunner(CORE_MODULE_ID, effectiveTimeSnomedFormat,
				localConceptFilePaths, localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths,
				classifierInferredRelationshipResultOutputFile.getAbsolutePath(), equivalencyReportOutputFile.getAbsolutePath());
		classificationRunner.execute();
	}

}