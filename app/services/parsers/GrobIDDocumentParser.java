package services.parsers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.GrobidPropertyKeys;

import model.Document;
import play.Configuration;
import services.DocumentParser;
import services.Utils;

/**
 * GROBID document parser.
 * Classe para processar documentos (artigos cientificos)
 * utilizando GROBID.
 * @author jose
 *
 */
public final class GrobIDDocumentParser implements DocumentParser{

	private static Engine ENGINE;

	private final Engine engine;

	private BiblioItem metadata;

	private List<BibDataSet> references;

	private final boolean consolidate;

	public GrobIDDocumentParser() throws Exception {
		this(false);
	}

	/**
	 * Cria um novo {@link GrobIDDocumentParser} (singleton)
	 * @param consolidate: the consolidation option allows GROBID to 
	 * exploit Crossref web services for improving header information
	 * @throws Exception se uma exceção ocorrer ao carregar GROBID
	 */
	public GrobIDDocumentParser(boolean consolidate) throws Exception {
		this.consolidate = consolidate;
		initialize();
		engine = ENGINE;
	}

	/**
	 * Inicializa GROBID
	 * @throws Exception
	 */
	private void initialize() throws Exception {
		InitialContext ic = new InitialContext();
		try{
			ic.lookup("java:comp/env/" + GrobidPropertyKeys.PROP_GROBID_HOME);
		}catch(NamingException e){
			Configuration configuration = Configuration.reference();
			String grobidHome = configuration.getString("grobid.home", "grobid-home");
			String grobidProperties = configuration.getString("grobid.properties", "grobid-home/config/grobid.properties");

			try {
				MockContext.setInitialContext(grobidHome, grobidProperties);
			} catch (Exception e1) {
				throw e1;
			}
			GrobidProperties.getInstance();	
			ENGINE = GrobidFactory.getInstance().createEngine();
		}
	}

	/**
	 * Returna a engine do GROBID
	 * @return grobid engine
	 */
	public Engine getEngine() {
		return engine;
	}

	@Override
	public void parseHeader(String filename) {
		metadata = new BiblioItem();
		engine.processHeader(filename, consolidate , metadata);
	}

	@Override
	public void parseReferences(String filename) {
		references = engine.processReferences(new File(filename), consolidate);
	}

	@Override
	public String getAuthors() {
		if (metadata.getFullAuthors() != null){
			StringBuilder sb = new StringBuilder();
			for(Person p : metadata.getFullAuthors()){
				sb.append(Utils.sanitize(p.getFirstName()));
				sb.append(" ");
				sb.append(Utils.sanitize(p.getMiddleName()));
				sb.append(" ");
				sb.append(Utils.sanitize(p.getLastName()));
				sb.append(Utils.AUTHOR_SEPARATOR);
			}
			sb.replace(sb.length()-1, sb.length(), "");
			return sb.toString();
		}
		return metadata.getAuthors();
	}

	@Override
	public String getLanguage() {
		return Utils.languageToISO3166(metadata.getLanguage());
	}
	
	@Override
	public String getTitle() {
		return metadata.getTitle();
	}

	@Override
	public String getAffiliation() {
		return metadata.getAffiliation();
	}

	@Override
	public String getDOI() {
		return metadata.getDOI();
	}

	@Override
	public String getPublicationDate() {
		return metadata.getPublicationDate() == null ? metadata.getYear() : 
			metadata.getPublicationDate();
	}

	@Override
	public String getAbstract() {
		return metadata.getAbstract();
	}

	@Override
	public String getJournal() {
		return metadata.getJournal();
	}

	@Override
	public String getKeywords() {
		if (metadata.getKeywords() != null )
			return metadata.getKeywords().stream().map(s -> s.getKeyword().toString()).collect(Collectors.joining(", "));
		return null;
	}
	
	@Override
	public String getContainer() {
		String container = null;
		if ( metadata.getJournal() != null )
			container = metadata.getJournal();
		else if (metadata.getBookTitle() != null)
			container = metadata.getBookTitle();
		else if ( metadata.getEvent() != null )
			container = metadata.getEvent();
		return container;
	}

	@Override
	public String getIssue() {
		return metadata.getIssue();
	}

	@Override
	public String getISSN() {
		return metadata.getISSN() != null ? metadata.getISSN() : metadata.getISSNe();
	}

	@Override
	public String getPages() {
		return metadata.getPageRange() != null ? metadata.getPageRange() : (
				metadata.getBeginPage() + "-" + metadata.getEndPage());
	}

	@Override
	public String getVolume() {
		return metadata.getVolume();
	}

	@Override
	public List<Document> getReferences() {
		List<Document> refs = new ArrayList<>(references.size());
		for(BibDataSet bds : references){
			BiblioItem bib = bds.getResBib();
			Document ref = new Document();
			
			ref.setDOI(bib.getDOI());
			ref.setAuthors(bib.getAuthors());
			ref.setTitle(bib.getTitle());
			ref.setAbstract(bib.getAbstract());
			ref.setIssue(bib.getIssue());
			if (bib.getKeywords() != null && !bib.getKeywords().isEmpty() )
				ref.setKeywords(bib.getKeywords().stream().map(s -> s.getKeyword().toString()).collect(Collectors.joining(", ")));
			ref.setLanguage(Utils.languageToISO3166(bib.getLanguage()));
			ref.setPages(bib.getPageRange());
			ref.setVolume(bib.getVolume());
			
			// Set Container ISSN
			if ( bib.getISSN() != null )
				ref.setISSN(bib.getISSN());
			else if (bib.getISSNe() != null )
				ref.setISSN(bib.getISSNe());
			
			// Set Container Name
			if ( bib.getJournal() != null )
				ref.setContainer(bib.getJournal());
			else if ( bib.getEvent() != null )
				ref.setContainer(bib.getEvent());
			else if ( bib.getBookTitle() != null )
				ref.setContainer(bib.getBookTitle());
			
			//Set publication date
			if ( bib.getPublicationDate() != null )
				ref.setPublicationDate( bib.getPublicationDate() );
			else if ( bib.getYear() != null ) 
				ref.setPublicationDate( bib.getYear() );
			
			refs.add(ref);
		}
		return refs;
	}
}
