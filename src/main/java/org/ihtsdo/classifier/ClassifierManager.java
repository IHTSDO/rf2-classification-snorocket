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

import java.io.File;

import org.apache.log4j.Logger;


/**
 * The Class ClassifierManager.
 * This class has main method to run the classifier and the cycle detector.
 * Params are: -CC to run cycle detector, -CR to run classifier, {file} file containing params  
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
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
