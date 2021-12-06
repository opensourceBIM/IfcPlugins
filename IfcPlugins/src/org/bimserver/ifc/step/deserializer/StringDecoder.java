package org.bimserver.ifc.step.deserializer;

import com.google.common.base.Charsets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.DeserializerErrorCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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
    private final StringBuilder decoded = new StringBuilder();
    private final String encoded;
    private int pos = 0;
    private final int end;
    private final long lineNumber;

    public StringDecoder(String encoded, long lineNumber){
        alphabet = alphabets.get('A');
        this.encoded = encoded;
        this.end = encoded.length();
        this.lineNumber = lineNumber;
    }

    public String decode() throws DeserializeException{
        int nextBackslash;
        while( (nextBackslash  = nextPos('\\')) > -1 ){
            decoded.append(encoded, pos, nextBackslash);
            setPos(nextBackslash);
            readDirective();
            next();
        }
        decoded.append(encoded, pos, end);
        return decoded.toString();
    }

    private void readDirective() throws DeserializeException {
        switch (next()) {
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
        if(next()!='\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\S directive not closed with \\");
        }
        ByteBuffer b = ByteBuffer.wrap(new byte[] { (byte) (next() + 128) });
        decoded.append(Charset.forName(alphabet).decode(b));
    }

    private void readAlphabetDirective() throws DeserializeException {
        alphabet = alphabets.get(next());
        if(alphabet == null){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\P invalid identifier in alphabet directive");
        }
        if(next()!='\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\P alphabet directive not closed with \\");
        }
    }

    private void readHexDirective() throws DeserializeException {
        switch(next()){
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
        int code = Integer.parseInt(new String(new char[]{ next(), next() }), 16);
        ByteBuffer b = ByteBuffer.wrap(new byte[] { (byte) (code) });
        decoded.append(Charsets.ISO_8859_1.decode(b));
    }

    private void readHex2Directive() throws DeserializeException {
        if ( next() != '\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\X2 directive not closed with \\");
        }
        pos ++;
        int indexOfEnd = nextPos("\\X0\\");
        if (indexOfEnd == -1) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_X4_NOT_CLOSED_WITH_X0, lineNumber, "\\X4\\ not closed with \\X0\\");
        }
        if ((indexOfEnd - pos) % 4 != 0) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_NUMBER_OF_HEX_CHARS_IN_X2_NOT_DIVISIBLE_BY_4, lineNumber, "Number of hex chars in \\X4\\ definition not divisible by 8");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(encoded.substring(pos, indexOfEnd).toCharArray()));
            decoded.append(Charsets.UTF_16BE.decode(buffer));
        } catch (DecoderException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_CHARACTER_DECODING_EXCEPTION, lineNumber, e);
        }
        setPos(indexOfEnd+3);
    }

    private void readHex4Directive() throws DeserializeException {
        if ( next() != '\\'){
            throw new DeserializeException(DeserializerErrorCode.UNKNOWN_DESERIALIZER_ERROR, lineNumber, "\\X4 directive not closed with \\");
        }
        pos++;
        int indexOfEnd = nextPos("\\X0\\" );
        if (indexOfEnd == -1) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_X4_NOT_CLOSED_WITH_X0, lineNumber, "\\X4\\ not closed with \\X0\\");
        }
        if ((indexOfEnd - pos) % 8 != 0) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_NUMBER_OF_HEX_CHARS_IN_X4_NOT_DIVISIBLE_BY_8, lineNumber, "Number of hex chars in \\X4\\ definition not divisible by 8");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Hex.decodeHex(encoded.substring(pos, indexOfEnd).toCharArray()));
            decoded.append(Charset.forName("UTF-32").decode(buffer));
        } catch (DecoderException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_CHARACTER_DECODING_EXCEPTION, lineNumber, e);
        } catch (UnsupportedCharsetException e) {
            throw new DeserializeException(DeserializerErrorCode.STRING_ENCODING_UTF32_NOT_SUPPORTED_ON_SYSTEM, lineNumber, "UTF-32 is not supported on your system", e);
        }
        setPos(indexOfEnd+3);
    }

    private void setPos(int i){
        if(i>=0 && i<=end){
            pos = i;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private char next(){
        if (pos < end - 1) {
            pos++;
            return encoded.charAt(pos);
        } else {
            pos = end;
            return '\uffff'; // Stolen from StringCharacterIterator
        }
    }

    private int nextPos(String c){
        return encoded.indexOf(c, pos);
    }

    private int nextPos(char c){
        return encoded.indexOf(c, pos);
    }


}
/*
String grammar from ISO 10303-21

LATIN_CODEPOINT   = SPACE | DIGIT | LOWER | UPPER | SPECIAL | REVERSE_SOLIDUS | APOSTROPHE.
STRING            = "'" { SPECIAL | DIGIT | SPACE | LOWER | UPPER | HIGH_CODEPOINT | APOSTROPHE APOSTROPHE | REVERSE_SOLIDUS REVERSE_SOLIDUS | CONTROL_DIRECTIVE } "'" .
CONTROL_DIRECTIVE = PAGE | ALPHABET | EXTENDED2 | EXTENDED4 | ARBITRARY .
PAGE              = REVERSE_SOLIDUS "S" REVERSE_SOLIDUS LATIN_CODEPOINT .
ALPHABET          = REVERSE_SOLIDUS "P" UPPER REVERSE_SOLIDUS .
EXTENDED2         = REVERSE_SOLIDUS "X2" REVERSE_SOLIDUS HEX_TWO { HEX_TWO } END_EXTENDED .
EXTENDED4         = REVERSE_SOLIDUS "X4" REVERSE_SOLIDUS HEX_FOUR { HEX_FOUR } END_EXTENDED .
END_EXTENDED      = REVERSE_SOLIDUS "X0" REVERSE_SOLIDUS .
ARBITRARY         = REVERSE_SOLIDUS "X" REVERSE_SOLIDUS HEX_ONE .

Refactored grammar to remove first-first conflicts

STRING        =  "'" { NON_RS | RS } "'" .
NON_RS        = SPECIAL | DIGIT | SPACE | LOWER | UPPER | HIGH_CODEPOINT | APOSTROPHE APOSTROPHE .
RS            = REVERSE_SOLIDUS RS_OR_CONTROL .
RS_OR_CONTROL = REVERSE_SOLIDUS | PAGE0 | ALPHABET0 | UNICODE0 .
PAGE0         = "S" REVERSE_SOLIDUS LATIN_CODEPOINT .
ALPHABET0     = "P" UPPER REVERSE_SOLIDUS .
UNICODE0      = "X" UNICODE1.
UNICODE1      = ARBITRARY0 | EXTENDED20 | EXTENDED40 .
ARBITRARY0    = REVERSE_SOLIDUS HEX_ONE .
EXTENDED20    = "2" REVERSE_SOLIDUS HEX_TWO { HEX_TWO } END_EXTENDED .
EXTENDED40    = "4" REVERSE_SOLIDUS HEX_FOUR { HEX_FOUR } END_EXTENDED .
 */

