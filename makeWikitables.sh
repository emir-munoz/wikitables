##################################################
# Extracting RDF Triples from Wikipedia's Tables
##################################################

cd wikitables-dal-release-1.0
ant
cd ..
cd wikitables-ml-release-1.0
ant
cd ..
cd wikitables-engine-release-1.0
ant
cd ..
cd wikitables-demo-release-1.0
ant deploy
cd ..
