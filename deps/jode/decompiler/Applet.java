/* Applet Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: Applet.java,v 4.3.2.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

public class Applet extends java.applet.Applet {
	private final int BORDER = 10;
	private final int BEVEL = 2;
	private Window jodeWin = new Window(this);
	private Insets myInsets;
	private Color pageColor;

	// /#ifdef AWT10
	// / public boolean action(Event e, Object arg) {
	// / jodeWin.action(e, arg);
	// / return true;
	// / }
	// /
	// / public Insets insets() {
	// / if (myInsets == null) {
	// / Insets appInsets = super.insets();
	// /#else
	public Insets getInsets() {
		if (myInsets == null) {
			Insets appInsets = super.getInsets();
			// /#endif
			myInsets = new Insets(appInsets.top + BORDER, appInsets.left
					+ BORDER, appInsets.bottom + BORDER, appInsets.right
					+ BORDER);
		}
		return myInsets;
	}

	public void paint(Graphics g) {
		super.paint(g);
		Color back = getBackground();
		Color bright = back.brighter();
		Color dark = back.darker();
		// /#ifdef AWT10
		// / Dimension size = size();
		// /#else
		Dimension size = getSize();
		// /#endif

		// Fill corners with page color:
		g.setColor(pageColor);
		g.fillRect(0, 0, BORDER, BORDER);
		g.fillRect(size.width - BORDER, 0, BORDER, BORDER);
		g.fillRect(size.width - BORDER, size.height - BORDER, BORDER, BORDER);
		g.fillRect(0, size.height - BORDER, BORDER, BORDER);

		// put filled arcs into corners with highlight color
		g.setColor(bright);
		g.fillArc(0, 0, 2 * BORDER, 2 * BORDER, 90, 90);
		g.fillArc(size.width - 2 * BORDER, 0, 2 * BORDER, 2 * BORDER, 45, 45);
		g.fillArc(0, size.height - 2 * BORDER, 2 * BORDER, 2 * BORDER, 180, 45);

		// draw highlighted edges
		g.fillRect(BORDER, 0, size.width - 2 * BORDER, BEVEL);
		g.fillRect(0, BORDER, BEVEL, size.height - 2 * BORDER);

		// The same as above on the other side with dark color.
		g.setColor(dark);
		g.fillArc(size.width - 2 * BORDER, 0, 2 * BORDER, 2 * BORDER, 0, 45);
		g.fillArc(0, size.height - 2 * BORDER, 2 * BORDER, 2 * BORDER, 225, 45);
		g.fillArc(size.width - 2 * BORDER, size.height - 2 * BORDER,
				2 * BORDER, 2 * BORDER, -90, 90);
		g.fillRect(BORDER, size.height - BEVEL, size.width - 2 * BORDER, BEVEL);
		g.fillRect(size.width - BEVEL, BORDER, BEVEL, size.height - 2 * BORDER);

		// Finally fill the corners with background color again.
		g.setColor(back);
		g.fillArc(BEVEL, BEVEL, 2 * (BORDER - BEVEL), 2 * (BORDER - BEVEL), 90,
				90);
		g.fillArc(size.width - (2 * BORDER - BEVEL), BEVEL,
				2 * (BORDER - BEVEL), 2 * (BORDER - BEVEL), 0, 90);
		g.fillArc(BEVEL, size.height - 2 * BORDER + BEVEL,
				2 * (BORDER - BEVEL), 2 * (BORDER - BEVEL), 180, 90);
		g.fillArc(size.width - (2 * BORDER - BEVEL), size.height
				- (2 * BORDER - BEVEL), 2 * (BORDER - BEVEL),
				2 * (BORDER - BEVEL), -90, 90);
	}

	public void init() {
		String colorstr = getParameter("pagecolor");
		if (colorstr == null)
			colorstr = "ffffff";
		this.pageColor = new Color(Integer.parseInt(colorstr, 16));
		colorstr = getParameter("bgcolor");
		if (colorstr != null)
			setBackground(new Color(Integer.parseInt(colorstr, 16)));
		String cp = getParameter("classpath");
		if (cp != null)
			jodeWin.setClassPath(cp);
		String cls = getParameter("class");
		if (cls != null)
			jodeWin.setClass(cls);
	}
}
