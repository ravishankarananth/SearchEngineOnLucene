/*
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.beans.FeatureDescriptor;
import java.io.*;
import java.security.Policy.Parameters;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.classic.QueryParser;




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

		if (model instanceof RetrievalModelLetor) {

			String execPath = parameters.get("letor:svmRankLearnPath");
			String qrelsFeatureOutputFile = parameters.get("letor:trainingFeatureVectorsFile");
			String modelOutputFile = parameters.get("letor:svmRankModelFile");
			String cValue = parameters.get("letor:svmRankParamC");
			System.out.println("start train");
			HashMap<String, Double> pagerankMap = new HashMap<>();
			
			BufferedReader pagerankreader = new BufferedReader(new FileReader(parameters.get("letor:pageRankFile")));
			String pager = "";
			
			while ((pager = pagerankreader.readLine()) != null) {
				String[] pagesplit = pager.split("\\s+");		
				
					pagerankMap.put( pagesplit[0], Double.parseDouble(pagesplit[1]));			
			
			}
			pagerankreader.close();
			
			trainSVM(model,parameters, pagerankMap);
			System.out.println("done feature vector creation");
			RunCommandTrain(execPath, qrelsFeatureOutputFile, modelOutputFile, cValue);
			System.out.println("done training");
			getBM25(parameters, model, pagerankMap);
			System.out.println("done testing vectors");
			RunCommandtest (parameters.get("letor:svmRankClassifyPath"),  parameters.get("letor:testingFeatureVectorsFile"), parameters.get("letor:svmRankModelFile"), parameters.get("letor:testingDocumentScores"));
			System.out.println("done test running");
			int limit =100;
			if(parameters.containsKey("trecEvalOutputLength")) {
				limit = Integer.parseInt(parameters.get("trecEvalOutputLength"));			}
			CombineScores (parameters.get("queryFilePath"), parameters.get("letor:testingFeatureVectorsFile"), parameters.get("letor:testingDocumentScores"), limit, parameters.get("trecEvalOutputPath"));
			System.out.println("combined and done");
			//RunCommandtest(parameters.get("letor:svmRankClassifyPath"), parameters.get("letor:testingFeatureVectorsFile"), parameters.get("letor:testingDocumentScores"), cValue);
			//test()
			//write()
		}else{
			//  Perform experiments.

			processQueryFile(parameters.get("queryFilePath"), model, parameters);

			//  Clean up.
		}
		timer.stop ();
		System.out.println ("Time:  " + timer);
	}



	private static void CombineScores(String queryPath, String testingVectors, String ScorePath, int limit, String evalOutputPath) throws Exception {
		BufferedReader input = new BufferedReader(new FileReader(queryPath));

		//  Each pass of the loop processes one query.
		String qLine = null;
		while ((qLine = input.readLine()) != null) {
			int d = qLine.indexOf(':');

			if (d < 0) {
				throw new IllegalArgumentException
				("Syntax error:  Missing ':' in query line.");
			}

			printMemoryUsage(false);

			String Qid = qLine.substring(0, d);
			String Query = qLine.substring(d + 1);
			BufferedReader inputVector = new BufferedReader(new FileReader(testingVectors));
			BufferedReader inputScore = new BufferedReader(new FileReader(ScorePath));
			String qVector = null;
			String qScore = null;
			ScoreList r = new ScoreList();
			while(((qVector = inputVector.readLine()) != null) && ((qScore = inputScore.readLine()) != null)){
				
				String[] vectorComp = qVector.split("\\s+");
				
				String[] qidtemp = vectorComp[1].split(":");				
				if(qidtemp[1].equals(Qid)){
					String[] docIDSplit = qVector.trim().split("#");
					int internalID = Idx.getInternalDocid(docIDSplit[1].trim());
					
					r.add(internalID, Double.parseDouble(qScore));
				
			}
			}
			r.sort();
			if (r != null) {
				//printResults(qid, r);
				//
				//System.out.println();
				
				
				FileWriter fw = new FileWriter(evalOutputPath, true);
				BufferedWriter writer = new BufferedWriter(fw);
				DecimalFormat dFormat = new DecimalFormat("#0.000000000000");
				if(r.size()==0){
					fw.write(Qid + "\tQ0\tdummy\t1\t0\trun-1\n");
				}

				for (int i = 0; i < r.size(); i++){
					if(i>limit-1)
						break;

					//System.out.println(qid+'\t'+"Q0"+'\t'+Idx.getExternalDocid(r.getDocid(i))+'\t'+(i+1)+'\t'+r.getDocidScore(i)+"\t"+"fubar");
					writer.write(Qid+'\t'+"Q0"+'\t'+Idx.getExternalDocid(r.getDocid(i))+'\t'+(i+1)+'\t'+dFormat.format(r.getDocidScore(i))+"\t"+"fubar");
					//if(i<r.size()-1)
					writer.newLine();
				}
				writer.close();

			}
			inputVector.close();
			inputScore.close();
			
		}
		input.close();
	}



	private static void RunCommandtest(String execPath, String testingFeatures, String ModelPath, String OutPutScore) throws Exception {
		
		 // runs svm_rank_learn from within Java to train the model
	    // execPath is the location of the svm_rank_learn utility, 
	    // which is specified by letor:svmRankLearnPath in the parameter file.
	    // FEAT_GEN.c is the value of the letor:c parameter.
	    Process cmdProc = Runtime.getRuntime().exec(
	        new String[] { execPath, testingFeatures, ModelPath, OutPutScore });

	    // The stdout/stderr consuming code MUST be included.
	    // It prevents the OS from running out of output buffer space and stalling.

	    // consume stdout and print it out for debugging purposes
	    BufferedReader stdoutReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getInputStream()));
	    String line;
	    while ((line = stdoutReader.readLine()) != null) {
	      System.out.println(line);
	    }
	    // consume stderr and print it for debugging purposes
	    BufferedReader stderrReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getErrorStream()));
	    while ((line = stderrReader.readLine()) != null) {
	      System.out.println(line);
	    }

	    // get the return value from the executable. 0 means success, non-zero 
	    // indicates a problem
	    int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }

		
	}



	private static void getBM25(Map<String, String> parameters, RetrievalModel model, HashMap<String, Double> pagerankMap ) throws Exception {
		
		RetrievalModel model2 = new RetrievalModelOkapiBM25(Double.parseDouble(parameters.get("BM25:k_1")), Double.parseDouble(parameters.get("BM25:b")), Double.parseDouble(parameters.get("BM25:k_3")));
		BufferedReader input = null;
		String qLine = null;
		input = new BufferedReader(new FileReader(parameters.get("queryFilePath")));

		//  Each pass of the loop processes one query.

		while ((qLine = input.readLine()) != null) {
			int d = qLine.indexOf(':');

			if (d < 0) {
				throw new IllegalArgumentException
				("Syntax error:  Missing ':' in query line.");
			}

			printMemoryUsage(false);

			String Qid = qLine.substring(0, d);
			String Query = qLine.substring(d + 1);

			ScoreList r1=processQuery(Query, model2);
			int limit =100;
			if(parameters.containsKey("trecEvalOutputLength")){
				limit = Integer.parseInt(parameters.get("trecEvalOutputLength"));
			}
			r1.truncate(limit);
		
			
			String t[] = QryParser.tokenizeString(Query);
			
			//max and min array for normalize
			Double[] maxvalues = new Double[18];
			Arrays.fill(maxvalues, -9999.0);
			Double[] minvalues = new Double[18];
			Arrays.fill(minvalues, Double.MAX_VALUE);
			
			ArrayList<ArrayList<Double>>featureVector = new ArrayList<>(); 
			//create the features
			for (int i = 0; i < r1.size(); i++){ //iterate through docList
				
				ArrayList<Double> fVector = new ArrayList<>();
				//create feature vector for q in d
				
				fVector = createFeatures( r1.getDocid(i), Idx.getExternalDocid(r1.getDocid(i)), 0, parameters, t, model, pagerankMap);
				//if(i<r.size()-1)
				
				fVector.add((double)r1.getDocid(i));
				//find min and max
				for (int j=0; j< fVector.size()-1; j++){
					
					if( (fVector.get(j)!=(-1*Double.MAX_VALUE))){
						if( fVector.get(j) > maxvalues[j]){
							maxvalues[j] = fVector.get(j); }

						if( fVector.get(j) < minvalues[j]){
							minvalues[j] = fVector.get(j); 
						}
					}


				}
				
				featureVector.add(fVector);
			}

			//normalize the features
			for (ArrayList<Double> fV : featureVector){
				for(int i=0; i<fV.size()-1; i++){
					double norm =0.0;
					if( (fV.get(i)!=(-1*Double.MAX_VALUE)) && (maxvalues[i]!=minvalues[i])) {
						norm = (fV.get(i)-minvalues[i])/(maxvalues[i]-minvalues[i]);
					}	        		
					fV.set(i, norm);}

			}
			
			ArrayList<Integer> disabledvectors = null;
			if(parameters.containsKey("letor:featureDisable")){
				disabledvectors = new ArrayList<>();
				String[] numbers = parameters.get("letor:featureDisable").split(",");
				for(String temp: numbers)
				disabledvectors.add(Integer.parseInt(temp));
			}
			
			//write to file	        	
			FileWriter fw = new FileWriter(parameters.get("letor:testingFeatureVectorsFile"), true);
			BufferedWriter writer2 = new BufferedWriter(fw);
			for (ArrayList<Double> fV : featureVector){
				String featureString = new String(0 + "\t" + "qid:" +Qid + "\t");

				
				for(int i=1; i<19;i++){
					//if( (i>3 && fV.get(i)!=0.0) || i<=3)
					if(disabledvectors==null){
					featureString = featureString + i+":"+fV.get(i-1)+'\t';
					}
					else{
						if(!disabledvectors.contains(i)){
							featureString = featureString + i+":"+fV.get(i-1)+'\t';
						}
					}
				}
				featureString = featureString + "#" + '\t' + Idx.getExternalDocid(fV.get(fV.size()-1).intValue());

				writer2.write(featureString);
				writer2.newLine();
			}
			writer2.close();


			
		}
		input.close();
		
		
	}
	

	private static void RunCommandTrain(String execPath, String qrelsFeatureOutputFile, String modelOutputFile, String valueC) throws Exception {
		
	    // runs svm_rank_learn from within Java to train the model
	    // execPath is the location of the svm_rank_learn utility, 
	    // which is specified by letor:svmRankLearnPath in the parameter file.
	    // FEAT_GEN.c is the value of the letor:c parameter.
	    Process cmdProc = Runtime.getRuntime().exec(
	        new String[] { execPath, "-c", String.valueOf(valueC), qrelsFeatureOutputFile,
	            modelOutputFile });

	    // The stdout/stderr consuming code MUST be included.
	    // It prevents the OS from running out of output buffer space and stalling.

	    // consume stdout and print it out for debugging purposes
	    BufferedReader stdoutReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getInputStream()));
	    String line;
	    while ((line = stdoutReader.readLine()) != null) {
	      System.out.println(line);
	    }
	    // consume stderr and print it for debugging purposes
	    BufferedReader stderrReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getErrorStream()));
	    while ((line = stderrReader.readLine()) != null) {
	      System.out.println(line);
	    }

	    // get the return value from the executable. 0 means success, non-zero 
	    // indicates a problem
	    int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
		
	}

	private static void trainSVM(RetrievalModel model, Map<String, String> parameters, HashMap<String, Double> pagerankMap) throws Exception {
		String trainQueryFilePath = parameters.get("letor:trainingQueryFile");
		BufferedReader input = null;

		try {
			String qLine = null;

			input = new BufferedReader(new FileReader(trainQueryFilePath));

			//  Each pass of the loop processes one query.

			while ((qLine = input.readLine()) != null) {
				int d = qLine.indexOf(':');

				if (d < 0) {
					throw new IllegalArgumentException
					("Syntax error:  Missing ':' in query line.");
				}

				printMemoryUsage(false);

				String trainQid = qLine.substring(0, d);
				String trainQuery = qLine.substring(d + 1);

				String t[] = QryParser.tokenizeString(trainQuery);

				System.out.println("tokenized");
				String judgementFilePath = parameters.get("letor:trainingQrelsFile");
				BufferedReader input2 = new BufferedReader(new FileReader(judgementFilePath));
				String trainDocs = "";
				ArrayList<ArrayList<Double>>featureVector = new ArrayList<>(); 
				//max and min array for normalize
				Double[] maxvalues = new Double[18];
				Arrays.fill(maxvalues, -9999.0);
				Double[] minvalues = new Double[18];
				Arrays.fill(minvalues, Double.MAX_VALUE);
				//ArrayList<Integer> disabledList = new ArrayList<>();
				while((trainDocs = input2.readLine()) != null){
					String[] splitDoc = trainDocs.split("\\s+"); 

					if(splitDoc[0].equals(trainQid)){
						
						int docID = Idx.getInternalDocid(splitDoc[2]);
						if(docID==0){
							//no internal docID
							continue;
						}
						int relevance = Integer.parseInt(splitDoc[3])+3;

						ArrayList<Double> fVector = new ArrayList<>();
						//create feature vectore for q in d
						fVector = createFeatures( docID, splitDoc[2], relevance, parameters, t, model, pagerankMap);
						fVector.add((double)relevance);
						fVector.add((double)docID);
						//find min and max
						for (int i=0; i< fVector.size()-2; i++){

							if( (fVector.get(i) !=(-1*Double.MAX_VALUE))){
								if( fVector.get(i) > maxvalues[i]){
									maxvalues[i] = fVector.get(i); }

								if( fVector.get(i) < minvalues[i]){
									minvalues[i] = fVector.get(i); 
								}
							}


						}
						
						
						featureVector.add(fVector);
					}}



				//normalize
				double minimumval = -1*Double.MAX_VALUE;
				for (ArrayList<Double> fV : featureVector){					
					for(int i=0; i<fV.size()-2; i++){						
						double norm =0.0;
						if( (fV.get(i) != minimumval) && (maxvalues[i]!=minvalues[i])) {
							
							norm = (fV.get(i)-minvalues[i])/(maxvalues[i]-minvalues[i]);
						}	        		
						fV.set(i, norm);}

				}
				
				ArrayList<Integer> disabledvectors = null;
				if(parameters.containsKey("letor:featureDisable")){
					disabledvectors = new ArrayList<>();
					String[] numbers = parameters.get("letor:featureDisable").split(",");
					for(String temp: numbers)
					disabledvectors.add(Integer.parseInt(temp));
				}

				//write to file	        	
				FileWriter fw = new FileWriter(parameters.get("letor:trainingFeatureVectorsFile"), true);
				BufferedWriter writer2 = new BufferedWriter(fw);
				for (ArrayList<Double> fV : featureVector){
					String featureString = new String(fV.get(fV.size()-2).intValue() + "\t" + "qid:" +trainQid + "\t");

					//writer2.write(fV.get(fV.size()-1).intValue() + '\t' + "qid:"+trainQid + '\t');
					for(int i=1; i<19;i++){
						//if( (i>3 && fV.get(i)!=0.0) || i<=3)
						if(disabledvectors==null){
						featureString = featureString + i+":"+fV.get(i-1)+'\t';
						}
						else{
							if(!disabledvectors.contains(i)){
								featureString = featureString + i+":"+fV.get(i-1)+'\t';
							}
						}
					}
					featureString = featureString + "#" + '\t' + Idx.getExternalDocid(fV.get(fV.size()-1).intValue());

					writer2.write(featureString);
					writer2.newLine();
				}
				writer2.close();




			}} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				input.close();
			}


	}

	private static ArrayList<Double> createFeatures(int docID, String externalDocID, int relevance, Map<String, String> parameters, String[] query, RetrievalModel model, HashMap<String, Double> pagerankMap) throws Exception {

		ArrayList<Double> featuresScore = new ArrayList<>();
		//f1 - spamscore
		
		double spamscore =  Double.parseDouble(Idx.getAttribute ("score", docID));
		featuresScore.add(spamscore);
		
		//f2 - url depth
	
		String rawUrl = Idx.getAttribute ("rawUrl", docID);
		double urlDepth = rawUrl.length() - rawUrl.replace("/", "").length();
		if(rawUrl.contains("http")){
			urlDepth = urlDepth-2;
		}
		featuresScore.add(urlDepth);
	
		//f3- wikipedia score
		if(rawUrl.contains("wikipedia.org")){
			featuresScore.add(1.0);
		}else {
			featuresScore.add(0.0);
		}
		//f4 - pagerank score
	
		double pagerank = -1*Double.MAX_VALUE;
		if(pagerankMap.containsKey(externalDocID)){
			pagerank=pagerankMap.get(externalDocID);
		} 
		featuresScore.add(pagerank);
	
		

		//f5 - bm25 score body
		
		double bm25score = calcbm25score(docID, query, (RetrievalModelLetor)model, "body");
		featuresScore.add(bm25score);
		
		//f6 - Indri score body
		
		double indriscore = calcIndriScore(docID, query, (RetrievalModelLetor)model, "body");
		featuresScore.add(indriscore);
		
		//f7 - term overlap score
		
		double termOverlapScore = calcOverlapScore(docID,query, "body");
		featuresScore.add(termOverlapScore);
		
		//f8 - bm25Title
		double bm25Ttile = calcbm25score(docID, query, (RetrievalModelLetor)model, "title");
		featuresScore.add(bm25Ttile);
		//f9 - Indri score title
		double indriscoretitle = calcIndriScore(docID, query, (RetrievalModelLetor)model, "title");
		featuresScore.add(indriscoretitle);
		// f10 - term overlap score title
		double termOverlapScoretitle = calcOverlapScore(docID,query, "title");
		featuresScore.add(termOverlapScoretitle);


		//f11 - bm25Url
		double bm25Url = calcbm25score(docID, query, (RetrievalModelLetor)model, "url");
		featuresScore.add(bm25Url);
		//f12 - Indri score Url
		double indriscoreurl = calcIndriScore(docID, query, (RetrievalModelLetor)model, "url");
		featuresScore.add(indriscoreurl);
		// f13 - term overlap score url
		double termOverlapScoreUrl = calcOverlapScore(docID,query, "url");
		featuresScore.add(termOverlapScoreUrl);


		//f14 - bm25Inlink
		double bm25Inlink = calcbm25score(docID, query, (RetrievalModelLetor)model, "inlink");
		featuresScore.add(bm25Inlink);
		//f15 - Indri score Inlink
		double indriscoreInlink = calcIndriScore(docID, query, (RetrievalModelLetor)model, "inlink");
		featuresScore.add(indriscoreInlink);
		// f16 - term overlap score Inlink
		double termOverlapScoreInlink = calcOverlapScore(docID,query, "inlink");
		featuresScore.add(termOverlapScoreInlink);

		// f17 - tf idf
		double tfidfScore = calctfIdfScore(docID, query, "title");
		featuresScore.add(tfidfScore);
		
		// f18 - indri*page rank
		double tfPagerank=0.0;
		
		//double sumoftf = calctf(docID, query, "title");
		if((indriscore!=-1*Double.MAX_VALUE) && (pagerank!=-1*Double.MAX_VALUE) )
			tfPagerank = pagerank*indriscore;
		else
			tfPagerank = -1*Double.MAX_VALUE;
		featuresScore.add(tfPagerank);
		//featuresScore.add(sumoftf);

		return featuresScore;

	}

	


	private static double calctf(int docID, String[] query, String field) throws IOException {
		double tf =0.0;
		TermVector td = new TermVector(docID, field);
		if(td.getLuceneTerms()==null){
			return -1*Double.MAX_VALUE;
		}
		
		
		for (String qryterm : query){			
		int index = td.indexOfStem(qryterm);
		if(index!=-1)
			tf+=(td.stemFreq(index));
		}
		return tf;
	}



	private static double calctfIdfScore(int docID, String[] query, String field) throws IOException {
		double tfidf =0.0;
		TermVector td = new TermVector(docID, field);
		if(td.getLuceneTerms()==null){
			return -1*Double.MAX_VALUE;
		}
		
		//double idf = 
		for (String qryterm : query){
			
		int index = td.indexOfStem(qryterm);
		//sumTf+=(td.stemFreq(index)+1);
		//sumSqTf+=((td.stemFreq(index)+1)*(td.stemFreq(index)+1));
		double tf = 0.0;
		double idf = 0.0;
		if(index!=-1){
		tf = td.stemFreq(index);
		idf = Math.log(Idx.getDocCount(field)/td.stemDf(index));
		}
			tfidf+=(tf*idf);
		}
		return tfidf;
		
	}



	private static double calcOverlapScore(int docID, String[] query, String field) throws IOException {
		// calculate the term overlap score
		TermVector td = new TermVector(docID, field);
		if(td.getLuceneTerms()==null){
			return -1*Double.MAX_VALUE;
		}
		double length = query.length;
		double count=0;
		for (String qryterm : query){

			int index = td.indexOfStem(qryterm);
			if(index!=-1){
				count++;
			}
		}
		return count*100/length;
	}

	private static double calcIndriScore(int docID, String[] query, RetrievalModelLetor model, String field) throws IOException {
		// function to calculate the Indri score
		double product =1.0;
		double lengthofQuery = (double)query.length;
		int counter = (int)lengthofQuery;
		double gmean = 1.0/lengthofQuery;
		TermVector td = new TermVector(docID, field);
		if(td.getLuceneTerms() == null){
			return -1*Double.MAX_VALUE;
		}
		for (String qryterm : query){
			int index = td.indexOfStem(qryterm);
			double termfreq =0.0;
			if(index!=-1){
				counter--;
				termfreq = td.stemFreq(index);
			}
			double sizeOfdoc = Idx.getFieldLength( field, docID);
			double lengthofC = Idx.getSumOfFieldLengths(field);
			double ctf = Idx.getTotalTermFreq(field, qryterm);
			double OneminusLamda = (1-model.getIdLambda());
			double prior = (ctf/lengthofC);
			double lengthAndMu = (sizeOfdoc+model.getIdMu());

			double score = OneminusLamda * (termfreq + model.getIdMu()*prior)/lengthAndMu;
			score = score + (model.getIdLambda()*prior);
			product = product * Math.pow(score,gmean);  
		}

		if(counter==lengthofQuery){
			return 0.0;
		}

		return product;
	}

	private static double calcbm25score(int docID, String[] query, RetrievalModelLetor model, String field) throws IOException {
		// function to calculate the bm25 score
		TermVector td = new TermVector(docID, field);

		if(td.getLuceneTerms() == null){
			return -1*Double.MAX_VALUE;
		}
		double sum =0.0;
		for (String qryterm : query){

			int index = td.indexOfStem(qryterm);
			if(index!=-1){
				double rsjweight = (Idx.getNumDocs() - td.stemDf(index) + 0.5)/ (td.stemDf(index)+0.5);
				rsjweight = Math.log(rsjweight);
				double termfreq = td.stemFreq(index);
				double sizeOfdoc = Idx.getFieldLength( field, docID);
				double averageLength =  Idx.getSumOfFieldLengths(field) / (double)Idx.getDocCount (field);

				double innervalue = (1-model.bm25_b) + (model.bm25_b * ( sizeOfdoc / averageLength) ); 
				double tfweight = termfreq/(termfreq + (model.bm25_k1 * innervalue));
				sum = sum+ rsjweight*tfweight; }

		}

		return sum;
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
		else if(modelString.equals("letor")){

			model =  new RetrievalModelLetor(Double.parseDouble(parameters.get("BM25:k_1")), Double.parseDouble(parameters.get("BM25:b")), Double.parseDouble(parameters.get("BM25:k_3")), Double.parseDouble(parameters.get("Indri:lambda")), Double.parseDouble(parameters.get("Indri:mu")), Double.parseDouble(parameters.get("letor:svmRankParamC")));
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
						HashSet<String> qterms = new HashSet<String>(); // Set with terms
						if (((RetrievalModelIndri) model).getFbInitialRankingFile()==null ){

							//feedback is true
							r = processQuery(query, model);

						} else {


							String initfilePath = ((RetrievalModelIndri) model).getFbInitialRankingFile();
							r = new ScoreList();
							try (BufferedReader br = new BufferedReader(new FileReader(initfilePath))) {

								String line;
								int count = (int) ((RetrievalModelIndri) model).getFbDocs();
								while (((line = br.readLine()) != null) && count!=0) {

									String[] parts = line.split("\\s+");

									if(parts[0].equals(qid)){       						
										count--;	

										int dID = Idx.getInternalDocid(parts[2]);

										double scorePart = Double.parseDouble(parts[4]);
										r.add(dID, scorePart);
									} 
								}
								br.close();
							}

							catch (Exception e) {
								e.printStackTrace();
							} 


							r.sort();
							r.truncate((int) ((RetrievalModelIndri) model).getFbDocs());
						}
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
						tv = null;

						HashMap<String, Double> termScore = new HashMap<>();
						for (String qterm : qterms){

							double score =0.0;
							for(int i =0; i< ((RetrievalModelIndri) model).getFbDocs(); i++){
								score += calcScore(qterm, r.getDocid(i), r.getDocidScore(i), i , ((RetrievalModelIndri) model).getFbMu());
							}
							termScore.put(qterm, score);

						}

						Map<String,Double> topTen = doaSort(termScore, (int)((RetrievalModelIndri) model).getFbTerms());
						termScore = null;
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

						System.out.println("----> this is the expanded query \n"+expandedQuery);        	
						FileWriter fw = new FileWriter(parameters.get("fbExpansionQueryFile"), true);
						BufferedWriter writer = new BufferedWriter(fw);
						writer.write(qid+":\t"+expandedQuery);
						writer.newLine();
						writer.close();
						double weight = ((RetrievalModelIndri) model).fbOrinWeight;

						String newQuery = "#wand ( " + weight + " #and(" + query + ") " + (1-weight) + " " + expandedQuery + ")\n";


						r = processQuery(newQuery, model);
						System.out.println("----> this is the new query \n"+newQuery);

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
				QryParser.testweight.clear();
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

	private static double calcScore(String qterm, int docid, Double scoreDoc, int i, double muVal) throws IOException {
		TermVector tv = new TermVector(docid, "body");
		int tf = 0;
		int index = tv.indexOfStem(qterm);
		if(index!=-1){
			tf = tv.stemFreq(index);
		}
		Double ptc =  (double)Idx.getTotalTermFreq("body", qterm)/(double)Idx.getSumOfFieldLengths("body");
		Double ptd = (double) ((double)tf + (muVal*ptc))/ (double)((double)Idx.getFieldLength("body", docid)+muVal);

		double idfLikeScore = Math.log(1.0/ptc); 


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
