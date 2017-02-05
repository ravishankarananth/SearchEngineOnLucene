/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.Vector;


/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchMin (r);
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
    } 
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }
  
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
	    if (! this.docIteratorHasMatchCache()) {
	      return 0.0;
	    } else {
	    	
	    	double max = 0.0;
	    	int docIDmin=this.docIteratorGetMatch();
	    		for (int i=0; i <this.args.size(); i++){
	    			double temp = 0;
	    			if (this.args.get(i).docIteratorHasMatchCache())
	    		    	if (((QrySop)this.args.get(i)).docIteratorGetMatch()==docIDmin)
		    		    	temp = ((QrySop)this.args.get(i)).getScore(r);
	    		    		if (max<temp){
	    		    			max = temp;
	    		    		}
	    		    	}
	    return max;		    		
	    
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
