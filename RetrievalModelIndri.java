
public class RetrievalModelIndri extends RetrievalModel {
double idLambda;
double idMu;
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
	}

	}