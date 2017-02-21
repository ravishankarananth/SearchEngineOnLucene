import java.io.IOException;

public class QrySopWsum extends QrySop{

	 /**
	   *  Indicates whether the query has a match.
	   *  @param r The retrieval model that determines what is a match
	   *  @return True if the query matches, otherwise false.
	   */
	  public boolean docIteratorHasMatch (RetrievalModel r) {
	    return this.docIteratorHasMatchMin(r);
	  }

	  
	  public double getScore (RetrievalModel r) throws IOException {

		  if (r instanceof RetrievalModelIndri){
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
		      return 0.0;
		    } else {
		    	double sumweight = 0;
		    	for(int i=0; i <this.args.size(); i++){
		    		if(QryParser.testweight.containsKey(this.args.get(i)))
		    			sumweight+= QryParser.testweight.get((this.args.get(i)));
		    		else
		    			sumweight += QryParser.testweight.get((this.args.get(i).args.get(0)));
		    	}
		    	
		    	
		    	double queryweight = 0.0;
		    	double weightpower =0.0;
		    	double score =0.0;
		    	double sum=0.0;
		    	int docIDmin=this.docIteratorGetMatch(); //variable to find the minimum docID
		    	//System.out.println(docIDmin);
		    	//double gmean = 1.0/(double)this.args.size();
		    	for (int i=0; i <this.args.size(); i++){
		    		if (this.args.get(i).docIteratorHasMatchCache()){
		    		if (((QrySop)this.args.get(i)).docIteratorGetMatch()==docIDmin) {
		    			
		    			if(QryParser.testweight.containsKey(this.args.get(i)))
		    				queryweight= QryParser.testweight.get((this.args.get(i)));
			    		else
			    			queryweight = QryParser.testweight.get((this.args.get(i).args.get(0)));
		    			
		    			weightpower = queryweight/sumweight;
		    			sum = ((QrySop)this.args.get(i)).getScore(r)* weightpower;
		    		score += sum; 
		    		
		    		}
		    		else{
		    			
		    			if(QryParser.testweight.containsKey(this.args.get(i)))
		    				queryweight= QryParser.testweight.get((this.args.get(i)));
			    		else
			    			queryweight = QryParser.testweight.get((this.args.get(i).args.get(0)));
		    			
		    			weightpower = queryweight/sumweight;
		    			
		    			sum =  ((QrySop)this.args.get(i)).getDefaultScore(r, docIDmin)* weightpower;
		    		score += sum;
		    		}
		    		} else
		    		{	
		    			if(QryParser.testweight.containsKey(this.args.get(i)))
		    				queryweight = QryParser.testweight.get((this.args.get(i)));
			    		else
			    			queryweight = QryParser.testweight.get((this.args.get(i).args.get(0)));
		    			
		    			weightpower = queryweight/sumweight;
		    			
		    			sum =  ((QrySop)this.args.get(i)).getDefaultScore(r, docIDmin)*weightpower;
		    			score += sum;
		    		}
		    	}
		    return score;
		    
		    }
	}

	@Override
	  public double getDefaultScore(RetrievalModel r, int docID) throws IOException {
	  	// TODO Auto-generated method stub
	  		double score =0.0;
	  		double sumweight = 0;
	    	for(int i=0; i <this.args.size(); i++){
	    		if(QryParser.testweight.containsKey(this.args.get(i)))
	    			sumweight+= QryParser.testweight.get((this.args.get(i)));
	    		else
	    			sumweight += QryParser.testweight.get((this.args.get(i).args.get(0)));
	    	}
	    	
	    	double queryweight = 0;
	    	double weightpower =0;
	  		
	  		//double gmean = 1.0/(double)this.args.size();
	  		for (int i = 0; i < this.args.size(); i++){	 
	  			if(QryParser.testweight.containsKey(this.args.get(i)))
  				queryweight = QryParser.testweight.get((this.args.get(i)));
	    		else
	    			queryweight = QryParser.testweight.get((this.args.get(i).args.get(0)));
  			
  			weightpower = queryweight/sumweight;
  			
	  			score = score + (((QrySop)this.args.get(i)).getDefaultScore(r, docID) * weightpower);
	  		}
	    	return score;
	  }  
	  

	}
	

