package Classify;

import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ClassifyMain
{

  private static ConcurrentHashMap<String, Object> cannotProcessList = new ConcurrentHashMap<>();
  public static void main(String[] args)
    throws Exception {
      String slash = "/";
      if (Constants.WINDOWS)
          slash = "\\";
      if(args.length != 5) {
          System.out.println("USAGE: java -jar <PathToAIClassify.jar> <pathOfDirectoryToProcess> <pathToTempDirectoryForIndex> <randomSeed> <SimilarityScore> <numberOfThreadsUsed");
          return;
      }

      final String directoryPath = args[0];
      String indexpath = args[1];
      if (!indexpath.endsWith(slash))
          indexpath = indexpath + slash;
      final long seed = Long.valueOf(args[2]);
      final float MAXSCORE = Float.valueOf(args[3]);
      final int processCount = Integer.valueOf(args[3]);
      final StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
      File mxk = new File(indexpath);
      Directory dir = NIOFSDirectory.open(Paths.get(indexpath));

      LimitTokenCountAnalyzer limitTokenCountAnalyzer = new LimitTokenCountAnalyzer(standardAnalyzer, 5000);
      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(limitTokenCountAnalyzer);
      indexWriterConfig.setRAMBufferSizeMB(512.0D);
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      IndexWriter writer = new IndexWriter(dir, indexWriterConfig);
      ClassifyThread.indexWriter = writer;
      ClassifyThread.standardAnalyzer = standardAnalyzer;
      ClassifyThread.cannotProcessList = cannotProcessList;


      ThreadPoolExecutor exService = new ThreadPoolExecutor(processCount, processCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(5000));
      recursiveFileLocator ft = new recursiveFileLocator();

      ft.filepath = directoryPath;
      ft.exService = exService;
      ft.startWalk();


      writer.commit();
      writer.close();

    IndexReader indexReader = DirectoryReader.open(dir);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    final HashMap<Integer, String> potentialCentroids = new HashMap<>();
    final HashMap<String, HashSet<String>> centroid = new HashMap<>();
    for(int iDocNumber=0; iDocNumber < indexReader.numDocs(); iDocNumber++)
    {
        Document doc = indexReader.document(iDocNumber);
        potentialCentroids.put(iDocNumber,doc.get("filepath"));

    }
    Random random = new Random();
    random.setSeed(seed);
    MoreLikeThis moreLikeThis = new MoreLikeThis(indexReader);
    moreLikeThis.setFieldNames(new String[]{"text"});
    //Select random centroid and finds all within MAX Distance and removes them from the centroid pool
    while(potentialCentroids.size()>0){
        int randomNumber = random.nextInt(potentialCentroids.size());
        Integer[] arrayOfDocIds = potentialCentroids.keySet().toArray(new Integer[potentialCentroids.size()]);
        int docIdOfNewCentriod= arrayOfDocIds[randomNumber];
        Document newCentroidDocument = indexReader.document(docIdOfNewCentriod);
        String filePathOfCentroid = newCentroidDocument.get("filepath");
        centroid.put(filePathOfCentroid, new HashSet<>());
        potentialCentroids.remove(docIdOfNewCentriod);

        Query qry;
        try {
            qry = moreLikeThis.like(docIdOfNewCentriod);
        } catch (Exception e) {
            e.printStackTrace();
            continue;
        }
        TopDocs td = indexSearcher.search(qry,500);
        for(int i=0; i< td.scoreDocs.length; i++){
            int ndocid = td.scoreDocs[i].doc;
            if(td.scoreDocs[i].score > MAXSCORE) {
                if (potentialCentroids.containsKey(ndocid))
                    potentialCentroids.remove(ndocid);
            }
            else
                break;
        }

    }

    // for each non-centroid find nearest centroid and add it to its collection
    for(int i=0; i < indexReader.numDocs(); i++){
      Document doc = indexReader.document(i);
      System.out.println(doc.get("filepath"));
      if(centroid.containsKey(doc.get("filepath")))
        continue;
      Query qry = moreLikeThis.like(i);
      TopDocs td = indexSearcher.search(qry, 5000);
        for(int k=0; k< td.scoreDocs.length; k++){
            if(td.scoreDocs[k].score < MAXSCORE)
                break;
            String fpath = indexReader.document(td.scoreDocs[k].doc).get("filepath");
            if(centroid.containsKey(fpath)){//Only if centroid
                centroid.get(fpath).add(doc.get("filepath"));
            }
        }

    }

    //put all centroids with no neighbors into onee collection

    HashSet<String> singles = new HashSet<>();
    for(String path:centroid.keySet()) {
        if(centroid.get(path).size() == 0){
            singles.add(path);
            }
    }
    for(String sing:singles){
        centroid.remove(sing);
    }
    int centroidCount = 0;
    FileWriter fileWriter = new FileWriter("results.csv");
    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    System.out.println();
    System.out.println("Centroids ");
    System.out.println();
    for(String centroidPath:centroid.keySet()){

        System.out.println("Centroid " + centroidPath);
        for(String centroidMemberPath:centroid.get(centroidPath)){
            System.out.println(centroidMemberPath);
            bufferedWriter.write(String.valueOf(centroidCount));
            bufferedWriter.write(",");
            bufferedWriter.write(centroidPath);
            bufferedWriter.write(",");
            bufferedWriter.write(centroidMemberPath);
            bufferedWriter.write("\n");

        }
        centroidCount++;
    }

    System.out.println();
    System.out.println("Singles ");
    System.out.println();
    for(String centroidWithNoNeighbows:singles){
        System.out.println(centroidWithNoNeighbows);
        bufferedWriter.write("-1");
        bufferedWriter.write(",");
        bufferedWriter.write(centroidWithNoNeighbows);
        bufferedWriter.write(",");
        bufferedWriter.write(centroidWithNoNeighbows);
        bufferedWriter.write("\n");
    }

    System.out.println();
    System.out.println("Unable To Process ");
    System.out.println();
    for(String unProcessedDocument:cannotProcessList.keySet()){
        System.out.println(unProcessedDocument);
        bufferedWriter.write("-2");
        bufferedWriter.write(",");
        bufferedWriter.write(unProcessedDocument);
        bufferedWriter.write(",");
        bufferedWriter.write(unProcessedDocument);
        bufferedWriter.write("\n");
    }
    bufferedWriter.close();


  }
}