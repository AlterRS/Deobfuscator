/* ProgressListener Copyright (C) 2000-2002 Jochen Hoenicke.
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
 * $Id: ProgressListener.java,v 4.1.2.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

/**
 * This interface is used by jode to tell about its progress. You supply an
 * instance of this interface to the {@link Decompiler.decompile} method.<br>
 * 
 * @author <a href="mailto:jochen@gnu.org">Jochen Hoenicke</a>
 * @version 1.0
 */
public interface ProgressListener {
	/**
	 * Gets called when jode makes some progress.
	 * 
	 * @param progress
	 *            A number between 0.0 and 1.0
	 * @param detail
	 *            The name of the currently decompiled method or class.
	 */
	public void updateProgress(double progress, String detail);
}
