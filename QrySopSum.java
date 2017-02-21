/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.Vector;


public class QrySopSum extends QrySop{

	 /**
	   *  Indicates whether the query has a match.
	   *  @param r The retrieval model that determines what is a match
	   *  @return True if the query matches, otherwise false.
	   */
	  public boolean docIteratorHasMatch (RetrievalModel r) {
	    return this.docIteratorHasMatchMin(r);
	  }

	  /**
	   *  Get a score for the document that docIteratorHasMatch matched.
	   *  @param r The retrieval model that determines how scores are calculated.
	   *  @return The document score.
	   *  @throws IOException Error accessing the Lucene index
	   */
	  public double getScore (RetrievalModel r) throws IOException {

	    if (r instanceof RetrievalModelOkapiBM25) {
	      return this.getOkapiScore(r);
	    } else {
	      throw new IllegalArgumentException
	        (r.getClass().getName() + " doesn't support the SUM operator.");
	    }
	  }
	  
	  
	  
	  private double getOkapiScore(RetrievalModel r) {
		// TODO Auto-generated method stub
		  if (! this.docIteratorHasMatchCache()) {
		      return 0.0;
		    } else {
		    	Double score=0.0;
		    	
		    	int docIDmin=this.docIteratorGetMatch();
		    	//loop through the arguments and call their getscore operation.
		    		    	for (int i=0; i <this.args.size(); i++){
		    		    		
		    		    		try {
		    		    			if (this.args.get(i).docIteratorHasMatchCache())
		    		    			if (((QrySop)this.args.get(i)).docIteratorGetMatch()==docIDmin)
									score = score + ((QrySop)this.args.get(i)).getScore(r);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		    		    		
		    		    	}
		    return score;		    		
		    }
	}



}
