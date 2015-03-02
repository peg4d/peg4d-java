package nez.expr;

import nez.ast.Tag;

public class NezTag {

	static final Tag Text         = Tag.tag("Text");
	static final Tag Integer   = Tag.tag("Integer");
	static final Tag Name        = Tag.tag("Name");

	static final Tag Any         = Tag.tag("Any");
	static final Tag Character   = Tag.tag("Character");
	static final Tag Byte        = Tag.tag("Byte");
	static final Tag CharacterSequence  = Tag.tag("CharacterSequence");
	static final Tag String      = Tag.tag("String");
	static final Tag List        = Tag.tag("List");
	
	static final Tag NonTerminal = Tag.tag("NonTerminal");
	static final Tag Choice      = Tag.tag("Choice");
	static final Tag Sequence    = Tag.tag("Sequence");
	static final Tag Repetition    = Tag.tag("Repetition");
	static final Tag OneMoreRepetition     = Tag.tag("OneMoreRepetition");
	static final Tag Option      = Tag.tag("Option");
	static final Tag Not         = Tag.tag("Not");
	static final Tag And         = Tag.tag("And");
	static final Tag Rule        = Tag.tag("Rule");
	static final Tag Import      = Tag.tag("Import");
	static final Tag Annotation  = Tag.tag("Annotation");
	static final Tag Constructor = Tag.tag("Constructor");
	static final Tag LeftJoin    = Tag.tag("LeftJoin");
	static final Tag Connector   = Tag.tag("Connector");
	static final Tag Value       = Tag.tag("Value");
	static final Tag Tagging     = Tag.tag("Tagging");
	static final Tag Match       = Tag.tag("Match");
	static final Tag Debug       = Tag.tag("Debug");
	static final Tag Memo        = Tag.tag("Memo");
	static final Tag If          = Tag.tag("If");
	static final Tag Without     = Tag.tag("Without");
	static final Tag With        = Tag.tag("With");
	static final Tag Block       = Tag.tag("Block");
	static final Tag Def         = Tag.tag("Def");
	static final Tag Is          = Tag.tag("Is");
	static final Tag Isa         = Tag.tag("Isa");
	static final Tag DefIndent      = Tag.tag("DefIndent");
	static final Tag Indent      = Tag.tag("Indent");
	
	static final Tag Scan = Tag.tag("Scan");
	static final Tag Repeat = Tag.tag("Repeat");
	

}
