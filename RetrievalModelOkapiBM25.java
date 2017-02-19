
public class RetrievalModelOkapiBM25 extends RetrievalModel{
	double bm25_k1;
	double bm25_b;
	double bm25_k3;
	
	
	public RetrievalModelOkapiBM25(double bm25_k1, double bm25_b, double bm25_k3) {
		super();
		this.bm25_k1 = bm25_k1;
		this.bm25_b = bm25_b;
		this.bm25_k3 = bm25_k3;
	}


	public double getBm25_k1() {
		return bm25_k1;
	}


	public double getBm25_b() {
		return bm25_b;
	}


	public double getBm25_k3() {
		return bm25_k3;
	}


	public String defaultQrySopName () {
	    return new String ("#sum");
	  }
}
