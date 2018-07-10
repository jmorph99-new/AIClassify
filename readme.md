Usage
USAGE: java -jar <PathToAIClassify.jar> <pathOfDirectoryToProcess> <pathToTempDirectoryForIndex> <randomSeed> <SimilarityScore> <numberOfThreadsUsed>

RandomSeed - just a value to change if you change the random selection of the centroids.
SimilarityScore -  How close the documents need to be to create a cluster.  I suggest to start with 50 and adjust from there.

Instruction

1) Install GIT
2) install MAVEN
3) Install Project From Git.  run "git clone"
4) run "cd AIClassify"
5) Compile and install.  run "mvn install"
6) java -jar target/AIClassify.jar <pathOfDirectoryToProcess> <pathToTempDirectoryForIndex> <randomSeed> <SimilarityScore> <numberOfThreadsUsed>

