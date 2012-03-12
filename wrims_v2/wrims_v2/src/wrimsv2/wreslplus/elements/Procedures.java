package wrimsv2.wreslplus.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;



import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.wreslparser.elements.LogUtils;

public class Procedures {
	
	
	private Procedures(){}


	public static void processGoalHS(StudyTemp s){
					
			for (String m: s.modelList){			
	
				ModelTemp mObj = s.modelMap.get(m);
				
				for (String gKey: mObj.gl2Map.keySet()){			
					
					GoalTemp g2 = mObj.gl2Map.get(gKey);
					
					for (int i=0; i<g2.caseName.size(); i++){
						
						String cn = g2.caseName.get(i); 
						GoalCase gc = g2.caseMap.get(cn);
						g2.caseCondition.add(gc.condition);
						
						// convert penalty into caseExpression
						Map<String,String> o = convertPenalty(g2.id, i, g2.lhs, gc);
						
						String slackName = o.get("slackName");
						String surplusName = o.get("surplusName");
						//System.out.println(slackName);
						//System.out.println(surplusName);
						
						g2.slackList.add(slackName);
						g2.surplusList.add(surplusName);
						g2.caseExpression.add(o.get("caseExpression"));	
						
						
						if (slackName!=null) {
							WeightTemp w = new WeightTemp();
							w.weight = o.get("slackWeight");
							
							DvarTemp d = new DvarTemp();
							d.fromWresl = g2.fromWresl;
							d.kind = "slack";
							
							if (g2.hasCase) {
								d.condition="conditional";
								w.condition="conditional";
							}
							//System.out.println(slackName+":"+g2.ruleType+":"+d.condition);
							mObj.ssList.add(slackName);
							mObj.ssMap.put(slackName, d);
							mObj.ssWeightMap.put(slackName, w);
						}
						if (surplusName!=null) {
							WeightTemp w = new WeightTemp();
							w.weight = o.get("surplusWeight");
							
							DvarTemp d = new DvarTemp();
							d.fromWresl = g2.fromWresl;
							d.kind = "surplus";
							
							
							if (g2.hasCase) {
								d.condition="conditional";
								w.condition="conditional";
							}
							//System.out.println(surplusName+":"+g2.ruleType+":"+d.condition);
							
							mObj.ssList.add(surplusName);
							mObj.ssMap.put(surplusName, d);
							mObj.ssWeightMap.put(surplusName, w);
						}
						
					}
					
					mObj.glMap.put(gKey, g2);
					
				}			
				
			}
			
			
	//		for (String m: s.modelList){			
	//
	//			ModelTemp mObj = s.modelMap.get(m);
	//			
	//		    for (String t: mObj.glMap.keySet()){
	//		    	
	//		    	System.out.println(mObj.glMap.get(t).id);
	//		    	System.out.println(mObj.glMap.get(t).fromWresl);
	//		    	
	//		    }
	//			
	//		}
	
		}


//	public static void processSlackSurplus(StudyTemp s){
//				
//		for (String m: s.modelList){			
//
//			ModelTemp mObj = s.modelMap.get(m);
//			
//			mObj.dvList.addAll(mObj.ssList);
//			mObj.dvMap.putAll(mObj.ssMap);
//			
//				
//			}			
//			
//	}
		

	
	
