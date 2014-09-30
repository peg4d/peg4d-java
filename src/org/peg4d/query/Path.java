package org.peg4d.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingTag;

public class Path {
	private final List<Segment> segmentList;

	/**
	 * if true, accept root segment
	 */
	private final boolean allowRootSegment;

	public Path(boolean allowRootSegment) {
		this.segmentList = new ArrayList<>();
		this.allowRootSegment = allowRootSegment;
	}

	/**
	 * 
	 * @param segment
	 * @throws IllegalArgumentException
	 */
	public void addSegment(Segment segment) throws IllegalArgumentException {
		if(segment instanceof RootSegment) {
			if(!this.allowRootSegment || this.segmentList.size() > 0) {
				throw new IllegalArgumentException("not accept root segment");
			}
		}
		this.segmentList.add(segment);
	}

	public List<ParsingObject> apply(ParsingObject tree) {
		List<ParsingObject> targetList = Collections.singletonList(tree);
		for(Segment segment : this.segmentList) {
			List<ParsingObject> resultList = new LinkedList<>();
			for(ParsingObject target : targetList) {
				resultList.addAll(segment.match(target));
			}
			targetList = resultList;
		}
		return targetList;
	}

	/**
	 * represent path segment
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static abstract class Segment {
		protected final String tag;
		protected final int tagId;

		/**
		 * 
		 * @param name
		 * not null
		 */
		public Segment(String name) {
			this.tag = name;
			this.tagId = ParsingTag.tagId(name);
		}

		public String getTag() {
			return this.tag;
		}

		@Override
		public String toString() {
			return this.tag;
		}

		protected static String resolveTag(String tagName) {
			return tagName;
		}

		public abstract List<ParsingObject> match(final ParsingObject target);
	}

	/**
	 * must be first segment
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class RootSegment extends Segment {
		public RootSegment() {
			super("/root");
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			return Collections.singletonList(target);
		}
	}

	/**
	 * find child node which tag is equivalent to name
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class TagNameSegment extends Segment {
		public TagNameSegment(String name) {
			super(resolveTag(name));
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			List<ParsingObject> resultList = new ArrayList<>();
			final int size = target.size();
			for(int i = 0; i < size; i++) {
				ParsingObject child = target.get(i);
				if(child.is(this.tagId)) {
					resultList.add(child);
				}
			}
			return resultList;
		}
	}

	/**
	 * find all child node
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class WildCardSegment extends Segment {
		public WildCardSegment() {
			super("*");
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			final int size = target.size();
			List<ParsingObject> resultList = new ArrayList<>(size);
			for(int i = 0; i < size; i++) {
				resultList.add(target.get(i));
			}
			return resultList;
		}
	}

	/**
	 * find all descendant node which tag is equivalent to name
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class DescendantSegment extends Segment {
		public DescendantSegment(String name) {
			super(resolveTag(name));
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			List<ParsingObject> resultList = new LinkedList<>();
			this.findAllMatchedDescendant(resultList, target);
			return resultList;
		}

		private void findAllMatchedDescendant(final List<ParsingObject> resultList, ParsingObject parent) {
			final int size = parent.size();
			for(int i = 0; i < size; i++) {
				ParsingObject child = parent.get(i);
				if(child.is(this.tagId)) {
					resultList.add(child);
				}
				this.findAllMatchedDescendant(resultList, child);
			}
		}

		@Override
		public String toString() {
			return "/descendant " + this.tag;
		}
	}

	/**
	 * find all descendant node
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class WildCardDescendantSegment extends Segment {
		public WildCardDescendantSegment() {
			super("/decendant *");
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			List<ParsingObject> resultList = new LinkedList<>();
			this.findAllDescendant(resultList, target);
			return resultList;
		}

		private void findAllDescendant(final List<ParsingObject> resultList, ParsingObject parent) {
			final int size = parent.size();
			for(int i = 0; i < size; i++) {
				ParsingObject child = parent.get(i);
				resultList.add(child);
				this.findAllDescendant(resultList, child);
			}
		}
	}

	public static class IndexListSegment extends Segment {
		private final Segment targetSegment;
		private final List<Integer> indexList;

		public IndexListSegment(Segment targetSegment, List<Integer> indexList) {
			super(formatName(indexList));
			this.targetSegment = targetSegment;
			this.indexList = indexList;
		}

		private static String formatName(List<Integer> indexList) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("indexList[");
			int count = 0;
			for(int index : indexList) {
				if(count++ > 0) {
					sBuilder.append(", ");
				}
				sBuilder.append(index);
			}
			sBuilder.append("]");
			return sBuilder.toString();
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {	//TODO: exception handling
			List<ParsingObject> preList = this.targetSegment.match(target);
			List<ParsingObject> resultList = new ArrayList<>(this.indexList.size());
			for(int index : this.indexList) {
				try {
					resultList.add(preList.get(index));
				}
				catch(IndexOutOfBoundsException e) {
				}
			}
			return resultList;
		}
	}

	public static class RangeSegment extends Segment {
		private final Segment targetSegment;
		private final int startIndex;	// include
		private final int stopIndex;	// exclude

		public RangeSegment(Segment targetSegment, Pair<Integer, Integer> rangePair) {
			this(targetSegment, rangePair.getLeft(), rangePair.getRight());
		}

		public RangeSegment(Segment targetSegment, int startIndex, int stopIndex) {
			super("range[" + startIndex + ".." + stopIndex + "]");
			this.targetSegment = targetSegment;
			this.startIndex = startIndex;
			this.stopIndex = stopIndex;
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {	//TODO: exception handling
			List<ParsingObject> preList = this.targetSegment.match(target);
			List<ParsingObject> resultList = new ArrayList<>(this.stopIndex - this.startIndex);
			for(int i = this.startIndex; i < this.stopIndex; i++) {
				resultList.add(preList.get(i));
			}
			return resultList;
		}
	}

	public static class MatchSegment extends Segment {
		private final Segment targetSegment;
		private final String value;

		public MatchSegment(Segment targetSegment, String value) {
			super("match[" + value + "]");
			this.targetSegment = targetSegment;
			this.value = value;
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			List<ParsingObject> preList = this.targetSegment.match(target);
			List<ParsingObject> resultList = new ArrayList<>();
			final int size = preList.size();
			for(int i = 0; i < size; i++) {
				ParsingObject p = preList.get(i);
				if(p.getText().equals(this.value)) {
					resultList.add(p);
				}
			}
			return resultList;
		}
	}

	public static class ParentSegment extends Segment {
		public ParentSegment() {
			super("/parent");
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<ParsingObject> match(ParsingObject target) {
			ParsingObject parent = target.getParent();
			if(parent != null) {
				return Collections.singletonList(parent);
			}
			return Collections.EMPTY_LIST;
		}
	}

	public static class SelfSegment extends Segment {
		public SelfSegment() {
			super("/self");
		}

		@Override
		public List<ParsingObject> match(ParsingObject target) {
			return Collections.singletonList(target);
		}
	}
}
