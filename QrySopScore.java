/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

import org.apache.lucene.queryparser.classic.QueryParser;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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
    }else if (r instanceof RetrievalModelRankedBoolean){    	
    	return this.getScoreRankedBoolean (r); }
    else if(r instanceof RetrievalModelOkapiBM25){
    	return this.performOkapiFormula( (RetrievalModelOkapiBM25) r);
    }else if(r instanceof RetrievalModelIndri){
    	return this.getScoreIndri( (RetrievalModelIndri) r);
    }
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }
 
   
  


/**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
	    if (! this.docIteratorHasMatchCache()) {
	      return 0.0;
	    } else {
	    	//cast into IOP and get the TF. We know that score operator arguments are always QRYIOP.
	    	return ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().tf;
	    }
	      
	    
	  }
  /*
   * Function that performs the BM25 formula and returns the score
   */
  private double performOkapiFormula(RetrievalModelOkapiBM25 r) throws IOException{
	  if (! this.docIteratorHasMatchCache()) {
	      return 0.0;
	    } else {
	    	   
	  	  double rsjweight = (Idx.getNumDocs() - ((QryIop)this.args.get(0)).getDf() + 0.5)/ (((QryIop)this.args.get(0)).getDf()+0.5);
	  	  rsjweight = Math.log(rsjweight);
	  	  double termfreq = ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().tf;
	  	  double sizeOfdoc = Idx.getFieldLength( ((QryIop)this.args.get(0)).getField(), ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().docid);
	  	  double averageLength =  Idx.getSumOfFieldLengths(((QryIop)this.args.get(0)).getField()) / (double)Idx.getDocCount (((QryIop)this.args.get(0)).getField());
	  	  
	  	  double innervalue = (1-r.getBm25_b()) + (r.getBm25_b() * ( sizeOfdoc / averageLength) ); 
	  	  double tfweight = termfreq/(termfreq + (r.getBm25_k1() * innervalue));
	  	  return rsjweight*tfweight;
	    	
	    }
	   
  }
  
  
  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   *  
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }
/*
 * Overriding the Default score function. 
 * Calculation of the default score with tf =0
 * (non-Javadoc)
 * @see QrySop#getDefaultScore(RetrievalModel, int)
 */
@Override
public double getDefaultScore(RetrievalModel r, int docID) throws IOException {
	// TODO Auto-generated method stub
	
	RetrievalModelIndri r_indri= (RetrievalModelIndri)r;


	
	// calculate the default score.
	double sizeOfdoc = Idx.getFieldLength( ((QryIop)this.args.get(0)).getField(), docID);
	double lengthofC = Idx.getSumOfFieldLengths(((QryIop)this.args.get(0)).getField());
	double ctf = ((QryIop)this.args.get(0)).getCtf();
	double OneminusLamda = (1-r_indri.getIdLambda());
	double prior = (ctf/lengthofC);
	double lengthAndMu = (sizeOfdoc+r_indri.getIdMu());
	double score = OneminusLamda * (r_indri.getIdMu()*prior)/lengthAndMu;
	score = score + (r_indri.getIdLambda()*(ctf/lengthofC));
	
  	return score;
}

/*
 * Calculates the Indri score here. Applies the formula and returns the score.
 */
private double getScoreIndri(RetrievalModelIndri r) throws IOException {
	
	  if (! this.docIteratorHasMatchCache()) {
	      return 1.0;
	    } else {
	    	   	
	    	
	    	double termfreq = ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().tf;
	    	double sizeOfdoc = Idx.getFieldLength( ((QryIop)this.args.get(0)).getField(), ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().docid);
	    	double lengthofC = Idx.getSumOfFieldLengths(((QryIop)this.args.get(0)).getField());
	    	double ctf = ((QryIop)this.args.get(0)).getCtf();
	    	double OneminusLamda = (1-r.getIdLambda());
	    	double prior = (ctf/lengthofC);
	    	double lengthAndMu = (sizeOfdoc+r.getIdMu());
	    	
	    	double score = OneminusLamda * (termfreq + r.getIdMu()*prior)/lengthAndMu;
	    	score = score + (r.getIdLambda()*prior);

	    	return score;
	    }
}

}
