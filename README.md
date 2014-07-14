###rf2-classification-snorocket
============================

This project supports the classification of SNOMED CT Stated Relationships in RF2 format using the SNOROCKET Reasoner.

============================

####How to run the classifier and cycles detection from the command line:

java -jar rf2-classification-snorocket.jar [-CC] [-CR] [config]

**Parameters** 
* -CC Runs cycles detection
* -CR Runs the reasoner (classifier)
* config path to an XML configuration file

**Example**

java -jar rf2-classification-snorocket.jar -CC -CR Runconfiguration.xml
