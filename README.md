rf2-classification-snorocket
============================

This project supports the classification of SNOMED CT Stated Relationships in RF2 format using the SNOROCKET Reasoner.

============================

To run the classifier and cycle check:

java -jar rf2-classification-snorocket.jar -CC -CR Runconfiguration.xml

where 
-CC invoke the cycle check
-CR invoke the classification runner
Runconfiguration.xml is the parameter file
