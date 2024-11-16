package net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent;
/*
 * Copyright (C) 2001-2003 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


import net.sourceforge.squirrel_sql.fw.util.StringUtilities;

/**
 * @author gwg
 *
 * This encapsulates the functions of converting binary data
 * (held as an array of Bytes)
 * into a string for display, and converting a string from a display
 * into an array of Bytes.
 * Both operations allow for the string to be "<null>".
 * Both operations allow for hex, octal, decimal or binary representation.
 * Both operations allow for individual bytes that represent ascii characters
 * to be displayed as that ascii char rather than as the binary representation.
 * <P>
 * These functions are entirely static since all parameters are handed in
 * each time they are called, so there is no need for any instances of this
 * class to ever be created.
 */
public class BinaryDisplayConverter {
	
	/**
	 * Use Hexidecimal representation
	 */
	 public static final int HEX = 16;
	 
	 /**
	  * Use decimal representation.
	  */
	 public static final int DECIMAL = 10;
	 
	 /**
	  * Use Octal representation.
	  */
	 public static final int OCTAL = 8;
	 
	 /**
	  * Use Binary representation.
	  */
	 public static final int BINARY = 2;
	
	/*
	 * Conversion Constants
	 */
	 static class ConversionConstants {
	 	int width;	// number of chars used to represent byte
	 	int radix;	// the base radix
	 	
	 	ConversionConstants(int w, int r) {
	 		width = w;
	 		radix = r;
	 	}
	 }
	 
	 static ConversionConstants hex = new ConversionConstants(2, 16);
	 static ConversionConstants decimal = new ConversionConstants(3, 10);
	 private static ConversionConstants octal = new ConversionConstants(3, 8);
	 private static ConversionConstants binary = new ConversionConstants(8, 2);
	 
	 /**
	  * List of characters considered "printable".
	  */
	 private static String printable = "0123456789abcdefghijklmnopqrstuvwxyz" +
	 	"ABCDEFGHIJKLMNOPQRSTUVWXYZ`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
	 
	
	/**
	 * Do not allow any instances to be created.
	 */
	private BinaryDisplayConverter() {}
	
	/**
	 * Convert from an array of Bytes into a string.
	 */
	public static String convertToString(Byte[] data, int base, boolean showAscii)
	{
		return convertToString(data, base, showAscii ? DisplayAsciiMode.ASCII_DEFAULT : DisplayAsciiMode.ASCII_NO);
	}

	public static String convertToString(Byte[] data, int base, DisplayAsciiMode displayAsciiMode) {

		// handle null
		if (data == null)
			return null;

		StringBuilder buf = new StringBuilder();
		
		ConversionConstants convConst = getConstants(base);
		
		// Convert each byte and put into string buffer
		for(int i = 0; i < data.length; i++)
		{
			int value = data[i].byteValue();
			String s = null;

			// if user wants to see ASCII chars as characters,
			// see if this is one that should be displayed that way
			if(DisplayAsciiMode.isShowAscii(displayAsciiMode))
			{
				if(printable.indexOf((char) value) > -1)
				{
					s = Character.toString((char) value);

					if(displayAsciiMode == DisplayAsciiMode.ASCII_DEFAULT)
               {
                  s += "          ".substring(10 - (convConst.width - 1));
               }
            }
				else if(displayAsciiMode == DisplayAsciiMode.ASCII_NO_ADDITIONAL_SPACES && Character.isWhitespace((char) value))
				{
					s = Character.toString((char) value);
				}
			}

			// if use is not looking for ASCII chars, or if this one is one that
			// is not printable, then convert it into numeric form
			if(s == null)
			{
				switch(base)
				{
					case DECIMAL:
						// convert signed to unsigned
						if(value < 0)
						{
							value = 256 + value;
						}
						s = Integer.toString(value);
						break;
					case OCTAL:
						s = Integer.toOctalString(value);
						break;
					case BINARY:
						s = Integer.toBinaryString(value);
						break;
					case HEX:   // fall through to default
					default:
						s = Integer.toHexString(value);
				}
				// some formats (e.g. hex & octal) extend a negative number to multiple places
				// (e.g. FC becomes FFFC), so chop off extra stuff in front
				if(s.length() > convConst.width)
				{
					s = s.substring(s.length() - convConst.width);
				}

				// front pad with zeros and add to output
				if(s.length() < convConst.width)
				{
					buf.append("00000000".substring(8 - (convConst.width - s.length())));
				}
			}
			buf.append(s);

			if(displayAsciiMode != DisplayAsciiMode.ASCII_NO_ADDITIONAL_SPACES)
         {
            buf.append("  ");   // always add spaces at end for consistancy
         }
      }
		return buf.toString();
	}
	
	/**
	 * Convert a string into Bytes.  The string is assumed to be in
	 * the form generated by the convertToString function, with each
	 * byte's data space separated from each other byte's.
	 */
	public static Byte[] convertToBytes(String data, int base, boolean showAscii)
		throws NumberFormatException {
		
		ConversionConstants convConst = getConstants(base);
		
		if (data == null)
			return null;
		
		if (data.length() == 0)
			return new Byte[0];
		
		if (data.equals(StringUtilities.NULL_AS_STRING))
			return null;
		
		int stringIndex = 0;
		int byteIndex = 0;
		Byte[] bytes = new Byte[(data.length()+2)/(convConst.width+2)];
		while (stringIndex < data.length()) {
			// get the text to be converted
			String s = data.substring(stringIndex, stringIndex+convConst.width);
			
			// handle ASCII chars
			// Irrespective of the radix, the second byte will always
			// be a space when the data is displayed as a single ASCII character.
			if (showAscii && s.charAt(1) == ' ') {
				// convert the char into its numeric value
				bytes[byteIndex++] = Byte.valueOf((byte)s.charAt(0));
			}
			else {

				// The following ugly conversion from text to Byte is necessary because
				// the Byte class is inconsistant.  When asked to output as Hex, it does
				// so as an UNSIGNED byte, but when asked to read back the same thing
				// using the Hex radix, it insists that the input must be SIGNED.
				// To get around this, we up-size the conversion to Integer, then 
				// truncate that to a byte, and finally convert the byte to a Byte.  Yech.
				bytes[byteIndex++] = Byte.valueOf(
					(byte)(Integer.valueOf(s, convConst.radix)).intValue());	
			}	

			stringIndex += convConst.width + 2;
		}
	
		return bytes;
	}

	/**
	 * Get the constants to use for the given base.
	 */
	private static ConversionConstants getConstants(int base) {
		if (base == HEX) return hex;
		if (base == DECIMAL) return decimal;
		if (base == OCTAL) return octal;
		if (base == BINARY) return binary;
		return hex;	// default to hex if unknown base passed in
	}
}
