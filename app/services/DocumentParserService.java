package services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.grobid.core.mock.MockContext;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.Document;
import play.mvc.Controller;
import services.parsers.GrobIDDocumentParser;

/**
 * Classe principal para processamento dos documentos
 * adicionando os ao banco de dados.
 * @author jose
 *
 */
@Singleton
public class DocumentParserService extends Controller {

	private Logger logger = LoggerFactory.getLogger(DocumentParserService.class);

	/**
	 * Document Parser: GROBID and Cermine
	 */
	private DocumentParser documentParser;

	private DatabaseService dbService;

	public DocumentParserService( DocumentParser parser, DataSource dataSource ) throws IOException{
		this.documentParser = parser;
		this.dbService = new DatabaseService(dataSource);
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
			final List<Document> docs = new ArrayList<>();
			//Iterates over all documents in the directory
			Files.list(dir.toPath()).forEach( (path) -> 
			{
				try {
					// Parse document file
					Document doc = parseDocument(path.toFile());
					List<Bibliography> references = parseReferences(path.toFile(), doc);

					if (doc != null){
						// Write document to the index
						dbService.addDocument(doc);
						docs.add(doc);
					}

				} catch (Exception e) {
					logger.error("Error importing document: "+path.toAbsolutePath(), e);
				}
			});

			//			updateCitations(writer, docs);

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
					List<Bibliography> references = parseReferences(file, doc);
					if ( references != null && ! references.isEmpty() ){
						addReferences(doc, references);
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
	public void removeDocument(String id) throws Exception{
		try{
			//				updateCitations(writer, Arrays.asList(isearch.doc(hits[0].doc)));
			// Remove from Index
			dbService.deleteDocument(id);
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * Update documents citations by adding nodes and
	 * edges to Neo4j graph and updating citCount field
	 * @param doc referenced document to process
	 * @throws Exception 
	 */
	//	private long addCitation(Document doc, Bibliography bib) throws Exception {
	//		try {
	//			long citedNodeId = DatabaseHelper.createCitaton(doc, bib.getDOI(), 
	//					bib.getTitle(), bib.getAuthors(), bib.getPublicationDate());
	//			return citedNodeId;
	//		} catch (Exception e) {
	//			logger.error("Error adding citation for document: " + doc.get("file"), e);
	//			throw e;
	//		}
	//	}

	private void addReferences(Document doc, List<Bibliography> references) throws IOException{
		for( Bibliography bib : references ){
			try {
				long refId = dbService.addReference(bib);
				dbService.addCitation(doc.getDocId(), refId);
			} catch (Exception e) {
				logger.error("Can't add referece: "+bib.toString() + " of doc: " + doc.toString() );
			}
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

	private List<Bibliography> parseReferences(File docFile, Document doc) {
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
		
		if ( title == null || authors == null)
			throw new Exception("Document has no title or authors");
			
		//TODO: adicionar outros campos ao objeto
		Document doc = new Document();
		doc.setTitle(title);
		doc.setAuthors(authors);
		doc.setKeywords(documentParser.getKeywords());
		doc.setAbstract(documentParser.getAbstract());
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

	/**
	 * Normalize authors raw string: removes new lines, extra spaces and
	 * 'and' words.
	 * @param authors a string to be normalized
	 * @return a normalized string with new line, extra 
	 * white spaces and  'and' words removed.
	 */
	private String normalizeAuthors(String authors) {
		return authors.replaceAll("\n|;|\\s+and\\s+", Utils.AUTHOR_SEPARATOR).toLowerCase();
	}

	public static void main(String[] args) throws Exception {

		if ( args.length != 1){
			System.out.println("Provide the directory path where articles are located");
			return;
		}
		
		try {
			PGSimpleDataSource ds = new PGSimpleDataSource();
			ds.setUrl("jdbc:postgresql://localhost/petrica");
			ds.setUser("postgres");
			ds.setPassword("kurt1234");
			DocumentParserService indexer = new DocumentParserService(new GrobIDDocumentParser(true), ds);
			indexer.addDocuments(args[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
