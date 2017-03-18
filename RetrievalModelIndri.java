
public class RetrievalModelIndri extends RetrievalModel {
double idLambda;
double idMu;
double fbDocs;
double fbTerms;
double fbOrinWeight;
String fbExpansionQueryFile;
double fbMu;
Boolean fb;
String fbInitialRankingFile;
	  public String defaultQrySopName () {
	    return new String ("#and");
	  }
	public double getIdLambda() {
		return idLambda;
	}
	public double getIdMu() {
		return idMu;
	}
	public RetrievalModelIndri(double idLambda, double idMu) {
		super();
		
		this.idLambda = idLambda;
		this.idMu = idMu;
		this.fb = false;
	}
	public RetrievalModelIndri(double idLambda, double idMu, double fbDocs, double fbTerms, double fbOrinWeight,
			String fbExpansionQueryFile, double fbMu, Boolean fb, String fbInitialRankingFile) {
		super();
		this.idLambda = idLambda;
		this.idMu = idMu;
		this.fbDocs = fbDocs;
		this.fbTerms = fbTerms;
		this.fbOrinWeight = fbOrinWeight;
		this.fbExpansionQueryFile = fbExpansionQueryFile;
		this.fbMu = fbMu;
		this.fb = fb;
		this.fbInitialRankingFile = fbInitialRankingFile;
	}
	public double getFbDocs() {
		return fbDocs;
	}
	public double getFbTerms() {
		return fbTerms;
	}
	public double getFbOrinWeight() {
		return fbOrinWeight;
	}
	public String getFbExpansionQueryFile() {
		return fbExpansionQueryFile;
	}
	public double getFbMu() {
		return fbMu;
	}
	public Boolean getFb() {
		return fb;
	}
	public String getFbInitialRankingFile() {
		return fbInitialRankingFile;
	}

	
	
	}