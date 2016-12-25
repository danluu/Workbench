package org.bitfunnel.workbench;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;  // TODO: don't import this name as is?
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
// import org.apache.lucene.index.SlowCompositeReaderWrapper; // Debug stuff.
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LuceneTest
        extends TestCase  {

    public LuceneTest(String testName) {
        super(testName);
    }


    public static Test suite() {
        return new TestSuite(LuceneTest.class);
    }

    public void testMakingATest() throws IOException {
        byte[] inputFile =
                ("000000000000007b\00000\000one\000\00001\000body\000text\000\000\000" +
                        "00000000000001c8\00000\000two\000\00001\000some\000more\000body\000text\000\000\000" +
                        "\000").getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputFile);

        Directory dir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(dir, config);

        LuceneTest.DocumentProcessor processor = new LuceneTest.DocumentProcessor(writer);
        CorpusFile corpus = new CorpusFile(inputStream);
        corpus.process(processor);
        writer.commit();


        // Now search the index:
        DirectoryReader ireader = DirectoryReader.open(dir);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        assertEquals(2, ireader.numDocs());

        // Debug prints.
        Fields fields = MultiFields.getFields(ireader);
        for (String field : fields) {
            System.out.println("Field: " + field);
            Terms terms = fields.terms(field);
            System.out.println("terms.size(): " + terms.size());
            TermsEnum termsEnum = terms.iterator();
            while (termsEnum.next() != null) {
                System.out.println(termsEnum.term().utf8ToString());
            }


        }
        // SlowCompositeReaderWrapper.wrap(ireader).terms("01");

        // Parse a simple query that searches for "text":
        Term term = new Term("01", "text");
        Query termQuery = new TermQuery(term);
        ConstantScoreQuery query = new ConstantScoreQuery(termQuery);

        TotalHitCountCollector collector = new TotalHitCountCollector();
        isearcher.search(query, collector);
        assertEquals(2, collector.getTotalHits());
        ireader.close();
        dir.close();
    }

    public class DocumentProcessor implements IDocumentProcessor
    {
        IndexWriter writer;
        Document currentDocument;
        StringBuilder currentString;
        String currentStreamName;


        public DocumentProcessor(IndexWriter writer) {
            this.writer = writer;
            currentString = new StringBuilder();
            currentDocument = new Document();
        }

        @Override
        public void openDocumentSet() {
        }

        @Override
        public void openDocument() {
            // System.out.println("openDocument");
            currentDocument.clear();
        }

        @Override
        public void openStream(String name) {
            // System.out.println("openStream: " + name);
            currentStreamName = name;
            currentString.setLength(0);
        }

        @Override
        public void term(String term) {
            // System.out.println("term: " + term);
            currentString.append(" " + term);
        }

        @Override
        public void closeStream() {
            Field field = new Field(currentStreamName, currentString.toString(), TextField.TYPE_NOT_STORED);
            currentDocument.add(field);
        }

        @Override
        public void closeDocument() {
            try {
                writer.addDocument(currentDocument);
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }

        }

        @Override
        public void closeDocumentSet() {

            //outputStream.write(0);
        }
    }
}

