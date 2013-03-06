package com.sirtom.rsgd.util;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An class used to store vairous utilities for this application.
 * @author Thomas Le Godais <thomaslegodais@live.com>
 * @author Lazaro Brito
 */
public class Utils {

	static byte[] decrypt(String arg0) throws Exception {
		int j1 = 0;
		int i;
		int j;
		int k;
		int l;
		byte[] abyte0;
		char c1;
		int i1;
		char c2;
		try {
			i = arg0.length();
			if (0 == i)
				return new byte[0];
		} catch (RuntimeException runtimeexception) {
			throw new Exception("");
		}
		label0: {
			label1: {
				label2: {
					label3: {
						label4: {
							j = -4 & i + 3;
							k = 3 * (j / 4);
							if (~i >= ~(-2 + j))
								break label1;
							c1 = arg0.charAt(j - 2);
							if ('\0' <= c1 && c1 < p.length) {
								l = p[c1];
								if (j1 == 0)
									break label4;
							}
							l = -1;
						}
						if (~l == 0)
							break label1;
						if (i <= j - 1)
							break label2;
						c2 = arg0.charAt(-1 + j);
						if (~c2 <= -1 && ~p.length < ~c2) {
							i1 = p[c2];
							if (j1 == 0)
								break label3;
						}
						i1 = -1;
					}
					if (0 != ~i1 && j1 == 0)
						break label0;
				}
				k--;
				if (j1 == 0)
					break label0;
			}
			k -= 2;
		}
		abyte0 = new byte[k];
		decrypt2(abyte0, 0, arg0, (byte) -75);
		return abyte0;
	}

	static int decrypt2(byte[] arg0, int arg1, String arg2, byte arg3)
			throws Exception {
		int var17 = 0;

		try {
			int var4 = arg1;
			int var5 = arg2.length();
			int var6 = 0;

			int var10000;
			while (true) {
				if (var5 > var6) {
					char var8 = arg2.charAt(var6);
					var10000 = var8;
					if (var17 != 0) {
						break;
					}

					int var7;
					label128: {
						if (var8 >= 0 && ~p.length < ~var8) {
							var7 = p[var8];
							if (var17 == 0) {
								break label128;
							}
						}

						var7 = -1;
					}

					int var10;
					label140: {
						if (var5 <= 1 + var6) {
							var10 = -1;
							if (var17 == 0) {
								break label140;
							}
						}

						int var11;
						label117: {
							char var12 = arg2.charAt(var6 + 1);
							if (0 <= var12 && var12 < p.length) {
								var11 = p[var12];
								if (var17 == 0) {
									break label117;
								}
							}

							var11 = -1;
						}

						var10 = var11;
					}

					int var19;
					label141: {
						if (~(2 + var6) <= ~var5) {
							var19 = -1;
							if (var17 == 0) {
								break label141;
							}
						}

						int var13;
						label106: {
							char var14 = arg2.charAt(2 + var6);
							if (0 <= var14 && p.length > var14) {
								var13 = p[var14];
								if (var17 == 0) {
									break label106;
								}
							}

							var13 = -1;
						}

						var19 = var13;
					}

					int var20;
					label142: {
						if (3 + var6 >= var5) {
							var20 = -1;
							if (var17 == 0) {
								break label142;
							}
						}

						int var15;
						label95: {
							char var16 = arg2.charAt(3 + var6);
							if (0 <= var16 && ~var16 > ~p.length) {
								var15 = p[var16];
								if (var17 == 0) {
									break label95;
								}
							}

							var15 = -1;
						}

						var20 = var15;
					}

					arg0[arg1++] = (byte) (var7 << 2 | var10 >>> 4);
					if (var19 != -1 || var17 != 0) {
						arg0[arg1++] = (byte) (var19 >>> 2 | 240 & var10 << 4);
						if (~var20 != 0) {
							arg0[arg1++] = (byte) (var20 | var19 << 6 & 192);
							var6 += 4;
							if (var17 == 0) {
								continue;
							}
						}
					}
				}

				var10000 = arg3;
				break;
			}

			return var10000 > -45 ? 45 : arg1 + -var4;
		} catch (RuntimeException var18) {
			throw new Exception("");

		}
	}

