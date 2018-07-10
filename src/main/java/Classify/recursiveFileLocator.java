package Classify;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

class recursiveFileLocator
{
  ThreadPoolExecutor exService;
  String filepath;


  private static int count;

  void startWalk()
    throws NoSuchAlgorithmException
  {
    walk(this.filepath);
    this.exService.shutdown();
    while (!this.exService.isTerminated())
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException ex) {
        Logger.getLogger(recursiveFileLocator.class.getName()).log(Level.SEVERE, null, ex);
      }

  }

  private void walk(String path)

  {
    File root = new File(path);
    final File[] list = root.listFiles();
    
    Runnable worker;
    for (File file : list)
    {
      if (file.isDirectory()) {
        walk(file.getAbsolutePath());
      }
      else
      {
        
            

        worker = new ClassifyThread(file);
        this.exService.execute(worker);
        System.out.println(String.valueOf(count++) + " " + file.getAbsolutePath());

        while (this.exService.getQueue().size() > 500)
          try {
            Thread.sleep(1000L);
          } catch (InterruptedException ex) {
            Logger.getLogger(recursiveFileLocator.class.getName()).log(Level.SEVERE, null, ex);
          }
          
        
      }
    }
  }
}