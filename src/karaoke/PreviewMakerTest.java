package karaoke;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PreviewMakerTest {

	@Test
	void testPhraseIndexForTimestamp() {
		long[][] timestamps = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 } };
		long timestamp = 0;
		long[] phraseArr = PreviewMaker.phraseArrayFromTimestamps(timestamps);

		assertEquals(0, PreviewMaker.indexForTimestamp(phraseArr, timestamp));

		long[][] timestamps2 = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 } };
		long timestamp2 = 2;
		long[] phraseArr2 = PreviewMaker.phraseArrayFromTimestamps(timestamps2);

		assertEquals(0, PreviewMaker.indexForTimestamp(phraseArr2, timestamp2));

		long[][] timestamps3 = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 } };
		long timestamp3 = 4;
		long[] phraseArr3 = PreviewMaker.phraseArrayFromTimestamps(timestamps3);

		assertEquals(1, PreviewMaker.indexForTimestamp(phraseArr3, timestamp3));

		long[][] timestamps4 = { { 2 }, { 3, 4, 5 }, { 6, 7, 8 } };
		long timestamp4 = 1;
		long[] phraseArr4 = PreviewMaker.phraseArrayFromTimestamps(timestamps4);

		assertEquals(-1, PreviewMaker.indexForTimestamp(phraseArr4, timestamp4));

		long[][] timestamps5 = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 } };
		long timestamp5 = 9;
		long[] phraseArr5 = PreviewMaker.phraseArrayFromTimestamps(timestamps5);

		assertEquals(2, PreviewMaker.indexForTimestamp(phraseArr5, timestamp5));
	}

}
