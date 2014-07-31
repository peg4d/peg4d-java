//package org.peg4d;
//
//
//public class PegJSFormatter extends PegFormatter {
//	@Override
//	public String getDesc() {
//		return "PEG.js - Parser Generator for JavaScript";
//	}
//	@Override
////	public String getSemiColon() {
//		return "";
//	}
//
//	@Override
//	public void formatRuleName(UStringBuilder sb, String ruleName, Peg e) {
//		sb.append(ruleName);
//	}
//
//	@Override
//	public void formatTagging(UStringBuilder sb, PegTagging e) {
//		//sb.append(e.symbol);
//	}
//	@Override
//	public void formatMessage(UStringBuilder sb, PegMessage e) {
//		//sb.append("`", e.symbol, "`");
//	}
//	@Override
////	public void formatIndent(UStringBuilder sb, PegIndent e) {
//		//sb.append("indent");
//	}
//	@Override
//	public void formatIndex(UStringBuilder sb, PegIndex e) {
//		//sb.appendInt(e.index);
//	}
//
//	@Override
//	public void formatSetter(UStringBuilder sb, PegSetter e) {
//		this.format(sb,  null, e, null);
//	}
//
//	@Override
//	public void formatExport(UStringBuilder sb, PegExport e) {
//		sb.append("&(");
//		e.inner.stringfy(sb, this);
//		sb.append(")");
//	}
//	@Override
//	public void formatNewObject(UStringBuilder sb, PegNewObject e) {
//		sb.append("( ");
//		this.format(sb,  e);
//		sb.append(" )");
//	}
//}
