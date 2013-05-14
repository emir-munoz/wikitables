package org.deri.wiki.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;


public class KappaStatistic {
	public static final String[] COMMON_FNS = new String[]{
		"data/wiki-eval-all_AH.txt",
		"data/wiki-eval-all_AM.txt",
		"data/wiki-eval-all_EM.txt"
	};
	
	public static final int NO_VAL = Integer.MIN_VALUE;
	
//	public static final int[][] MAP_VAL = null;
	public static final int[][] MAP_VAL = {{0,-1}}; //list of pairs
	
	public static HashMap<Integer,Integer> VAL_MAP = null;
	static{
		if(MAP_VAL!=null){
			VAL_MAP = new HashMap<Integer,Integer>();
			for(int[] ia:MAP_VAL){
				if(ia.length==2){
					VAL_MAP.put(ia[0],ia[1]);
				}
			}
		}
	}
	
	static Logger _log = Logger.getLogger(KappaStatistic.class.getName());
	
	public static void main(String[] args) throws IOException{
		ArrayList<ArrayList<Result>> common = new ArrayList<ArrayList<Result>>();
		for(String fn:COMMON_FNS){
			common.add(load(fn));
		}
		
		int size = -1;
		for(ArrayList<Result> results:common){
			if(size==-1) size = results.size();
			else if(size!=results.size()) throw new IOException("Results files not the same size!");
		}

		
		ArrayList<ArrayList<Integer>> comp = new ArrayList<ArrayList<Integer>>();
		Count<ArrayList<Integer>> agree_dist = new Count<ArrayList<Integer>>();
		ArrayList<Count<Integer>> judge_dist = new ArrayList<Count<Integer>>();
		HashSet<Integer> vals = new HashSet<Integer>();
		
		for(int i=0;i<common.size(); i++){
			judge_dist.add(new Count<Integer>());
		}
		
		for(int i=0; i<size; i++){
			ArrayList<Integer> result = new ArrayList<Integer>(common.size());
			for(int j=0; j<common.size(); j++){
				int val = common.get(j).get(i).val;
				result.add(val);
				judge_dist.get(j).add(val);
				vals.add(val);
			}
			comp.add(result);
			agree_dist.add(result);
		}
		
		System.err.println("=== Agree Dist ===");
		agree_dist.printStats(System.err);
		
		int agree = 0;
		ArrayList<Integer> consensus = new ArrayList<Integer>();
		for(ArrayList<Integer> c:comp){
			Count<Integer> max = new Count<Integer>();
			boolean disagree = false;
			max.add(c.get(0));
			for(int i=1;i<c.size();i++){
				if(!c.get(i).equals(c.get(i-1))){
					disagree = true;
					max.add(c.get(i));
				}
			}
			if(!disagree){
				agree++;
			}
			// order based on key if tied!!!
			consensus.add(max.getOccurrenceOrderedEntries().first().getKey());
		}
		
		System.err.println("=== Consensus ===");
		for(int c:consensus){
			System.err.println(c);
		}
		
		System.err.println("* Agree "+agree);
		double pra = (double)agree/(double)size;
		System.err.println("* ALL Pr (a) = "+pra);
		
		double pre = 0;
		for(Integer val:vals){
			//probability of random agreement for that value
			double prv = 1;
			for(int i=0; i<judge_dist.size(); i++){
				Count<Integer> jd = judge_dist.get(i);
				Integer c = jd.get(val);
				if(c==null) c = 0;
				double prv_j = (double)c/(double)size;;
				System.err.println("  * J-"+i+" Pr (val="+val+") = "+prv_j);
				prv*=prv_j;
			}
			System.err.println(" * ALL Pr (val="+val+") = "+prv);
			pre+=prv;
		}
		
		System.err.println("* ALL Pr (e) = "+pre);
		
		double kappa = (pra-pre)/(1d-pre);
		System.err.println("Kappa "+kappa);
	}
	
//	private static <E> String toString(E[] ea){
//		StringBuffer buf = new StringBuffer();
//		for(E e:ea)
//			buf.append(e.toString()+"\t");
//		return buf.toString().trim();
//	}
	
	private static ArrayList<Result> load(String resultsFile) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(resultsFile));
		String line = null;
		
		ArrayList<Result> results = new ArrayList<Result>();
		
		while((line=br.readLine())!=null){
			line = line.trim();
			if(!line.isEmpty() && !line.startsWith("source")){
				String[] split = line.split("\t");
				if(split.length<4){
					throw new IOException("Cannot read line (expecting four or five columns) "+line);
				}
				int val = NO_VAL;
				if(split.length>4){
					val = Integer.parseInt(split[4]);
				}
				
				if(VAL_MAP!=null){
					Integer map_val = VAL_MAP.get(val);
					if(map_val!=null)
						val = map_val;
				}
				
				Result r = new Result(split[0],split[1],split[2],split[3],val);
				
				results.add(r);
			}
		}
		br.close();
		return results;
	}
	
	public static class Result{
		String sub;
		String pred;
		String obj;
		String source;
		int val;
		
		public Result(String source, String s, String p, String o, int val){
			this.source = source;
			this.sub = s;
			this.pred = p;
			this.obj = o;
			this.val = val;
		}
	}
}
