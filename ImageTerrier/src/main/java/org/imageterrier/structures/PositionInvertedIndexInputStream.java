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
 * The Original Code is PositionInvertedIndexInputStream.java
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
package org.imageterrier.structures;

import java.io.IOException;
import java.util.Iterator;

import org.imageterrier.structures.postings.PositionIterablePosting;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.IndexConfigurable;
import org.terrier.structures.InvertedIndexInputStream;
import org.terrier.structures.Pointer;
import org.terrier.structures.postings.IterablePosting;


public class PositionInvertedIndexInputStream extends InvertedIndexInputStream implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;
	protected int [] positionBits = {8,8};

	public PositionInvertedIndexInputStream(Index index, String structureName, Class<? extends IterablePosting> postingIteratorClass) throws IOException {
		super(index, structureName, postingIteratorClass);
	}

	public PositionInvertedIndexInputStream(Index index, String structureName, Iterator<? extends Pointer> lexInputStream, Class<? extends IterablePosting> postingIteratorClass) throws IOException {
		super(index, structureName, lexInputStream, postingIteratorClass);
	}

	public PositionInvertedIndexInputStream(Index index, String structureName, Iterator<? extends Pointer> lexInputStream) throws IOException {
		super(index, structureName, lexInputStream);
	}

	public PositionInvertedIndexInputStream(Index index, String structureName) throws IOException {
		super(index, structureName);
	}

	/** let it know which index to use */
	@Override
	public void setIndex(Index i)
	{
		DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);
		
		//read in the index properties for term positions
		int nPositionOrdinates = i.getIntIndexProperty("positions.ordinates", 2);
		positionBits = new int[nPositionOrdinates];
		
		String [] parts = i.getIndexProperty("positions.nbits", "8,8").split(",");
		if (parts.length != nPositionOrdinates)
			throw new RuntimeException("Position bits length is not equal to the number of ordinates");
		
		for (int j=0; j<nPositionOrdinates; j++)
			positionBits[j] = Integer.parseInt(parts[j].trim());
	}

	@Override
	protected IterablePosting loadPostingIterator(BitIndexPointer pointer) throws IOException {
		IterablePosting ip = super.loadPostingIterator(pointer);
		
		if (ip instanceof PositionIterablePosting) {
			((PositionIterablePosting)ip).setPositionBits(positionBits);
		}
		
		return ip;
	}
	
	@Override
	protected int[][] getNextDocuments(BitIndexPointer pointer) throws IOException {
		//System.err.println("pointer="+pointer.toString() + " actual=@{"+file.getByteOffset() + ","+ file.getBitOffset()+ "}");
		if (file.getByteOffset() != pointer.getOffset())
		{
			//System.err.println("skipping " + (pointer.getOffset() - file.getByteOffset()) + " bytes");
			file.skipBytes(pointer.getOffset() - file.getByteOffset());
		}
		if (file.getBitOffset() != pointer.getOffsetBits())
		{
			//System.err.println("skipping "+ (pointer.getOffsetBits() - file.getBitOffset()) + "bits");
			file.skipBits(pointer.getOffsetBits() - file.getBitOffset());
		}
		
		final int df = pointer.getNumberOfEntries();
		
		final int[][] documentTerms = new int[5][];
		documentTerms[0] = new int[df];
		documentTerms[1] = new int[df];
		documentTerms[2] = new int[df];
		documentTerms[3] = new int[df];

		documentTerms[0][0] = file.readGamma() - 1; //docid
		documentTerms[1][0] = file.readUnary();		//freq

		int numOfTerms = documentTerms[3][0] = documentTerms[1][0];
		int tmpBlocks[][] = new int[numOfTerms][positionBits.length];
		
		for (int i=0; i<numOfTerms; i++) {
			for (int j=0; j<positionBits.length; j++)
				tmpBlocks[i][j] = file.readBinary(positionBits[j]);
		}

		for (int i = 1; i < df; i++) {					
			documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
			documentTerms[1][i]  = file.readUnary();

			numOfTerms = documentTerms[3][i] = documentTerms[1][i];
			tmpBlocks = new int[numOfTerms][positionBits.length];
			
			for (int k=0; k<numOfTerms; k++) {
				for (int j=0; j<positionBits.length; j++)
					tmpBlocks[k][j] = file.readBinary(positionBits[j]);
			}
		}

		//documentTerms[4] = blockids.toNativeArray();
		return documentTerms;
	}

	@Override
	public void print() {
		int documents[][] = null;
		int i =0;
		try{
			while((documents = getNextDocuments()) != null)
			{
				System.out.print("tid"+i);
				int blockindex = 0;
				for (int j = 0; j < documents[0].length; j++) {
					System.out.print(
							"("
							+ documents[0][j]
							               + ", "
							               + documents[1][j]
							                              + ", ");
					if (super.fieldCount>0)
					{
						System.out.print(documents[2][j]
						                              + ", ");
					}
					System.out.print( documents[3][j]);

					for (int k = 0; k < documents[3][j]; k++) {
						System.out.print(", B" + documents[4][blockindex]);
						blockindex++;
					}
					System.out.print(")");
				}
				System.out.println();
			}
		} catch (IOException ioe) {}
	}

}