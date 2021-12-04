package org.bimserver.ifc.step.deserializer;

import com.google.common.base.Charsets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;

public class StringDecoder {
    final static private Map<Character, String> alphabets = new HashMap<>();
    static {
        for(int i= 0; i<9; i++){
            alphabets.put((char) ('A'+i), "ISO-8859-"+ (char)('1'+ i));
        }
    }
    private String alphabet;
    private final StringBuffer decoded = new StringBuffer();
    private final StringCharacterIterator encoded;
    private final String encodedString;
    private final long lineNumber;

    public StringDecoder(String encoded, long lineNumber){
        alphabet = alphabets.get('A');
        this.encoded = new StringCharacterIterator(encoded);
        this.encodedString = encoded;
        this.lineNumber = lineNumber;
    }

    public String decode() throws DeserializeException {
        while(encoded.current() != CharacterIterator.DONE){
            if (encoded.current() == '\\') {
                readDirective();
            } else {
                decoded.append(encoded.current());
            }
            encoded.next();
        }
        return decoded.toString();
    }

    private void readDirective() throws DeserializeException {
        switch (encoded.next()) {
            case '\\':
                decoded.append('\\'); break;
            case 'S':
                readPageDirective(); break;
            case 'P':
                readAlphabetDirective(); break;
            case 'X':
                readHexDirective(); break;
            default:
                throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "dangling \\, cannot decode directive or escaped backslash");
        }
    }

    private void readPageDirective() throws DeserializeException {
        if(encoded.next()!='\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\S directive not closed with \\");
        }
        ByteBuffer b = ByteBuffer.wrap(new byte[] { (byte) (encoded.next() + 128) });
        decoded.append(Charset.forName(alphabet).decode(b));
    }

    private void readAlphabetDirective() throws DeserializeException {
        alphabet = alphabets.get(encoded.next());
        if(alphabet == null){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\P invalid identifier in alphabet directive");
        }
        if(encoded.next()!='\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\P alphabet directive not closed with \\");
        }
    }

    private void readHexDirective() throws DeserializeException {
        switch(encoded.next()){
            case '2':
                readHex2Directive();
                break;
            case '4':
                readHex4Directive();
                break;
            case '\\':
                readHexArbitrary();
                break;
            default:
                throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "dangling \\X, cannot decode hex directive ");
        }
    }

    private void readHexArbitrary() {
        int code = Integer.parseInt(new String(new char[]{ encoded.next(), encoded.next() }), 16);
        ByteBuffer b = ByteBuffer.wrap(new byte[] { (byte) (code) });
        decoded.append(Charsets.ISO_8859_1.decode(b));
    }

    private void readHex2Directive() throws DeserializeException {
        if ( encoded.next() != '\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\X2 directive not closed with \\");
        }
        int index = encoded.getIndex()+1;
        int indexOfEnd = encodedString.indexOf("\\X0\\", index);
        if (indexOfEnd == -1) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_X4_NOT_CLOSED_WITH_X0, lineNumber, "\\X4\\ not closed with \\X0\\");
        }
        if ((indexOfEnd - index) % 4 != 0) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_NUMBER_OF_HEX_CHARS_IN_X2_NOT_DIVISIBLE_BY_4, lineNumber, "Number of hex chars in \\X4\\ definition not divisible by 8");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(encodedString.substring(index, indexOfEnd).toCharArray()));
            decoded.append(Charsets.UTF_16BE.decode(buffer));
        } catch (DecoderException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_CHARACTER_DECODING_EXCEPTION, lineNumber, e);
        }
        encoded.setIndex(indexOfEnd+3);
    }

    private void readHex4Directive() throws DeserializeException {
        if ( encoded.next() != '\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\X4 directive not closed with \\");
        }
        int index = encoded.getIndex()+1;
        int indexOfEnd = encodedString.indexOf("\\X0\\", index);
        if (indexOfEnd == -1) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_X4_NOT_CLOSED_WITH_X0, lineNumber, "\\X4\\ not closed with \\X0\\");
        }
        if ((indexOfEnd - index) % 8 != 0) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_NUMBER_OF_HEX_CHARS_IN_X4_NOT_DIVISIBLE_BY_8, lineNumber, "Number of hex chars in \\X4\\ definition not divisible by 8");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(encodedString.substring(index, indexOfEnd).toCharArray()));
            decoded.append(Charset.forName("UTF-32").decode(buffer));
        } catch (DecoderException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_CHARACTER_DECODING_EXCEPTION, lineNumber, e);
        } catch (UnsupportedCharsetException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_UTF32_NOT_SUPPORTED_ON_SYSTEM, lineNumber, "UTF-32 is not supported on your system", e);
        }
        encoded.setIndex(indexOfEnd+3);
    }

}
/*
Refactored grammar to remove first-first conflicts

BS = REVERSE_SOLIDUS {PAGE0 | ALPHABET0 | EXTENDED2X | EXTENDED4X | ARBITRYX }.
BSX = "X" { EXTENDED2X0 | EXTENDED4X0 | ARBITRARYX0 }.
EXTENDED2X0 = "2" REVERSE_SOLIDUS HEX_TWO { HEX_TWO } END_EXTENDED .
EXTENDED4X0 = "4" REVERSE_SOLIDUS HEX_FOUR { HEX_FOUR } END_EXTENDED .
ARBITRARYX0 = REVERSE_SOLIDUS HEX_ONE .
PAGE0 = "S" REVERSE_SOLIDUS LATIN_CODEPOINT .
ALPHABET0 = "P" UPPER REVERSE_SOLIDUS .
 */

