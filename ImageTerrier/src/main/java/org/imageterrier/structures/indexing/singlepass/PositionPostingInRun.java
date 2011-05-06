/**
 * ImageTerrier - The Terabyte Retriever for Images
 * Webpage: http://www.imageterrier.org/
 * Contact: jsh2@ecs.soton.ac.uk
 * Electronics and Computer Science, University of Southampton
 * http://www.ecs.soton.ac.uk/
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is PositionPostingInRun.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk> (original contributor)
 *   Sina Samangooei <ss@ecs.soton.ac.uk>
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.imageterrier.structures.indexing.singlepass;

import java.io.IOException;

import org.terrier.compression.BitOut;
import org.terrier.structures.indexing.singlepass.SimplePostingInRun;

public class PositionPostingInRun extends SimplePostingInRun {
	/**
	 * This stores how many bits were used to encode each
	 * of the position elements. The length of this array
	 * is the number of position ordinates per posting.
	 */
	protected int [] positionBits;
	
	public PositionPostingInRun() {
		super();
	}

	/**
	 * Writes the document data of this posting to a {@link org.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1), blockNo(d1), bid1, bid2, ...,  d2 - d1, idf(d2), blockNo(d2), ...</code> etc
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @param runShift amount of delta to apply to the first posting read.
	 * @return The last posting written.
	 */
	@Override
	public int append(BitOut bos, int last, int runShift)  throws IOException {
		int current = runShift - 1;
		for(int i = 0; i < termDf; i++){
			int docid = postingSource.readGamma() + current;
			bos.writeGamma(docid - last);

			int tf = postingSource.readGamma();
			bos.writeUnary(tf);
			current = last = docid;	

			//System.out.println("append\t\t" + docid + "\t" + tf + "\t" + this.termTF);
			
			//now deal with blocks
			final int numOfBlocks = tf;

			for(int j = 0; j < numOfBlocks; j++){
				for (int k=0; k<positionBits.length; k++)
					bos.writeBinary(positionBits[k], postingSource.readBinary(positionBits[k]));
			}
		}
		try{
			postingSource.align();
		}catch(Exception e){
			// last posting
		}
		return last;
	}

	public void setPositionBits(int[] positionBits) {
		this.positionBits = positionBits;
	}
}