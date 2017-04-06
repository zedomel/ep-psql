package services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.grobid.core.mock.MockContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import model.Document;
import play.Configuration;
import play.db.Database;
import play.db.DefaultDatabase;
import services.database.DatabaseService;
import services.parsers.GrobIDDocumentParser;

/**
 * Classe principal para processamento dos documentos
 * adicionando os ao banco de dados.
 * @author jose
 *
 */
public class DocumentParserService {

	private Logger logger = LoggerFactory.getLogger(DocumentParserService.class);

	/**
	 * Document Parser
	 */
	private DocumentParser documentParser;

	private DatabaseService dbService;
	
	private String TMP_DIR = System.getProperty("java.io.tmpdir");

	@Inject
	public DocumentParserService( @Named("docParser") DocumentParser parser, Database db ) throws IOException{
		this.documentParser = parser;
		this.dbService = new DatabaseService(db);
	}
	
	public void addDocumentsFromPackage(File packageFile) throws Exception{
		
		byte[] buffer = new byte[1024];
		final String output = TMP_DIR + File.separator + packageFile.getName() + "_" + System.nanoTime();
		File outputDir = new File(output);
		if ( !outputDir.mkdirs() )
			return; //TODO should throw an error here!
		
		try ( ZipInputStream zis = new ZipInputStream(new FileInputStream(packageFile));){
			ZipEntry entry = zis.getNextEntry();
			
			while ( entry != null ){
				if ( entry.isDirectory() || !entry.getName().endsWith(".pdf")){
					entry = zis.getNextEntry();
					continue;
				}
				
				File newFile = new File(outputDir + File.separator + entry.getName());
				try(FileOutputStream fos = new FileOutputStream(newFile)){
					int len;
					while ( (len = zis.read(buffer)) > 0){
						fos.write(buffer, 0, len);
					}
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					continue;
				}
				
				entry = zis.getNextEntry();
			}
		}catch (Exception e) {
			// TODO: handle exception
			throw e;
		}
		
		addDocument(outputDir.getAbsolutePath());
	}
	
	/**
	 * Import all document in given directory to the index and
	 * also create Neo4j nodes.
	 * Initially all imported documents have citation count 1 (one)
	 * then {@link #updateCitations(DatabaseHelper, Document)} is called
	 * to update citation count fields.
	 * @param docsDir directory contains PDF documents.
	 * @throws IOException a error occurs when indexing documents.
	 */
	public void addDocuments(String docsDir) throws IOException
	{
		File dir = new File(docsDir);
		try{
			//Iterates over all documents in the directory
			Files.list(dir.toPath()).forEach( (path) -> 
			{
				try {
					addDocument(path.toFile().getAbsolutePath());
				} catch (Exception e) {
					logger.error("Error importing document: "+path.toAbsolutePath(), e);
				}
			});

		}catch(Exception e){
			throw e;	
		}
	}

	/**
	 * Adds a new document to index
	 * @param docPath the full path to the document
	 * to be inserted
	 * @throws Exception error adding to index.
	 */
	public void addDocument(String docPath) throws Exception
	{
		File file = new File(docPath);
		try {
			Document doc = parseDocument(file);
			if (doc != null){
				// Write document to the Index
				long docId = dbService.addDocument(doc);
				if ( docId > 0){
					doc.setDocId(docId);
					List<Document> references = parseReferences(file, doc);
					if ( references != null && ! references.isEmpty() ){
						dbService.addReferences(docId, references);
					}
				}

			}
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * Removes a document from the Index.
	 * @param id the id of the document to remove (Neo4j node id).
	 * @throws Exception if any error occurs when removing the document.
	 */
	public void removeDocument(long id) throws Exception{
		try{
			// Remove from Index
			dbService.deleteDocument(id);
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * Parses a document and creates a {@link Document} object
	 * to be inserted into Lucene index.
	 * @param filename the full filename of the document to be parsed
	 * @return a new Document to be added to the index
	 * @throws IOException if can't parse the document
	 */
	private Document parseDocument(File filename) throws Exception 
	{
		Document doc = null;
		try {
			// Processa documento utilizando os
			// parsers registrados: extrai dados
			// do cabeçalho e referências.
			doc = parseDocument(filename.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Error extracting document's information: " + filename, e);
			return null;
		}

		return doc;
	}

	private List<Document> parseReferences(File docFile, Document doc) {
		try {
			documentParser.parseReferences(docFile.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Can't parse references for: " + docFile.getAbsolutePath(), e);
			return null;
		}

		return documentParser.getReferences(); 
	}

	/**
	 * Process document using registered {@link DocumentParser}: extracts header data.
	 * @param filename path to the document's file
	 * @return 
	 * @return 
	 * @throws Exception if any error occurs extracting data.
	 */
	private Document parseDocument(String filename) throws Exception {

		// Parse document
		this.documentParser.parseHeader(filename);

		String title = documentParser.getTitle();
		String authors = documentParser.getAuthors();
		String doi = documentParser.getDOI();

		if ( (title == null || authors == null) && doi == null )
			throw new Exception("Document has no title, authors or DOI");

		Document doc = new Document();
		doc.setTitle(title);
		doc.setAuthors(authors);
		doc.setDOI(doi);
		doc.setKeywords(documentParser.getKeywords());
		doc.setAbstract(documentParser.getAbstract());
		doc.setContainer(documentParser.getContainer());
		doc.setISSN(documentParser.getISSN());
		doc.setIssue(documentParser.getIssue());
		doc.setPages(documentParser.getPages());
		doc.setVolume(documentParser.getVolume());
		doc.setPublicationDate(documentParser.getPublicationDate());
		doc.setLanguage(documentParser.getLanguage());

		return doc;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if ( documentParser instanceof GrobIDDocumentParser){
			MockContext.destroyInitialContext();
		}
	}

	public static void main(String[] args) throws Exception {

		if ( args.length != 1){
			System.out.println("Provide the directory path where articles are located");
			System.out.println("Usage: DocumentParserService <directory_with_pdf_documents>");
			return;
		}

		try {
			Config conf = ConfigFactory.parseFile(new File("conf/application.conf"));
			Configuration configuration = new Configuration(conf);

			DefaultDatabase db = new DefaultDatabase("db.default", configuration.getConfig("db.default"));
			DocumentParserService parserService = new DocumentParserService(new GrobIDDocumentParser(true), db);
			parserService.addDocuments(args[0]);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