	private static Map<String,String> convertPenalty(String goalName, int caseIndex, String lhs, GoalCase cm) {
		
		String caseNumber = Integer.toString(caseIndex+1);
		String caseExpression=null;
		String lhs_m=null;
		//String rhs_m=null;
		String relation=null;		
		
		String lt = cm.lhs_lt_rhs;
		String gt = cm.lhs_gt_rhs;		
		
		String slackName = null;
		String surplusName = null;
		String slackWeight = null;
		String surplusWeight = null;
		
		Map<String,String> expression_slack_surplus = new HashMap<String,String>();
		
		if ( isConstrain(gt) && isConstrain(lt)) {

			lhs_m = lhs; 
			relation = "=";
			caseExpression = lhs_m + relation + cm.rhs; 
			
		} else if ( isConstrain(gt)) {
						
			if ( isFree(lt)) {
			
				lhs_m = lhs;
				relation = "<";
				caseExpression = lhs_m + relation + cm.rhs; 
				
			} else {
				
				slackName = "slack__"+goalName+"_"+caseNumber;  
				slackWeight = lt ;
				lhs_m = lhs + "+" + slackName;
				relation = "=";
				caseExpression = lhs_m + relation + cm.rhs; 
			}
			
		} else if ( isConstrain(lt)) {
			
			if ( isFree(gt)) {

				lhs_m = lhs;
				relation = ">";
				caseExpression = lhs_m + relation + cm.rhs; 
	
			} else {
	
				surplusName = "surplus__"+goalName+"_"+caseNumber;  
				surplusWeight = gt;
				lhs_m = lhs + "-" + surplusName;
				relation = "=";
				caseExpression = lhs_m + relation + cm.rhs; 
			}

		} else if ( isFree(gt) && isFree(lt)){
			
			
			caseExpression = " 1 > 0 "; 

			
		} else if ( isFree(lt)){
						
			surplusName = "surplus__"+goalName+"_"+caseNumber; 
			surplusWeight = gt;
			lhs_m = lhs + "-" + surplusName;
			relation = "<";
			caseExpression = lhs_m + relation + cm.rhs; 
			
		} else if ( isFree(gt)){
			
			slackName = "slack__"+goalName+"_"+caseNumber;
			slackWeight = lt;
			lhs_m = lhs + "+" + slackName;
			relation = ">";
			caseExpression = lhs_m + relation + cm.rhs; 
			
		} else {
			
			surplusName = "surplus__"+goalName+"_"+caseNumber; 
			surplusWeight = gt;
			lhs_m = lhs + "-" + surplusName;
			slackName = "slack__"+goalName+"_"+caseNumber;
			slackWeight = lt;
			lhs_m = lhs_m + "+" + slackName;
			relation = "=";
			caseExpression = lhs_m + relation + cm.rhs; 
		}
				
		
		if (isNumber(slackWeight)) {
			slackWeight="-"+slackWeight;
		} else {
			slackWeight="-("+slackWeight+")";
		}
		
		if (isNumber(surplusWeight)) {
			surplusWeight="-"+surplusWeight;
		} else {
			surplusWeight="-("+surplusWeight+")";
		}
		
		expression_slack_surplus.put("caseExpression",caseExpression);
		expression_slack_surplus.put("slackName",slackName);
		expression_slack_surplus.put("surplusName",surplusName);
		expression_slack_surplus.put("slackWeight",slackWeight);
		expression_slack_surplus.put("surplusWeight",surplusWeight);
		return expression_slack_surplus;
	}


	private static boolean isConstrain(String v) {

		return v.equals(Param.constrain);
		
	}


	private static boolean isFree(String v) {

		try { 
			
			int p = Integer.parseInt(v);
			if (p==0) return true;
	
		} catch (Exception e){}
		
		return false;
		
	}

	private static boolean isNumber(String v) {

		try { 
			
			Float p = Float.parseFloat(v);
			return true;
	
		} catch (Exception e){}
		
		return false;
		
	}
	
	public static StudyTemp processDependants(StudyTemp s){
		
		
		for (String m: s.modelList){			

			ModelTemp mObj = s.modelMap.get(m);
			
			for (String svKey: mObj.svMap.keySet()){			
				
				SvarTemp svObj = mObj.svMap.get(svKey);
				
				//svObj.dependants.removeAll(arg0);
				//svObj.caseCondition = Tools.replace_ignoreChar(svObj.caseCondition);
				//svObj.caseExpression = 
				
				//o.caseCondition = Tools.allToLowerCase(s.caseCondition);
				//o.caseExpression = Tools.allToLowerCase(s.caseExpression);
				
			}			

		}
		
		return s;

	}	
	


	

}
	