/* MethodInfo Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: MethodInfo.java,v 1.13.2.2 2002/05/28 17:34:00 hoenicke Exp $
 */

package alterrs.jode.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class MethodInfo extends BinaryInfo {

	ClassInfo clazzInfo;

	int modifier;
	String name;
	String typeSig;

	BytecodeInfo bytecode;
	String[] exceptions;
	boolean syntheticFlag;
	boolean deprecatedFlag;

	public MethodInfo(ClassInfo ci) {
		clazzInfo = ci;
	}

	public MethodInfo(ClassInfo ci, String name, String typeSig, int modifier) {
		this.clazzInfo = ci;
		this.name = name;
		this.typeSig = typeSig;
		this.modifier = modifier;
	}

	protected void readAttribute(String name, int length, ConstantPool cp,
			DataInputStream input, int howMuch) throws IOException {
		if ((howMuch & KNOWNATTRIBS) != 0 && name.equals("Code")) {
			bytecode = new BytecodeInfo(this);
			bytecode.read(cp, input);
		} else if (name.equals("Exceptions")) {
			int count = input.readUnsignedShort();
			exceptions = new String[count];
			for (int i = 0; i < count; i++)
				exceptions[i] = cp.getClassName(input.readUnsignedShort());
			if (length != 2 * (count + 1))
				throw new ClassFormatException(
						"Exceptions attribute has wrong length");
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
		if (bytecode != null)
			bytecode.reserveSmallConstants(gcp);
	}

	public void prepareWriting(GrowableConstantPool gcp) {
		gcp.putUTF8(name);
		gcp.putUTF8(typeSig);
		if (bytecode != null) {
			gcp.putUTF8("Code");
			bytecode.prepareWriting(gcp);
		}
		if (exceptions != null) {
			gcp.putUTF8("Exceptions");
			for (int i = 0; i < exceptions.length; i++)
				gcp.putClassName(exceptions[i]);
		}
		if (syntheticFlag)
			gcp.putUTF8("Synthetic");
		if (deprecatedFlag)
			gcp.putUTF8("Deprecated");
		prepareAttributes(gcp);
	}

	protected int getKnownAttributeCount() {
		int count = 0;
		if (bytecode != null)
			count++;
		if (exceptions != null)
			count++;
		if (syntheticFlag)
			count++;
		if (deprecatedFlag)
			count++;
		return count;
	}

	public void writeKnownAttributes(GrowableConstantPool gcp,
			DataOutputStream output) throws IOException {
		if (bytecode != null) {
			output.writeShort(gcp.putUTF8("Code"));
			output.writeInt(bytecode.getSize());
			bytecode.write(gcp, output);
		}
		if (exceptions != null) {
			int count = exceptions.length;
			output.writeShort(gcp.putUTF8("Exceptions"));
			output.writeInt(2 + count * 2);
			output.writeShort(count);
			for (int i = 0; i < count; i++)
				output.writeShort(gcp.putClassName(exceptions[i]));
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
		if ((howMuch & KNOWNATTRIBS) != 0) {
			bytecode = null;
			exceptions = null;
		}
		if (bytecode != null)
			bytecode.dropInfo(howMuch);
		super.dropInfo(howMuch);
	}

	public ClassInfo getClazzInfo() {
		return clazzInfo;
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

	public boolean isStatic() {
		return Modifier.isStatic(modifier);
	}

	public boolean isSynthetic() {
		return syntheticFlag;
	}

	public boolean isDeprecated() {
		return deprecatedFlag;
	}

	public BytecodeInfo getBytecode() {
		return bytecode;
	}

	public String[] getExceptions() {
		return exceptions;
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

	public void setBytecode(BytecodeInfo newBytecode) {
		clazzInfo.loadInfo(KNOWNATTRIBS);
		bytecode = newBytecode;
	}

	public void setExceptions(String[] newExceptions) {
		clazzInfo.loadInfo(KNOWNATTRIBS);
		exceptions = newExceptions;
	}

	public String toString() {
		return "Method " + Modifier.toString(modifier) + " " + typeSig + " "
				+ clazzInfo.getName() + "." + name;
	}
}
