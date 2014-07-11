package org.ihtsdo.classifier;

import java.io.File;

import org.apache.log4j.Logger;


public class ClassifierManager {

	private static Logger logger;

	public static void main(String[] args){

		logger = Logger.getLogger("org.ihtsdo.classifier.ClassifierManager");
		try {
			if (args==null || args.length>3){
				logger.info("Error happened getting params. Params file doesn't exist");
				System.exit(0);
			}
			File file=null ;
			for (String arg : args){
				if (!arg.equals("-CC") && !arg.equals("-CR")){
					file =new File(arg);
					if (!file.exists()){
						logger.info("Error happened getting params. Params file doesn't exist");
						System.exit(0);
					}
				}
			}
			if (file==null){
				logger.info("Error happened getting params. Params file doesn't exist");
				System.exit(0);
			}
			boolean classified=true;
			for (String arg : args){
				if (arg.equals("-CC")){
					CycleCheck cc=new CycleCheck(file);
					if (cc.cycleDetected()){
						classified=false;
					}
					cc=null;
					
				}
			}
			if (classified){
				for (String arg : args){
					if (arg.equals("-CR")){
						ClassificationRunner cc=new ClassificationRunner(file);
						cc.execute();
						cc=null;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
		System.exit(0);
	}
}
