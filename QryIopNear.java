import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  The NEAR operator for all retrieval models.
 */
public class QryIopNear extends QryIop{

		int operatorDistance; // int value that stores the distance of the NEAR operator to check
	  public QryIopNear(int operatorDistance) { // constructor that sets the value of distance.
		// TODO Auto-generated constructor stub
		  this.operatorDistance=operatorDistance;
	  }

	
	  /**
	   *  Evaluate the query operator; the result is an internal inverted
	   *  list that may be accessed via the internal iterators.
	   *  Instead of using helper functions I have explicitly written the code. 
	   *  But it is easy to understand.
	   *  @throws IOException Error accessing the Lucene index.
	   */
	@Override
	protected void evaluate() throws IOException {
		// TODO Auto-generated method stub
		
		this.invertedList = new InvList (this.getField());
		
		if (args.size () == 0) {
		      return;
		    }
		List<Integer> positions; //position is list that stores the values of term position index  within the document.
		
		while (true){
			
		      int minDocid = Qry.INVALID_DOCID;
		      int maxDocId=0;
		      int index=0;
		      					//function to find the minimum docID, same as syn function. Also find the index of the min docID to k.
		      for (int k=0;k<this.args.size(); k++) {
		        if (this.args.get(k).docIteratorHasMatch (null)) {
		          int q_iDocid = this.args.get(k).docIteratorGetMatch ();
		          
		          if ((minDocid > q_iDocid) ||
		              (minDocid == Qry.INVALID_DOCID)) {
		            minDocid = q_iDocid;
		            index=k;
		          }
		          if(maxDocId<q_iDocid){
		        	  maxDocId=q_iDocid;  
		          }
		          
		        }
		      }

		      if (minDocid == Qry.INVALID_DOCID)
		        break;
			boolean flag1=true; //check to see if there are more documents
			boolean endLoop=false;
		      for (Qry q_i: this.args){	//iterate to matching docID
		    	  if(q_i.docIteratorHasMatch(null)){
		    	  if(minDocid!=q_i.docIteratorGetMatch()){
		    		  flag1=false;
		    	  }
		      } else{endLoop=true;}
		    	  }
		      if(endLoop){
		    	  break;
		      }
		      if(!flag1){ //check to see if there are more documents
		    	  if(this.args.get(index).docIteratorHasMatch(null)){
		    		  this.args.get(index).docIteratorAdvanceTo(maxDocId);
		    	  }
		    		  
		      }
		      else{	//DOCID matched now look for positions
		    	  positions= new ArrayList<Integer>();
		    	  boolean endloop2 = false;
		    	  int checkVal=0;
		    	  while(true){	//while for iterator. ends through breaks that happen when iterator has no match.
		    	  boolean flag=false;
		    	  int prevar=0; 
		    	  checkVal=0;
		    	  if(((QryIop)this.args.get(0)).locIteratorHasMatch()){  
		    		  prevar=((QryIop)this.args.get(0)).locIteratorGetMatch();
		    	  flag=false;
		    	  					//checking for distance in this iteration
					for(int k = 1; k<this.args.size(); k++){
						if(((QryIop)this.args.get(k)).locIteratorHasMatch()){
						int newvar = ((QryIop)this.args.get(k)).locIteratorGetMatch();
						if((newvar-prevar)<=this.operatorDistance && (newvar-prevar)>0){
							flag=true;//distance of terms are matching
						}
						else{
							flag=false;
							checkVal=k;
							break;
						}
						prevar=newvar;	//for next iteration make prevar into newvar, and compare with the new newvar.
					}
					else{
						endloop2=true;
					}
					}}
		    	  else{
		    		  break;
		    	  }
			if(endloop2){	//no iterator match break form while, go to next document.
				flag=false;
				break;
			}
				if(flag){
					//flag==true => distance are matching, add to postions list
					positions.add(((QryIop)this.args.get(0)).locIteratorGetMatch());
					
				for(int k=0;k<this.args.size(); k++){	//advance all iterators
						if(((QryIop)this.args.get(k)).locIteratorHasMatch())
						((QryIop)this.args.get(k)).locIteratorAdvance();
					else
							break;	//end of iteration
					}
				}
				else{		//Advance only the MIN iterator	
					int minvalIterator = ((QryIop)this.args.get(0)).locIteratorGetMatch();
					index=0;
					for(int k=1;k<this.args.size(); k++){
						if(((QryIop)this.args.get(k)).locIteratorHasMatch())
						if(minvalIterator>((QryIop)this.args.get(k)).locIteratorGetMatch())
						{minvalIterator = ((QryIop)this.args.get(k)).locIteratorGetMatch();
						index=k;
						}
					}
					if(((QryIop)this.args.get(index)).locIteratorHasMatch()) {
						if(checkVal!=0){	//if unmatched index iterator is lower than matched one, then we iterate that unmatched instead of min of the all the lists.
						if(((QryIop)this.args.get(checkVal)).locIteratorGetMatch()<((QryIop)this.args.get(checkVal-1)).locIteratorGetMatch()){ //check for testcase 19.
							index=checkVal;
						}}
						((QryIop)this.args.get(index)).locIteratorAdvance();
						}
					else
						break;
					}
		    	  }//outside of iterator while
				
				if(!positions.isEmpty()){	//once iterations are complete, we sort the positions list and append postings
					Collections.sort (positions);				
					this.invertedList.appendPosting(minDocid, positions);}
		      
				for (Qry q_i: this.args){	//advance all arguments to next docid
					q_i.docIteratorAdvancePast(minDocid);
				}
		      
		      }
		
		
		}
	}	
	}


