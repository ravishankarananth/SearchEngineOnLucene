
public class RetrievalModelLetor extends RetrievalModel{
	double bm25_k1;
	double bm25_b;
	double bm25_k3;
	double idLambda;
	double idMu;
	double svmRankParamC;
	
	
	
	public double getBm25_k1() {
		return bm25_k1;
	}



	public double getBm25_b() {
		return bm25_b;
	}



	public double getBm25_k3() {
		return bm25_k3;
	}



	public double getIdLambda() {
		return idLambda;
	}



	public double getIdMu() {
		return idMu;
	}



	public double getSvmRankParamC() {
		return svmRankParamC;
	}



	public RetrievalModelLetor(double bm25_k1, double bm25_b, double bm25_k3, double idLambda, double idMu,
			double svmRankParamC) {
		super();
		this.bm25_k1 = bm25_k1;
		this.bm25_b = bm25_b;
		this.bm25_k3 = bm25_k3;
		this.idLambda = idLambda;
		this.idMu = idMu;
		this.svmRankParamC = svmRankParamC;
	}



	@Override
	public String defaultQrySopName() {
		
		return new String ("#and");
	}

}
