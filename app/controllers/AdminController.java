package controllers;

import java.io.File;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.Database;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.routing.JavaScriptReverseRouter;
import services.DocumentParserService;
import services.pagerank.RelevanceCalculator;
import services.projection.MultidimensionalProjection;
import utils.Options;
import views.html.Admin;

public class AdminController extends Controller{


	private final DocumentParserService parserService;

	private final FormFactory formFactory;

	private final Options options;

	private final Database db;

	private String lastUpdateMPD;

	private String lastUpdatePageRank;

	@Inject
	public AdminController(DocumentParserService parserService, FormFactory formFactory, 
			Options options, Database db) {
		this.parserService = parserService;
		this.formFactory = formFactory;
		this.options = options;
		this.db = db;

		this.lastUpdateMPD = options.getOption(Options.LAST_UPDATE_MDP);
		this.lastUpdatePageRank = options.getOption(Options.LAST_UPDATE_PAGERANK);

	}

	/**
	 * An action that renders an HTML page with a welcome message.
	 * The configuration in the <code>routes</code> file means that
	 * this method will be called when the application receives a
	 * <code>GET</code> request with a path of <code>/</code>.
	 */
	public Result index() {
		return ok(Admin.render(null, lastUpdateMPD, lastUpdatePageRank));
	}

	public Result submit(){
		MultipartFormData<Object> body = request().body().asMultipartFormData();
		FilePart<Object> fileInfo = body.getFile("file");
		if ( fileInfo != null ){
			String contentType = fileInfo.getContentType();
			if ( contentType.equals("application/zip")){
				File file = (File) fileInfo.getFile();
				try {
					List<String> docs = parserService.addDocumentsFromPackage(file);
					return ok(Admin.render(docs, lastUpdateMPD, lastUpdatePageRank));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		flash("error", "Missing file");
		return badRequest();
	}

	public Result update(){
		String update = formFactory.form().bindFromRequest().get("update");
		try {
			if ( update.equals("mdp")){
				MultidimensionalProjection mdp = new MultidimensionalProjection(db);
				mdp.project();
				lastUpdateMPD = DateFormat.getDateInstance().format(Date.from(Instant.now()));
				options.setOption(Options.LAST_UPDATE_MDP, lastUpdateMPD);
				return ok(Admin.render(null, lastUpdateMPD, lastUpdatePageRank));
			}
			else if ( update.equals("pagerank")){
				RelevanceCalculator pageRank = new RelevanceCalculator(db);
				pageRank.updateRelevance();
				lastUpdatePageRank = DateFormat.getDateInstance().format(Date.from(Instant.now()));
				options.setOption(Options.LAST_UPDATE_PAGERANK, lastUpdatePageRank);
				return ok(Admin.render(null, lastUpdateMPD, lastUpdatePageRank));
			}
		}catch (Exception e) {
			flash("error", "Error updating database");
			return badRequest();
		}
			
		flash("error", "Unkown action");
		return badRequest();
	}
	
	/**
     * Create javascript routes for this controller
     * @return javascrit route
     */
    public Result javascriptRoutes(){
    	return ok(
    			JavaScriptReverseRouter.create("jsRoutes",
    					routes.javascript.AdminController.update()
    				)).as("text/javascript");
    }
}
