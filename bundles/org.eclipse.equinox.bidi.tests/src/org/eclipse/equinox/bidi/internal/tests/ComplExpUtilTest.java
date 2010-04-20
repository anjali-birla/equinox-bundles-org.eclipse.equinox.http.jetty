/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.complexp.*;
import java.util.Locale;

/**
 * Tests methods in ComplExpUtil
 */

public class ComplExpUtilTest extends ComplExpTestBase {

	private static final String HEBREW = "iw";
	private static final String HEBREW2 = "he";
	private static final String ARABIC = "ar";
	private static final String FARSI = "fa";
	private static final String URDU = "ur";
	private Locale locale;

	protected void setUp() throws Exception {
		super.setUp();
		locale = Locale.getDefault();
	}

	protected void tearDown() {
		Locale.setDefault(locale);
	}

	private void doTest1(String data, String result) {
		Locale.setDefault(Locale.ENGLISH);
		String full = ComplExpUtil.process(toUT16(data));
		assertEquals("Util #1 full EN - ", data, toPseudo(full));
		Locale.setDefault(new Locale(HEBREW2));
		full = ComplExpUtil.process(toUT16(data));
		assertEquals("Util #1 full HE - ", result, toPseudo(full));
		Locale.setDefault(new Locale(ARABIC));
		full = ComplExpUtil.process(toUT16(data));
		assertEquals("Util #1 full AR - ", result, toPseudo(full));
		Locale.setDefault(new Locale(FARSI));
		full = ComplExpUtil.process(toUT16(data));
		assertEquals("Util #1 full FA - ", result, toPseudo(full));
		Locale.setDefault(new Locale(URDU));
		full = ComplExpUtil.process(toUT16(data));
		assertEquals("Util #1 full UR - ", result, toPseudo(full));
		Locale.setDefault(new Locale(HEBREW));
		full = ComplExpUtil.process(toUT16(data));
		String ful2 = ComplExpUtil.process(toUT16(data), null);
		assertEquals("Util #1 full - ", result, toPseudo(full));
		assertEquals("Util #1 ful2 - ", result, toPseudo(ful2));
		String lean = ComplExpUtil.deprocess(full);
		assertEquals("Util #1 lean - ", data, toPseudo(lean));
	}

	private void doTest2(String msg, String data, String result) {
		doTest2(msg, data, result, data);
	}

	private void doTest2(String msg, String data, String result, String resLean) {
		String full = ComplExpUtil.process(toUT16(data), "*");
		assertEquals(msg + "full", result, toPseudo(full));
		String lean = ComplExpUtil.deprocess(full);
		assertEquals(msg + "lean", resLean, toPseudo(lean));
	}

	private void doTest3(String msg, String data, String result) {
		doTest3(msg, data, result, data);
	}

	private void doTest3(String msg, String data, String result, String resLean) {
		String full = ComplExpUtil.processTyped(toUT16(data),
				IBiDiProcessor.COMMA_DELIMITED);
		assertEquals(msg + "full", result, toPseudo(full));
		String lean = ComplExpUtil.deprocess(full,
				IBiDiProcessor.COMMA_DELIMITED);
		assertEquals(msg + "lean", resLean, toPseudo(lean));
	}

	private void doTest4(String msg, String data, int[] offsets, int direction,
			boolean affix, String result) {
		String txt = msg + "text=" + data + "\n    offsets="
				+ array_display(offsets) + "\n    direction=" + direction
				+ "\n    affix=" + affix;
		String lean = toUT16(data);
		String full = ComplExpUtil.insertMarks(lean, offsets, direction, affix);
		assertEquals(txt, result, toPseudo(full));
	}

	public void testComplExpUtil() {

		// Test process() and deprocess() with default delimiters
		doTest1("ABC/DEF/G", ">@ABC@/DEF@/G@^");
		// Test process() and deprocess() with specified delimiters
		doTest2("Util #2.1 - ", "", "");
		doTest2("Util #2.2 - ", ">@ABC@^", ">@ABC@^", "ABC");
		doTest2("Util #2.3 - ", "abc", "abc");
		doTest2("Util #2.4 - ", "!abc", ">@!abc@^");
		doTest2("Util #2.5 - ", "abc!", ">@abc!@^");
		doTest2("Util #2.6 - ", "ABC*DEF*G", ">@ABC@*DEF@*G@^");
		// Test process() and deprocess() with specified expression type
		doTest3("Util #3.1 - ", "ABC,DEF,G", ">@ABC@,DEF@,G@^");
		doTest3("Util #3.2 - ", "", "");
		doTest3("Util #3.3 - ", ">@DEF@^", ">@DEF@^", "DEF");
		String str = ComplExpUtil.deprocess(toUT16("ABC,DE"), "wrong_type");
		assertEquals("deprocess(9999)", "ABC,DE", toPseudo(str));
		str = ComplExpUtil.process("abc", "wrong_type");
		assertEquals("invalid type", "abc", toPseudo(str));
		// Test insertMarks()
		doTest4("Util #4.1 - ", "ABCDEFG", new int[] { 3, 6 }, 0, false,
				"ABC@DEF@G");
		doTest4("Util #4.2 - ", "ABCDEFG", new int[] { 3, 6 }, 0, true,
				">@ABC@DEF@G@^");
		doTest4("Util #4.3 - ", "ABCDEFG", new int[] { 3, 6 }, 1, false,
				"ABC&DEF&G");
		doTest4("Util #4.4 - ", "ABCDEFG", new int[] { 3, 6 }, 1, true,
				"<&ABC&DEF&G&^");
		doTest4("Util #4.5 - ", "", new int[] { 3, 6 }, 0, false, "");
		doTest4("Util #4.6 - ", "", new int[] { 3, 6 }, 0, true, "");
		doTest4("Util #4.7 - ", "ABCDEFG", null, 1, false, "ABCDEFG");
		doTest4("Util #4.8 - ", "ABCDEFG", null, 1, true, "<&ABCDEFG&^");
	}
}