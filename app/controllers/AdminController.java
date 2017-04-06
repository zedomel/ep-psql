package controllers;

import java.io.File;

import javax.inject.Inject;

import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import services.DocumentParserService;
import views.html.Admin;

public class AdminController extends Controller{
	
	
	private final DocumentParserService parserService;
	
	private final FormFactory formFactory;
	
	@Inject
	public AdminController(DocumentParserService parserService, FormFactory formFactory) {
		this.parserService = parserService;
		this.formFactory = formFactory;
	}
	
	/**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(Admin.render(""));
    }
    
    public Result submit(){
    	MultipartFormData<Object> body = request().body().asMultipartFormData();
        FilePart<Object> fileInfo = body.getFile("file");
        if ( fileInfo != null ){
        	String contentType = fileInfo.getContentType();
        	if ( contentType.equals("application/zip")){
        		File file = (File) fileInfo.getFile();
        		try {
					parserService.addDocumentsFromPackage(file);
					return ok(Admin.render(file.getAbsolutePath()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        
        flash("error", "Missing file");
        return badRequest();
    }

}
