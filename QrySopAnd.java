import java.io.*;


public class QrySopAnd extends QrySop {
	
	
	  /**
	   *  Indicates whether the query has a match.
	   *  @param r The retrieval model that determines what is a match
	   *  @return True if the query matches, otherwise false.
	   */
	  public boolean docIteratorHasMatch (RetrievalModel r) {
		  if(r instanceof RetrievalModelIndri){
			  return this.docIteratorHasMatchMin(r);
		  }
		  else 
	    return this.docIteratorHasMatchAll(r);
	  }

	  /**
	   *  Get a score for the document that docIteratorHasMatch matched.
	   *  @param r The retrieval model that determines how scores are calculated.
	   *  @return The document score.
	   *  @throws IOException Error accessing the Lucene index
	   */
	  public double getScore (RetrievalModel r) throws IOException {

	    if (r instanceof RetrievalModelUnrankedBoolean) {
	      return this.getScoreUnrankedBoolean (r);
	    } else if (r instanceof RetrievalModelRankedBoolean){
	    	return this.getScoreRankedBoolean (r);
	    }else if (r instanceof RetrievalModelIndri){
	    	return this.getscoreIndriModel(r);
	    }
	    else {
	      throw new IllegalArgumentException
	        (r.getClass().getName() + " doesn't support the AND operator.");
	    }
	  }
	  
	  private double getscoreIndriModel(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		  
		  if (! this.docIteratorHasMatchCache()) {
		      return 1.0;
		    } else {
		    	
		    	
		    	double score =1.0;
		    	double product=1.0;
		    	int docIDmin=this.docIteratorGetMatch(); //variable to find the minimum docID
		    	//System.out.println(docIDmin);
		    	double gmean = 1.0/(double)this.args.size();
		    	for (int i=0; i <this.args.size(); i++){
		    		if (this.args.get(i).docIteratorHasMatchCache()){
		    		if (((QrySop)this.args.get(i)).docIteratorGetMatch()==docIDmin) {
		    			product = Math.pow(((QrySop)this.args.get(i)).getScore(r), gmean);
		    		score *= product; }
		    		else{
		    			product =  Math.pow(((QrySop)this.args.get(i)).getDefaultScore(r, docIDmin), gmean);
		    		score *= product;
		    		}
		    		} else
		    		{	product =  Math.pow(((QrySop)this.args.get(i)).getDefaultScore(r, docIDmin),gmean);
		    			score *= product;
		    		}
		    	}
		    return score;
		    }
	}
	  @Override
	  public double getDefaultScore(RetrievalModel r, int docID) throws IOException {
	  	// TODO Auto-generated method stub
	  		double score =1;
	  		double gmean = 1.0/(double)this.args.size();
	  		for (int i = 0; i < this.args.size(); i++){	  		
	  			score = score * Math.pow(((QrySop)this.args.get(i)).getDefaultScore(r, docID), gmean);
	  		}
	    	return score;
	  }
	  
	/**
	   *  getScore for the RankedBoolean retrieval model.
	   *  @param r The retrieval model that determines how scores are calculated.
	   *  @return The document score.
	   *  @throws IOException Error accessing the Lucene index
	   */

	  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
		    if (! this.docIteratorHasMatchCache()) {
		      return 0.0;
		    } else {
		    	Double min=Double.MAX_VALUE;
		    	
		    	//loop through the arguments and call their getscore operation.
		    		    	for (int i=0; i <this.args.size(); i++){
		    		    		
		    		    		double temp = ((QrySop)this.args.get(i)).getScore(r);
		    		    		if (min>temp){
		    		    			min = temp;
		    		    		}
		    		    	}
		    return min;		    		
		    }
		      
		    
		  }
	  
	  /**
	   *  getScore for the UnrankedBoolean retrieval model.
	   *  @param r The retrieval model that determines how scores are calculated.
	   *  @return The document score.
	   *  @throws IOException Error accessing the Lucene index
	   */
	  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
	    if (! this.docIteratorHasMatchCache()) {
	      return 0.0;
	    } else {
	      return 1.0;
	    }
	  }



}