	@SuppressWarnings("resource")
	public static HashMap<String, byte[]> decryptPack(String variable1, String variable2) {
		int dummy = 0;
		HashMap<String, byte[]> classes = new HashMap<String, byte[]>();
		try {
			byte[] key = decrypt(variable1);
			byte[] ivParameterSpec = decrypt(variable2);
			Cipher cipherObject;
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

			try {
				cipherObject = Cipher.getInstance("AES/CBC/PKCS5Padding");
				(cipherObject).init(2, secretKeySpec, new IvParameterSpec(ivParameterSpec));
			} catch (NoSuchPaddingException var34) {
				throw new RuntimeException(var34);
			} catch (NoSuchAlgorithmException var35) {
				throw new RuntimeException(var35);
			} catch (InvalidKeyException var36) {
				throw new RuntimeException(var36);
			} catch (InvalidAlgorithmParameterException var37) {
				throw new RuntimeException(var37);
			}

			InputStream packStream = new FileInputStream(new File("./data/inner.pack.gz"));
			byte[] buffer = new byte[5242880];
			int offset = 0;

			int bytesread;
			try {
				int bytesRead = packStream.read(buffer, offset,
						5242880 - offset);

				while (-1 != bytesRead) {
					offset += bytesRead;
					bytesRead = packStream.read(buffer, offset,
							-offset + 5242880);
					bytesread = dummy;

					if (bytesread != 0 || dummy != 0) {
						break;
					}
				}
			} catch (IOException var44) {
				var44.printStackTrace();
			}

			byte[] dest = new byte[offset];
			System.arraycopy(buffer, 0, dest, 0, offset);
			byte[] decryptedData = null;

			try {
				decryptedData = cipherObject.doFinal(dest);
			} catch (BadPaddingException var32) {
				var32.printStackTrace();
			} catch (IllegalBlockSizeException var33) {
				var33.printStackTrace();
			}
			Pack200.Unpacker unpacker = Pack200.newUnpacker();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(5242880);

			try {
				JarOutputStream jos = new JarOutputStream(bos);
				GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(decryptedData));
				unpacker.unpack(gzip, jos);
				jos.close();
			} catch (IOException var31) {
				var31.printStackTrace();
			}

			label191: {
				try {
					JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(bos.toByteArray()));

					JarEntry entry;
					while ((entry = jarInputStream.getNextJarEntry()) != null) {
						String entryName = entry.getName();
						offset = 0;
						bytesread = jarInputStream.read(buffer, offset,
								5242880 + -offset);
						if (dummy != 0) {
							break label191;
						}

						int bytesRead1 = bytesread;

						label143: {
							while (true) {
								if (0 != ~bytesRead1) {
									offset += bytesRead1;
									bytesRead1 = jarInputStream.read(buffer,
											offset, -offset + 5242880);
									bytesread = dummy;

									if (bytesread != 0) {
										break;
									}

									if (dummy == 0) {
										continue;
									}
								}

								if (!entryName.endsWith(".class")) {
									break label143;
								}

								entryName = entryName.replace('/', '.');
								break;
							}

							byte[] desBytes = new byte[offset];
							System.arraycopy(buffer, 0, desBytes, 0, offset);
							classes.put(
									entryName.substring(0, entryName.length()
											+ -6), desBytes);
						}

						if (dummy != 0) {
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return classes;
	}

	static int[] p;
	private static char[] m;

	static {
		label0: {
			p = new int[128];
			int i1 = 0;
			while (~i1 > ~p.length) {
				p[i1] = -1;
				i1++;
			}
			break label0;

		}
		label1: {
			int j1 = 65;
			while (-91 <= ~j1) {
				p[j1] = -65 + j1;
				j1++;
			}
			break label1;
		}
		label2: {
			int k1 = 97;
			while (122 >= k1) {
				p[k1] = k1 + -71;
				k1++;
			}
			break label2;
		}
		label3: {
			int l1 = 48;
			while (-58 <= ~l1) {
				p[l1] = (-48 + l1) - -52;
				l1++;
			}
			break label3;
		}
		label4: {
			p[43] = 62;
			int ai[] = p;
			ai[42] = 62;
			p[47] = 63;
			int ai1[] = p;
			ai1[45] = 63;
			m = new char[64];
			int i2 = 0;
			while (26 > i2) {
				m[i2] = (char) (65 + i2);
				i2++;
			}
			break label4;
		}
		label5: {
			int j2 = 26;
			while (~j2 > -53) {
				m[j2] = (char) (-26 + (97 - -j2));
				j2++;
			}
			break label5;
		}
		label6: {
			int k2 = 52;
			while (k2 < 62) {
				m[k2] = (char) (-52 + k2 + 48);
				k2++;
			}
			break label6;
		}
		m[63] = '/';
		m[62] = '+';
	}

	static {
		label0: {
			p = new int[128];
			int i1 = 0;
			while (~i1 > ~p.length) {
				p[i1] = -1;
				i1++;
			}
			break label0;
		}
		label1: {
			int j1 = 65;
			while (-91 <= ~j1) {
				p[j1] = -65 + j1;
				j1++;
			}
			break label1;
		}
		label2: {
			int k1 = 97;
			while (122 >= k1) {
				p[k1] = k1 + -71;
				k1++;
			}
			break label2;
		}
		label3: {
			int l1 = 48;
			while (-58 <= ~l1) {
				p[l1] = (-48 + l1) - -52;
				l1++;
			}
			break label3;
		}
		label4: {
			p[43] = 62;
			int ai[] = p;
			ai[42] = 62;
			p[47] = 63;
			int ai1[] = p;
			ai1[45] = 63;
			m = new char[64];
			int i2 = 0;
			while (26 > i2) {
				m[i2] = (char) (65 + i2);
				i2++;
			}
			break label4;
		}
		label5: {
			int j2 = 26;
			while (~j2 > -53) {
				m[j2] = (char) (-26 + (97 - -j2));
				j2++;
			}
			break label5;
		}
		label6: {
			int k2 = 52;
			while (k2 < 62) {
				m[k2] = (char) (-52 + k2 + 48);
				k2++;
			}
			break label6;
		}
		m[63] = '/';
		m[62] = '+';
	}
}
