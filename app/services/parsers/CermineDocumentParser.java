package services.parsers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import model.Document;
import pl.edu.icm.cermine.ContentExtractor;
import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.metadata.model.DocumentAffiliation;
import pl.edu.icm.cermine.metadata.model.DocumentAuthor;
import pl.edu.icm.cermine.metadata.model.DocumentDate;
import pl.edu.icm.cermine.metadata.model.DocumentMetadata;
import services.DocumentParser;
import utils.Utils;

/**
 * CERMINE document parser
 * Classe para processar documentos (artigos cientificos)
 * utilizando CERMINE.
 * @author jose
 *
 */
public class CermineDocumentParser implements DocumentParser{


	private DocumentMetadata metadata;

	private List<BibEntry> references;

	public CermineDocumentParser() {

	}

	@Override
	public void parseHeader(String documentFile) throws Exception{
		ContentExtractor extractor = new ContentExtractor();
		InputStream input = new FileInputStream(documentFile);
		extractor.setPDF(input);		
		metadata = extractor.getMetadata();
	}
	
	@Override
	public void parseReferences(String documentFile) throws Exception {
		ContentExtractor extractor = new ContentExtractor();
		InputStream input = new FileInputStream(documentFile);
		extractor.setPDF(input);		
		references = extractor.getReferences();
	}

	@Override
	public String getAuthors() {
		StringBuilder sb = new StringBuilder();
		if (metadata.getAuthors() != null){
			for (DocumentAuthor author : metadata.getAuthors()){
				sb.append(Utils.sanitize(author.getName()));
				sb.append(Utils.AUTHOR_SEPARATOR);
			}
			sb.replace(sb.length()-1, sb.length(), "");
			return sb.toString();
		}
		return null;
	}

	@Override
	public String getTitle() {
		return metadata.getTitle();
	}

	@Override
	public String getAffiliation() {
		StringBuilder sb = new StringBuilder();
		for (DocumentAffiliation aff : metadata.getAffiliations()){
			sb.append(aff.getOrganization()+"-"+aff.getCountry());
			sb.append(";");
		}
		sb.replace(sb.length()-1, sb.length(), "");
		return sb.toString();
	}

	@Override
	public String getLanguage() {
		return "english";
	}
	
	@Override
	public String getDOI() {
		return metadata.getId(DocumentMetadata.ID_DOI);
	}

	@Override
	public String getPublicationDate() {
		DocumentDate date = metadata.getDate(DocumentDate.DATE_PUBLISHED);
		if (date != null)
			return date.getDay() + "/"+date.getMonth()+ "/"+date.getYear();
		return null;
	}

	@Override
	public String getAbstract() {
		return metadata.getAbstrakt();
	}
	
	@Override
	public String getJournal() {
		return metadata.getJournal();
	}
	
	@Override
	public String getKeywords() {
		return metadata.getKeywords().stream().map(s -> s.toString()).collect(Collectors.joining(", "));
	}
	
	@Override
	public String getContainer() {
		return metadata.getJournal();
	}

	@Override
	public String getIssue() {
		return metadata.getIssue();
	}

	@Override
	public String getISSN() {
		return metadata.getJournalISSN();
	}

	@Override
	public String getPages() {
		if ( metadata.getFirstPage() != null && metadata.getLastPage() != null )
			return metadata.getFirstPage() + "-" + metadata.getLastPage();
		return null;
	}

	@Override
	public String getVolume() {
		return metadata.getVolume();
	}

	public List<Document> getReferences(){
		List<Document> refs = new ArrayList<>(references.size());
		for(BibEntry entry : references){
			Document ref = new Document();
			
			ref.setTitle(entry.getFirstFieldValue(BibEntry.FIELD_TITLE));
			ref.setDOI(entry.getFirstFieldValue(BibEntry.FIELD_DOI));
			ref.setAuthors(entry.getAllFieldValues(BibEntry.FIELD_AUTHOR).stream().map(
					s -> s.toString()).collect(Collectors.joining(", ")));
			ref.setISSN( entry.getFirstFieldValue(BibEntry.FIELD_ISSN));
			ref.setIssue( entry.getFirstFieldValue(BibEntry.FIELD_SERIES));
			ref.setKeywords(entry.getFirstFieldValue(BibEntry.FIELD_KEYWORDS));
			ref.setLanguage(entry.getFirstFieldValue(BibEntry.FIELD_LANGUAGE));
			ref.setPages(entry.getFirstFieldValue(BibEntry.FIELD_PAGES));
			ref.setPublicationDate(entry.getFirstFieldValue(BibEntry.FIELD_YEAR));
			ref.setVolume(entry.getFirstFieldValue(BibEntry.FIELD_VOLUME));
			ref.setAbstract(entry.getFirstFieldValue(BibEntry.FIELD_ABSTRACT));
			
			if ( entry.getFirstFieldValue(BibEntry.FIELD_JOURNAL) != null )
				ref.setContainer(entry.getFirstFieldValue(BibEntry.FIELD_JOURNAL));
			else if ( entry.getFirstFieldValue(BibEntry.FIELD_BOOKTITLE) != null )
				ref.setContainer( entry.getFirstFieldValue(BibEntry.FIELD_BOOKTITLE));
			
			refs.add(ref);
		}
		return refs;
	}
}
