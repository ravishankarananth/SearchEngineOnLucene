/*
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.io.*;
import java.security.Policy.Parameters;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;




/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static final String[] TEXT_FIELDS =
    { "body", "title", "url", "inlink" };


  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.
    
    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }
    
    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    RetrievalModel model = initializeRetrievalModel (parameters);

    //  Perform experiments.
    
    processQueryFile(parameters.get("queryFilePath"), model, parameters);

    //  Clean up.
    
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    if (modelString.equals("unrankedboolean")) {
      model = new RetrievalModelUnrankedBoolean();
    }
    else if (modelString.equals("rankedboolean")) {
    	model = new RetrievalModelRankedBoolean();
    }
    else if(modelString.equals("bm25")){
    	model = new RetrievalModelOkapiBM25(Double.parseDouble(parameters.get("BM25:k_1")), Double.parseDouble(parameters.get("BM25:b")), Double.parseDouble(parameters.get("BM25:k_3")));
    }
    else if(modelString.equals("indri")){
			if (parameters.containsKey("fb")) {
				if (parameters.get("fb").equals("true")) {
					model = new RetrievalModelIndri(Double.parseDouble(parameters.get("Indri:lambda")),
							Double.parseDouble(parameters.get("Indri:mu")), Double.parseDouble(parameters.get("fbDocs")), 
							Double.parseDouble(parameters.get("fbTerms")), Double.parseDouble(parameters.get("fbOrigWeight")), parameters.get("fbExpansionQueryFile"),
							Double.parseDouble(parameters.get("fbMu")), Boolean.parseBoolean(parameters.get("fb")), parameters.get("fbInitialRankingFile"));
					
				} else {
					model = new RetrievalModelIndri(Double.parseDouble(parameters.get("Indri:lambda")),
							Double.parseDouble(parameters.get("Indri:mu")));
				}
			} else {
				model = new RetrievalModelIndri(Double.parseDouble(parameters.get("Indri:lambda")),
						Double.parseDouble(parameters.get("Indri:mu")));
			}
    	}
    else {
      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }
      
    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qString, RetrievalModel model)
    throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qString = defaultOp + "(" + qString + ")";
    Qry q = QryParser.getQuery (qString);

    // Show the query that is evaluated
    
    System.out.println("    --> " + q);
    
    if (q != null) {

      ScoreList r = new ScoreList ();
      
      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          r.add (docid, score);
          
          q.docIteratorAdvancePast (docid);
        }
      }

     
      	r.sort();
     
      return r;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath
   *  @param model
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath,
                               RetrievalModel model, Map<String, String> parameters)
      throws IOException {

    BufferedReader input = null;
    DecimalFormat four = new DecimalFormat("0.0000");
    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));

      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        ScoreList r = null;
        if(model instanceof RetrievalModelIndri){
        	if(((RetrievalModelIndri) model).getFb()){
        		
        		//feedback is true
        		r = processQuery(query, model);
        		HashSet<String> qterms = new HashSet<String>(); // Set with terms
        		TermVector tv = null;
        		for(int i =0; i< ((RetrievalModelIndri) model).getFbDocs(); i++){
        			int dID = r.getDocid(i); //top fb docIDs
        			tv = new TermVector(dID, "body");
        			
        			int k=1;
        			while(tv.stemString(k)!=null){
        				if(!(tv.stemString(k).contains(",") || tv.stemString(k).contains(".")))
        				qterms.add(tv.stemString(k));
        				k++;
        				
        			}
        		}
        		HashMap<String, Double> termScore = new HashMap<>();
        		for (String qterm : qterms){
        			
        			double score =0.0;
        			for(int i =0; i< ((RetrievalModelIndri) model).getFbDocs(); i++){
        				score += calcScore(qterm, r.getDocid(i), r.getDocidScore(i), i );
        				}
        			termScore.put(qterm, score);
        			
        		}
        		
        		Map<String,Double> topTen = doaSort(termScore, (int)((RetrievalModelIndri) model).getFbTerms());
        		ArrayList<Double> tempscore = new ArrayList<>();
        		for(String keyset : topTen.keySet()){
        			tempscore.add(topTen.get(keyset));
        		}
        		Collections.sort(tempscore);
        		
        		String expandedQuery = new String("#wand( ");
        		for (Double score : tempscore){
        		for(String keyset : topTen.keySet()){
        			if(topTen.get(keyset)==score){
        			expandedQuery = expandedQuery + " " + four.format(topTen.get(keyset)) + " " + keyset;
        			
        			}
        		}}
        	expandedQuery = expandedQuery + ")";
        	
        	FileWriter fw = new FileWriter(parameters.get("fbExpansionQueryFile"), true);
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(qid+":\t"+expandedQuery);
            writer.newLine();
            writer.close();
            double weight = ((RetrievalModelIndri) model).fbOrinWeight;
            String newQuery = qid+":\t"+"#wand ( " + weight + " " + qLine + " " + (1-weight) + " " + expandedQuery + ")\n";
            r = processQuery(newQuery, model);
        	//query = make new query	
        	}
        	else {
        		r = processQuery(query, model);
        	}
        }else {
        	r = processQuery(query, model);
        }

        if (r != null) {
          //printResults(qid, r);
        	//
          //System.out.println();
          int length=100;
          if(parameters.containsKey("trecEvalOutputLength"))
          length = Integer.parseInt(parameters.get("trecEvalOutputLength"));
          
          FileWriter fw = new FileWriter(parameters.get("trecEvalOutputPath"), true);
          BufferedWriter writer = new BufferedWriter(fw);
          DecimalFormat dFormat = new DecimalFormat("#0.000000000000");
          if(r.size()==0){
        	  fw.write(qid + "\tQ0\tdummy\t1\t0\trun-1\n");
          }
          
          for (int i = 0; i < r.size(); i++){
          if(i>length-1)
        	  break;
         
          //System.out.println(qid+'\t'+"Q0"+'\t'+Idx.getExternalDocid(r.getDocid(i))+'\t'+(i+1)+'\t'+r.getDocidScore(i)+"\t"+"fubar");
          writer.write(qid+'\t'+"Q0"+'\t'+Idx.getExternalDocid(r.getDocid(i))+'\t'+(i+1)+'\t'+dFormat.format(r.getDocidScore(i))+"\t"+"fubar");
		  //if(i<r.size()-1)
		  writer.newLine();
          }
          writer.close();
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
    }
  }

  private static Map<String, Double> doaSort(HashMap<String, Double> termScore, int limit) {
	 
	  Map<String,Double> topTen =
			  termScore.entrySet().stream()
			       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			       .limit(limit)
			       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	  

	    return topTen;	
}

private static double calcScore(String qterm, int docid, Double scoreDoc, int i) throws IOException {
	TermVector tv = new TermVector(docid, "body");
	int tf = 0;
	  int index = tv.indexOfStem(qterm);
	  if(index!=-1){
		  tf = tv.stemFreq(index);
	  }
	Double ptd = (double) tf/ (double)Idx.getFieldLength("body", docid);
	  
	  double idfLikeScore = Math.log( (double)Idx.getSumOfFieldLengths("body")/ (double)Idx.getTotalTermFreq("body", qterm)); 
	  
	 
	  return ptd*scoreDoc*idfLikeScore;
  }

/**
   * Print the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
   * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryName, ScoreList result) throws IOException {

    System.out.println(queryName + ":  ");
    if (result.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.size(); i++) {
        System.out.println("\t" + i + ":  " + Idx.getExternalDocid(result.getDocid(i)) + ", "
            + result.getDocidScore(i));
      }
    }
  }

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }


  
}
