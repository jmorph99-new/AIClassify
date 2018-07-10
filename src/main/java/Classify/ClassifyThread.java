/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Classify;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.WriteOutContentHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author murphy
 */
class ClassifyThread implements Runnable{

    static StandardAnalyzer standardAnalyzer;
    static IndexWriter indexWriter;
    static ConcurrentHashMap<String, Object> cannotProcessList;
    private File filepath;


            
    ClassifyThread(File filepath){
        //noinspection AccessStaticViaInstance
        this.filepath = filepath;
    }
    @Override
    public void run() {

        Document doc = new Document();

        Parser parser;
        StringWriter writer;
        Metadata metadata;
        
        
        parser = new AutoDetectParser();
        ByteArrayInputStream byteArrayInputStream = null;
       
        metadata = new Metadata();
        writer = new StringWriter();
        try {
            byteArrayInputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ClassifyThread.class.getName()).log(Level.SEVERE, null, ex);
        }



       String extractedFileText;
        try {
            parser.parse(byteArrayInputStream,new WriteOutContentHandler(writer),metadata,new ParseContext());
            extractedFileText = writer.toString();
        } catch (Exception ex) {
            extractedFileText = "Could not parse";
        }

        

        FieldType ft = new FieldType();
        ft.setStored(true);
        ft.setTokenized(true);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorPositions(true);
        ft.setStoreTermVectorOffsets(true);
        
        doc.add(new StringField("filepath",filepath.getAbsolutePath(),Field.Store.YES));
        doc.add(new Field("text",extractedFileText.toLowerCase(),ft));
        TokenStream ts = standardAnalyzer.tokenStream("text", extractedFileText.toLowerCase());
        try {
            ts.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int count = 0;
        try {
            while(ts.incrementToken()){
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ts.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(count > 4)
            adddoc(doc);
        else{
            cannotProcessList.put(filepath.getAbsolutePath(), new Object());
        }
    }
    private void adddoc(Document document)
    {
        try{
            indexWriter.addDocument(document);
        }
        catch(java.lang.OutOfMemoryError oom ){

            System.out.println(filepath.getAbsolutePath() + "\r\n" + " Out of memory!");
        }
        catch(org.apache.lucene.index.CorruptIndexException com ){

            System.out.println(filepath.getAbsolutePath() + "\r\n" + " Index Corrupt!");
        }
        catch(java.io.IOException ex){
            System.out.println(filepath.getAbsolutePath() + "\r\n" + ex.getMessage());
        }
        catch(Exception ex){
            System.out.println(filepath.getAbsolutePath() + "\r\n" + ex.getMessage());
            
        }
         
    }

}
    
