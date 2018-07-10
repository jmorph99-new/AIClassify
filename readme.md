Usage
USAGE: java -jar <PathToAIClassify.jar> <pathOfDirectoryToProcess> <pathToTempDirectoryForIndex> <randomSeed> <SimilarityScore> <numberOfThreadsUsed>

RandomSeed - just a value to change if you change the random selection of the centroids.
SimilarityScore -  How close the documents need to be to create a cluster.  I suggest to start with 50 and adjust from there.

Instruction

1) Install GIT
2) install MAVEN
3) Install Project From Git.  run "git clone https://github.com/jmorph99/AIClassify.git"
4) run "cd AIClassify"
5) Compile and install.  run "mvn install"
6) move libraries.  Run "mvn dependency:copy-dependencies"
6) java -jar <PathTo>/AIClassify.jar <pathOfDirectoryToProcess> <pathToTempDirectoryForIndex> <randomSeed> <SimilarityScore> <numberOfThreadsUsed>

This will create a results.csv file that can be opened in Excel
First Column is the groupids
Second Column is the centroid file.
Third Column is the file grouped with the centroid

Special Cases

groupid:-1 - Centroids with no members.
groupid:-2 - Files that could not be processed.

In these cases the second and third columns are identical


