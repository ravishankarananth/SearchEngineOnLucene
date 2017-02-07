import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QryIopNear extends QryIop{

		int operatorDistance;
	  public QryIopNear(int operatorDistance) {
		// TODO Auto-generated constructor stub
		  this.operatorDistance=operatorDistance;
	  }

	
	
	@Override
	protected void evaluate() throws IOException {
		// TODO Auto-generated method stub
		
		this.invertedList = new InvList (this.getField());
		
		if (args.size () == 0) {
		      return;
		    }
		List<Integer> positions;
		/*int minTf = ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().getTF(); 
		for(int i = 1; i< this.args.size();i++){
			int temp =((QryIop)this.args.get(i)).docIteratorGetMatchPosting().getTF();
			if(minTf> temp) {
				minTf = temp;
			}
		}*/
		
		while (true){
			
		      int minDocid = Qry.INVALID_DOCID;
		      int maxDocId=0;
		      int index=0;
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
			boolean flag1=true;
			boolean endLoop=false;
		      for (Qry q_i: this.args){
		    	  if(q_i.docIteratorHasMatch(null)){
		    	  if(minDocid!=q_i.docIteratorGetMatch()){
		    		  flag1=false;
		    	  }
		      } else{endLoop=true;}
		    	  }
		      if(endLoop){
		    	  break;
		      }
		      if(!flag1){
		    	  if(this.args.get(index).docIteratorHasMatch(null)){
		    		  this.args.get(index).docIteratorAdvanceTo(maxDocId);
		    	  }
		    		  
		      }
		      else{
		    	  positions= new ArrayList<Integer>();
		    	  boolean endloop2 = false;
		    	  int checkVal=0;
		    	  while(true){
		    	  boolean flag=false;
		    	  int prevar=0; 
		    	  checkVal=0;
		    	  if(((QryIop)this.args.get(0)).locIteratorHasMatch()){  
		    		  prevar=((QryIop)this.args.get(0)).locIteratorGetMatch();
		    	  flag=false;
		    	  
					for(int k = 1; k<this.args.size(); k++){
						if(((QryIop)this.args.get(k)).locIteratorHasMatch()){
						int newvar = ((QryIop)this.args.get(k)).locIteratorGetMatch();
						if((newvar-prevar)<=this.operatorDistance && (newvar-prevar)>0){
							flag=true;
						}
						else{
							flag=false;
							checkVal=k;
							break;
						}
						prevar=newvar;
					}
					else{
						endloop2=true;
					}
					}}
		    	  else{
		    		  break;
		    	  }
			if(endloop2){
				flag=false;
				break;
			}
				if(flag){
					
					positions.add(((QryIop)this.args.get(0)).locIteratorGetMatch());
					
				for(int k=0;k<this.args.size(); k++){
						if(((QryIop)this.args.get(k)).locIteratorHasMatch())
						((QryIop)this.args.get(k)).locIteratorAdvance();
					else
							break;
					}
				}
				else{					
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
						if(checkVal!=0){
						if(((QryIop)this.args.get(checkVal)).locIteratorGetMatch()<((QryIop)this.args.get(checkVal-1)).locIteratorGetMatch()){
							index=checkVal;
						}}
						((QryIop)this.args.get(index)).locIteratorAdvance();
						}
					else
						break;
					}
		    	  }
				
				if(!positions.isEmpty()){
					Collections.sort (positions);				
					this.invertedList.appendPosting(minDocid, positions);}
		      
				for (Qry q_i: this.args){
					q_i.docIteratorAdvancePast(minDocid);
				}
		      
		      }
		
		
		}
	}	
	}


