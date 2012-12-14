/* FieldInfo Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: FieldInfo.java,v 1.10.2.1 2002/05/28 17:34:00 hoenicke Exp $
 */

package jode.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class FieldInfo extends BinaryInfo {
	ClassInfo clazzInfo;

	int modifier;
	String name;
	String typeSig;

	Object constant;
	boolean syntheticFlag;
	boolean deprecatedFlag;

	public FieldInfo(ClassInfo ci) {
		this.clazzInfo = ci;
	}

	public FieldInfo(ClassInfo ci, String name, String typeSig, int modifier) {
		this.clazzInfo = ci;
		this.name = name;
		this.typeSig = typeSig;
		this.modifier = modifier;
	}

	protected void readAttribute(String name, int length, ConstantPool cp,
			DataInputStream input, int howMuch) throws IOException {
		if ((howMuch & KNOWNATTRIBS) != 0 && name.equals("ConstantValue")) {
			if (length != 2)
				throw new ClassFormatException("ConstantValue attribute"
						+ " has wrong length");
			int index = input.readUnsignedShort();
			constant = cp.getConstant(index);
		} else if (name.equals("Synthetic")) {
			syntheticFlag = true;
			if (length != 0)
				throw new ClassFormatException(
						"Synthetic attribute has wrong length");
		} else if (name.equals("Deprecated")) {
			deprecatedFlag = true;
			if (length != 0)
				throw new ClassFormatException(
						"Deprecated attribute has wrong length");
		} else
			super.readAttribute(name, length, cp, input, howMuch);
	}

	public void read(ConstantPool constantPool, DataInputStream input,
			int howMuch) throws IOException {
		modifier = input.readUnsignedShort();
		name = constantPool.getUTF8(input.readUnsignedShort());
		typeSig = constantPool.getUTF8(input.readUnsignedShort());
		readAttributes(constantPool, input, howMuch);
	}

	public void reserveSmallConstants(GrowableConstantPool gcp) {
	}

	public void prepareWriting(GrowableConstantPool gcp) {
		gcp.putUTF8(name);
		gcp.putUTF8(typeSig);
		if (constant != null) {
			gcp.putUTF8("ConstantValue");
			if (typeSig.charAt(0) == 'J' || typeSig.charAt(0) == 'D')
				gcp.putLongConstant(constant);
			else
				gcp.putConstant(constant);
		}
		if (syntheticFlag)
			gcp.putUTF8("Synthetic");
		if (deprecatedFlag)
			gcp.putUTF8("Deprecated");
		prepareAttributes(gcp);
	}

	protected int getKnownAttributeCount() {
		int count = 0;
		if (constant != null)
			count++;
		if (syntheticFlag)
			count++;
		if (deprecatedFlag)
			count++;
		return count;
	}

	public void writeKnownAttributes(GrowableConstantPool gcp,
			DataOutputStream output) throws IOException {
		if (constant != null) {
			output.writeShort(gcp.putUTF8("ConstantValue"));
			output.writeInt(2);
			int index;
			if (typeSig.charAt(0) == 'J' || typeSig.charAt(0) == 'D')
				index = gcp.putLongConstant(constant);
			else
				index = gcp.putConstant(constant);
			output.writeShort(index);
		}
		if (syntheticFlag) {
			output.writeShort(gcp.putUTF8("Synthetic"));
			output.writeInt(0);
		}
		if (deprecatedFlag) {
			output.writeShort(gcp.putUTF8("Deprecated"));
			output.writeInt(0);
		}
	}

	public void write(GrowableConstantPool constantPool, DataOutputStream output)
			throws IOException {
		output.writeShort(modifier);
		output.writeShort(constantPool.putUTF8(name));
		output.writeShort(constantPool.putUTF8(typeSig));
		writeAttributes(constantPool, output);
	}

	public void dropInfo(int howMuch) {
		if ((howMuch & KNOWNATTRIBS) != 0)
			constant = null;
		super.dropInfo(howMuch);
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return typeSig;
	}

	public int getModifiers() {
		return modifier;
	}

	public boolean isSynthetic() {
		return syntheticFlag;
	}

	public boolean isDeprecated() {
		return deprecatedFlag;
	}

	public Object getConstant() {
		clazzInfo.loadInfo(KNOWNATTRIBS);
		return constant;
	}

	public void setName(String newName) {
		name = newName;
	}

	public void setType(String newType) {
		typeSig = newType;
	}

	public void setModifiers(int newModifier) {
		modifier = newModifier;
	}

	public void setSynthetic(boolean flag) {
		syntheticFlag = flag;
	}

	public void setDeprecated(boolean flag) {
		deprecatedFlag = flag;
	}

	public void setConstant(Object newConstant) {
		constant = newConstant;
	}

	public String toString() {
		return "Field " + Modifier.toString(modifier) + " " + typeSig + " "
				+ name;
	}
}
