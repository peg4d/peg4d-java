package org.peg4d.regex;

import java.util.HashMap;
import java.util.Map;

import org.peg4d.ParsingObject;

public class RegexObjectConverter {

	private ParsingObject po;
	private Map<String, RegexObject> rules;
	private int ruleId = 0;
	private final static String rulePrefix = "E";

	public RegexObjectConverter(ParsingObject po) {
		this.po = po;
	}

	private String createId() {
		return rulePrefix + ruleId++;
	}

	public Map<String, RegexObject> convert() {
		ParsingObject tokens = po.get(0);
		RegSeq rs = new RegSeq();
		for(ParsingObject e: tokens) {
			rs.add(createRegexObject(e));
		}

		rules = new HashMap<String, RegexObject>();
		RegexObject continuation = rs.popContinuation();
		RegexObject top = pi(rs, continuation);
		rules.put("TopLevel", top);
		return rules;
	}

	private RegSeq createSequence(ParsingObject po) {
		RegSeq r = new RegSeq();
		for(ParsingObject child: po) {
			r.add(createRegexObject(child));
		}
		return r;
	}

	private RegexObject createRegexObject(ParsingObject e) {
		switch(e.getTag().toString()){
		case "Or":
			RegChoice roOr = new RegChoice();
			roOr.add(createSequence(e.get(0)));
			roOr.add(createSequence(e.get(1)));
			return roOr;
		case "Item":
			return createSequence(e.get(0));
		default: {
			switch(e.get(1).getTag().toString()) {
			case "Char":
			case "OneOf":
			case "ExceptFor":
			case "EscapedChar":
				return new RegCharSet(e);
			case "Block":
				RegSeq roBlock = createSequence(e.get(1));
				roBlock.addQuantifier(e);
				return roBlock;
			default:
				System.err.println("Sorry!! An error occurred on 'createRegexObject'.");
				return null;
			}
		}
		}
	}

	private RegexObject pi(RegexObject target, RegexObject continuation) {
		int target_size = target.size();
		if(target_size == 0) {
			//(1)
			return continuation;
		}
		else if(target_size == 1) {
			RegexObject child = target.get(0);
			if(child instanceof RegChoice) {
				//(4)
				//(a|b)c -> pi(a, c) / pi(b, c)
				//FIXME
				RegexObject r1 = child.get(0);
				RegexObject r2 = child.get(1);
				RegChoice r = new RegChoice();
				r.quantifier = target.quantifier; //(a|b)*
				r.add(pi(r1, continuation));
				r.add(pi(r2, continuation));
				return r;
			}
			else if(child instanceof RegSeq) {
				//pi((ab), c) -> pi(ab, c)
				//return pi(child, continutaion);
				if(!(continuation instanceof RegSeq)){
					return pi(child, continuation);
				}
				else if(((RegSeq) child).contains(continuation)){
					if(child.getQuantifier() != null && !"Times".equals(child.getTag())){
						continuation.pushHead(child);
						continuation = continuationBasedConversion((RegSeq) child, continuation);
						child.rmQuantifier();
						return continuation.get(0);
					}
					else{
						target.concat(continuation);
						return target;
					}
				}
				else if(child.getQuantifier() != null){
					continuation.pushHead(child);
					return continuation;
				}
				else{
					return pi(child, continuation);
				}
			}
			else if(child instanceof RegCharSet && continuation.size() > 0){
				RegexObject rHead = continuation.get(0);
				RegCharSet charSet = (RegCharSet)child;
				if(rHead instanceof RegCharSet && charSet.contains(rHead)){
					if(charSet.getQuantifier() != null && !"Times".equals(charSet.getTag())){
						return continuationBasedConversion(charSet, continuation);
					}
					else{
						target.concat(continuation);
						return target;
					}
				}
				//(2)
				else{
				target.concat(continuation);
				return target;
				}
			}
			else{
				System.err.println("Sorry!! An error occurred on 'pi'.");
				return null;
			}
		}
		else {
			//(3)
			RegexObject c2 = target.popContinuation();
			return pi(target, pi(c2, continuation));
		}
	}

	private RegexObject continuationBasedConversion(RegSeq rcLeft, RegexObject roRight){
		RegSeq rHeadSeq = (RegSeq)roRight.popHead();
		RegNonTerminal nt = new RegNonTerminal(createId());
		switch(rcLeft.getTag()){
		case "ZeroMoreL":	//a*a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadSeq, nt);
			return roRight;
		case "ZeroMoreS":	//a*?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadSeq, nt);
			return roRight;
		case "OneMoreL":	//a+a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadSeq, nt);
			roRight.pushHead(rHeadSeq);
			return roRight;
		case "OneMoreS":	//a+?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadSeq, nt);
			roRight.pushHead(rHeadSeq);
			return roRight;
		case "OptionalL":	//a?a
			roRight.pushHead(nt);
			createNewLongestOptionalRule(rHeadSeq, nt);
			return roRight;
		case "OptionalS": 	//a??a
			roRight.pushHead(nt);
			createNewShortestOptionalRule(rHeadSeq, nt);
			return roRight;
		default:
			System.err.print("Sorry!! An error occurred on conversion(seq).");
			return null;
		}
	}

	private RegexObject continuationBasedConversion(RegCharSet rcLeft, RegexObject roRight){
		RegCharSet rHeadChar = (RegCharSet)roRight.popHead();
		RegNonTerminal nt = new RegNonTerminal(createId());
		switch(rcLeft.getTag()){
		case "ZeroMoreL":	//a*a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadChar, nt);
			return roRight;
		case "ZeroMoreS":	//a*?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadChar, nt);
			return roRight;
		case "OneMoreL":	//a+a
			roRight.pushHead(nt);
			createNewLongestZeroMoreRule(rHeadChar, nt);
			roRight.pushHead(rHeadChar);
			return roRight;
		case "OneMoreS":	//a+?a
			roRight.pushHead(nt);
			createNewShortestZeroMoreRule(rHeadChar, nt);
			roRight.pushHead(rHeadChar);
			return roRight;
		case "OptionalL":	//a?a
			roRight.pushHead(nt);
			createNewLongestOptionalRule(rHeadChar, nt);
			return roRight;
		case "OptionalS": 	//a??a
			roRight.pushHead(nt);
			createNewShortestOptionalRule(rHeadChar, nt);
			return roRight;
		default:
			System.err.print("Sorry!! An error occurred on conversion(charset).");
			return null;
		}
	}

	private void createNewLongestZeroMoreRule(RegexObject rHead, RegNonTerminal nt) {
		//E0 = a E0 / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		s1.add(nt);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewShortestZeroMoreRule(RegexObject rHead, RegNonTerminal nt) {
		//E0 = a / a E0
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		s2.add(nt);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewLongestOptionalRule(RegexObject rHead, RegNonTerminal nt) {
		//E0 = a a / a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		s1.add(rHead);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}

	private void createNewShortestOptionalRule(RegexObject rHead, RegNonTerminal nt) {
		//E0 = a / a a
		RegSeq newRule = new RegSeq();
		RegChoice choice = new RegChoice();
		RegSeq s1 = new RegSeq();
		s1.add(rHead);
		RegSeq s2 = new RegSeq();
		s2.add(rHead);
		s2.add(rHead);
		choice.add(s1);
		choice.add(s2);
		newRule.add(choice);
		rules.put(nt.toString(), newRule);
	}
}
